package org.iosmcn.ims.n5.service;

import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iosmcn.ims.n5.configuration.N5Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service class for managing Network Function (NF) registration, heartbeat
 * monitoring,
 * creating audio and video PDU, and deletion of PDUs.
 * Handles communication with the SCP (Service Communication Proxy) for
 * registration and monitoring.
 */
@Service
public class NFService {

    // Logger instance for recording events and errors.
    private final static Logger logger = LogManager.getLogger(NFService.class);

    // Configuration instance containing NF-related parameters.
    private final N5Configuration config;

    private final OkHttpClient okHttpClient;
    private final OkHttpClient OkHttpPriorKnowledgeClient;

    // Map to track UE (User Equipment) sessions.
    private final ConcurrentHashMap<String, List<String>> ueSessionMap = new ConcurrentHashMap<>();

    // Scheduled executor for handling periodic heartbeat tasks.
    private ScheduledExecutorService scheduler;

    // Counter to track consecutive heartbeat failures.
    private int failedHeartbeatCount = 0; // Track consecutive failures

    // Maximum allowed failed heartbeat attempts before stopping.
    private final int MAX_FAILED_ATTEMPTS = 3; // Threshold before stopping

    // Cached JSON content
    private String nfRegisterJsonContent;
    private String heartbeatJsonContent;
    private String audioQosJsonContent;
    private String videoQosJsonContent;

    /**
     * Constructor for NFService.
     * Initializes the HTTP client and assigns configuration.
     *
     * @param client HTTP client used for communication.
     * @param config Configuration containing network parameters.
     */
    public NFService(OkHttpClient okHttpClient, OkHttpClient OkHttpPriorKnowledgeClient, N5Configuration config) {
        this.okHttpClient = okHttpClient;
        this.OkHttpPriorKnowledgeClient = OkHttpPriorKnowledgeClient;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        loadJsonTemplates();
        logger.info("Starting NF registration...");
        retryRegistration(); // Keeps retrying registration every 10s if it fails
        startN5SocketServer();
    }

    private void loadJsonTemplates() {
        try {
            nfRegisterJsonContent = loadJsonContent("dynamic/nfRegister.json");
            heartbeatJsonContent = loadJsonContent("static/heartbeat.json");
            audioQosJsonContent = loadJsonContent("dynamic/audioqos.json");
            videoQosJsonContent = loadJsonContent("dynamic/videoqos.json");

            logger.info("All JSON templates loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load JSON templates: {}", e.getMessage(), e);
        }
    }

    private String loadJsonContent(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        if (!resource.exists()) {
            throw new IOException("JSON file not found: " + filePath);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes());
        }
    }

    private void startN5SocketServer() {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                InetAddress bindAddress = InetAddress.getByName(config.getAfBindIp());
                serverSocket = new ServerSocket(config.getAfBindPort(), 50, bindAddress);

                logger.info("N5 Module listening on {}:{}", config.getAfBindIp(), config.getAfBindPort());

                while (!serverSocket.isClosed()) { // Ensure the server runs unless stopped
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.info("Connection received from {}", clientSocket.getInetAddress());

                        // Handle each client connection in a separate thread
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (serverSocket.isClosed()) {
                            logger.info("N5 Socket Server shutting down...");
                            break; // Exit the loop if the server is closed
                        }
                        logger.error("Error handling client connection: {}", e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                logger.error("Error in N5 Socket Server: {}", e.getMessage(), e);
            } finally {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                        logger.info("N5 Socket Server closed successfully.");
                    } catch (IOException e) {
                        logger.error("Error closing ServerSocket: {}", e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try {
                logger.info("Processing connection from {}", clientSocket.getInetAddress());
                // TODO: Read & process client input here
                clientSocket.close(); // Close the client socket after handling
            } catch (IOException e) {
                logger.error("Error in client communication: {}", e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Registers the Network Function (NF) with the SCP.
     * Loads the NF registration JSON template, fills in dynamic values,
     * and sends a PUT request to the SCP.
     *
     * @return A message indicating success or failure of the registration.
     */
    public String registerNF() {
        logger.info("Registering NF with SCP IP: {} and SCP PORT: {}", config.getScpIp(), config.getScpPort());

        if (nfRegisterJsonContent == null) {
            logger.error("Failed to load nfRegister.json. NF registration aborted.");
            return "Failed to load nfRegister.json. NF registration aborted.";
        }

        // Construct the NF registration URL.
        String url = "http://" + config.getScpIp() + ":" + config.getScpPort()
                + "/nnrf-nfm/v1/nf-instances/" + config.getInstanceId();

        String jsonPayload = nfRegisterJsonContent
                .replace("##{UUID}", config.getInstanceId())
                .replace("##{AFIP}", config.getAfBindIp())
                .replace("##{AFPORT}", String.valueOf(config.getAfBindPort()));

        // Create the request body with JSON content.
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));

        // Construct the HTTP request with required headers.
        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .addHeader("user-agent", "AF")
                .addHeader("accept", "application/json,application/problem+json")
                .addHeader("content-type", "application/json")
                .addHeader("3gpp-sbi-discovery-target-nf-type", "NRF")
                .addHeader("3gpp-sbi-max-rsp-time", "10000")
                .addHeader("3gpp-sbi-discovery-service-names", "nnrf-nfm")
                .addHeader("3gpp-sbi-sender-timestamp",
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now()))
                .build();

        OkHttpClient client = getSbiClient();

        // Execute the HTTP request and handle the response.
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("NF Registration failed with response code: {}", response.code());
                return "NF Registration failed with response code: " + response.code();
            } else {
                String responseBody;
                try (ResponseBody responsebody = response.body()) {
                    responseBody = responsebody != null ? responsebody.string() : null;
                }
                logger.info("NF Registered Successfully! Response: {}", responseBody);

                // Extract the heartbeat time from the response.
                int heartbeatTime = extractHeartbeatTime(responseBody);
                if (heartbeatTime > 0) {
                    logger.info("Heartbeat time received: {} seconds", heartbeatTime);
                    scheduleHeartbeat(heartbeatTime);
                    return "NF Registration and Heartbeat scheduling started successfully!";
                } else {
                    logger.warn("Invalid heartbeat time received, defaulting to 10 seconds.");
                    scheduleHeartbeat(10);
                    return "NF Registration successful, but invalid heartbeat time received. Defaulting to 10 seconds.";
                }
            }
        } catch (IOException e) {
            logger.error("Error while registering NF: {}", e.getMessage());
            return "Error occurred during NF Registration: " + e.getMessage();
        }
    }

    /**
     * Extracts the heartbeat timer value from the JSON response.
     * If the "heartBeatTimer" field is found, its value is returned.
     * If an error occurs or the field is missing, -1 is returned.
     *
     * @param responseBody The JSON response containing the heartbeat timer.
     * @return The extracted heartbeat time in seconds, or -1 if extraction fails.
     */
    private int extractHeartbeatTime(String responseBody) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);

            if (jsonNode.has("HeartBeatTimer")) {
                return jsonNode.get("HeartBeatTimer").asInt();
            } else if (jsonNode.has("heartBeatTimer")) {
		return jsonNode.get("heartBeatTimer").asInt();
	    }
        } catch (Exception e) {
            logger.error("Error parsing heartbeat timer from response: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Schedules a periodic heartbeat request to the SCP to maintain NF
     * registration.
     * Ensures only one scheduler is active at a time and handles failures
     * accordingly.
     *
     * @param heartbeatTime The interval (in seconds) at which the heartbeat is
     *                      sent.
     */
    private void scheduleHeartbeat(int heartbeatTime) {
        // Validate heartbeat time, default to 10 seconds if invalid.
        if (heartbeatTime <= 0) {
            logger.warn("Invalid heartbeat time received: {}. Defaulting to 10 seconds.", heartbeatTime);
            heartbeatTime = 10;
        }

        if (heartbeatJsonContent == null) {
            logger.error("Heartbeat JSON content not loaded. Cannot schedule heartbeat.");
            return;
        }
        // Construct the heartbeat request URL.
        String url = "http://" + config.getScpIp() + ":" + config.getScpPort()
                + "/nnrf-nfm/v1/nf-instances/" + config.getInstanceId();
        // If an existing scheduler is running, shut it down before starting a new one.
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // Stop any existing scheduler before starting a new one
        }

        // Initialize a new scheduled executor service with a single-threaded pool.
        scheduler = Executors.newScheduledThreadPool(1);

        // Define the heartbeat task to be executed periodically.
        Runnable task = () -> {
            try {
                String jsonPayload = heartbeatJsonContent;
                logger.debug("Sending heartbeat JSON: {}", jsonPayload);

                RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json-patch+json"));

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("accept", "application/json,application/problem+json")
                        .addHeader("user-agent", "AF")
                        .addHeader("content-type", "application/json-patch+json")
                        .addHeader("3gpp-sbi-discovery-target-nf-type", "NRF")
                        .addHeader("3gpp-sbi-max-rsp-time", "10000")
                        .addHeader("3gpp-sbi-discovery-service-names", "nnrf-nfm")
                        .addHeader("3gpp-sbi-sender-timestamp", java.time.ZonedDateTime.now().toString())
                        .patch(body)
                        .build();

                OkHttpClient client = getSbiClient();

                // Execute the heartbeat request and handle the response.
                try (Response response = client.newCall(request).execute()) {
                    if (response.code() == 204 || response.code() == 200) {
                        logger.info("Heartbeat sent successfully.");
                        // Reset failure count on success
                        failedHeartbeatCount = 0;
                    } else {
                        logger.error("Heartbeat failed. Response Code: {}", response.code());
                        handleHeartbeatFailure();
                    }
                }
            } catch (Exception e) {
                logger.error("Heartbeat connection lost: {}", e.getMessage());
                handleHeartbeatFailure();
            }
        };

        // Schedule the heartbeat task at a fixed rate.
        scheduler.scheduleAtFixedRate(task, heartbeatTime, heartbeatTime, TimeUnit.SECONDS);
    }

    /**
     * Creates an Audio PDU session by sending a request to the PCF.
     * This method constructs the request payload dynamically using entity
     * parameters.
     *
     * @param entity A map containing UE (User Equipment) details such as IP, ports,
     *               and MSISDN.
     * @return A string message indicating success or failure of PDU Audio creation.
     */
    public String createPDUAudio(Map<String, String> entity) {
        if (audioQosJsonContent == null) {
            logger.error("Audio QoS JSON content not loaded");
            return "Audio QoS JSON content not loaded";
        }
        // Extract UE RTCP Port; fallback to UE Audio Port +1 if missing.
        int ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUERTCPPORT", "0"));
        if (ueRtcpPort == 0) {
            ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEAUDIOPORT", "0")) + 1;
        }

        // Log PCF connection details.
        logger.info("Triggering AUDIO PDU with PCF IP: {} and PCF PORT: {}", config.getPcfIp(), config.getPcfPort());

        // Construct the PCF API endpoint for session creation.
        String url = "http://" + config.getPcfIp() + ":" + config.getPcfPort()
                + "/npcf-policyauthorization/v1/app-sessions";
        logger.info("Final URL Is : {}", url);

        // Extract and set placeholders in the JSON template with actual values.
        String ueIp = entity.getOrDefault("fromUEIP", "0.0.0.0");
        String ueAudioPort = entity.getOrDefault("fromUEAUDIOPORT", "0");
        String afIp = entity.getOrDefault("AFIP", config.getAfBindIp());
        String afPort = entity.getOrDefault("AFPORT", String.valueOf(config.getAfBindPort()));
        String ueMsisdn = entity.getOrDefault("fromUEMSISDN", "unknown");

        String jsonPayload = audioQosJsonContent
                .replace("##{UEIP}", ueIp)
                .replace("##{UEAUDIOPORT}", ueAudioPort)
                .replace("##{UERTCPPORT}", String.valueOf(ueRtcpPort))
                .replace("##{AFIP}", afIp)
                .replace("##{AFPORT}", afPort)
                .replace("##{UEMSISDN}", ueMsisdn);

        logger.debug("Audio PDU JSON Payload: {}", jsonPayload);

        // Create the request body with the prepared JSON payload.
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));

        // Construct the HTTP POST request with required headers.
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("user-agent", "AF")
                .addHeader("accept", "application/json")
                .addHeader("accept", "application/problem+json")
                .addHeader("content-type", "application/json")
                .addHeader("3gpp-sbi-discovery-target-nf-type", "NRF")
                .addHeader("3gpp-sbi-max-rsp-time", "10000")
                .addHeader("3gpp-sbi-discovery-service-names", "npcf-policyauthorization")
                .addHeader("3gpp-sbi-sender-timestamp",
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now()))
                .build();

        OkHttpClient client = getSbiClient();

        // Execute the HTTP request and handle response.
        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String locationHeader = response.header("location"); // Extract Location header
            logger.info("Audio PDU Creation Response Code: {} and Location header is : {}", statusCode, locationHeader);

            // Check if request was unsuccessful.
            if (!response.isSuccessful()) {
                logger.error("Audio PDU Creation failed with response code: {}", response.code());
                return "Audio PDU Creation with response code: " + response.code();
            } else {
                String responseBody;
                try (ResponseBody responsebody = response.body()) {
                    responseBody = responsebody != null ? responsebody.string() : null;
                    logger.info("Audio PDU Response: {}", responseBody);
                    // Store session location for the UE MSISDN.
                    ueSessionMap.computeIfAbsent(ueMsisdn, k -> new ArrayList<>()).add(locationHeader);
                }
                logger.info("Audio PDU Created Successfully! Response: {}", responseBody);
                return "Audio PDU Created Successfully!";
            }
        } catch (IOException e) {
            // Log and return error in case of failure.
            logger.error("Error while creating Audio PDU: {}", e.getMessage(), e);
            return "Error occurred during Audio PDU Creation: " + e.getMessage();
        }
    }

    /**
     * Creates a Video PDU session by sending a request to the PCF.
     * This method constructs the request payload dynamically using entity
     * parameters.
     *
     * @param entity A map containing UE (User Equipment) details such as IP, ports,
     *               and MSISDN.
     * @return A string message indicating success or failure of PDU Video creation.
     */
    public String createPDUVideo(Map<String, String> entity) {
        if (videoQosJsonContent == null) {
            logger.error("Video QoS JSON content not loaded");
            return "Video QoS JSON content not loaded";
        }

        // Extract UE & VIDEO RTCP Port; fallback to UE Audio Port +1 & UE Video Port +1
        // if missing.
        int ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUERTCPPORT", "0"));
        int ueVideoRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEVIDEORTCPPORT", "0"));
        if (ueRtcpPort == 0) {
            ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEAUDIOPORT", "0")) + 1;
        }
        if (ueVideoRtcpPort == 0) {
            ueVideoRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEVIDEOPORT", "0")) + 1;
        }

        // Log PCF connection details.
        logger.info("Triggering VIDEO PDU with PCF IP: {} and PCF PORT: {}", config.getPcfIp(), config.getPcfPort());

        // Construct the PCF API endpoint for session creation.
        String url = "http://" + config.getPcfIp() + ":" + config.getPcfPort()
                + "/npcf-policyauthorization/v1/app-sessions";
        logger.info("Final URL Is : {}", url);

        // Extract and set placeholders in the JSON template with actual values.
        String ueIp = entity.getOrDefault("fromUEIP", "0.0.0.0");
        String ueAudioPort = entity.getOrDefault("fromUEAUDIOPORT", "0");
        String ueVideoPort = entity.getOrDefault("fromUEVIDEOPORT", "0");
        String afIp = entity.getOrDefault("AFIP", config.getAfBindIp());
        String afPort = entity.getOrDefault("AFPORT", String.valueOf(config.getAfBindPort()));
        String ueMsisdn = entity.getOrDefault("fromUEMSISDN", "unknown");

        String jsonPayload = videoQosJsonContent
                .replace("##{UEIP}", ueIp)
                .replace("##{UEAUDIOPORT}", ueAudioPort)
                .replace("##{UEVIDEOPORT}", ueVideoPort)
                .replace("##{UERTCPPORT}", String.valueOf(ueRtcpPort))
                .replace("##{UEVIDEORTCPPORT}", String.valueOf(ueVideoRtcpPort))
                .replace("##{AFIP}", afIp)
                .replace("##{AFPORT}", afPort)
                .replace("##{UEMSISDN}", ueMsisdn);

        logger.debug("Video PDU JSON Payload: {}", jsonPayload);

        // Create the request body with the prepared JSON payload.
        RequestBody body = RequestBody.create(jsonPayload, MediaType.get("application/json"));

        // Construct the HTTP POST request with required headers.
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("user-agent", "AF")
                .addHeader("accept", "application/json")
                .addHeader("accept", "application/problem+json")
                .addHeader("content-type", "application/json")
                .addHeader("3gpp-sbi-discovery-target-nf-type", "NRF")
                .addHeader("3gpp-sbi-max-rsp-time", "10000")
                .addHeader("3gpp-sbi-discovery-service-names", "npcf-policyauthorization")
                .addHeader("3gpp-sbi-sender-timestamp",
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(java.time.ZonedDateTime.now()))
                .build();

        OkHttpClient client = getSbiClient();

        // Execute the HTTP request and handle response.
        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String locationHeader = response.header("location"); // Extract Location header
            logger.info("Video PDU Creation Response Code: {} and Location header is : {}", statusCode, locationHeader);

            // Check if request was unsuccessful.
            if (!response.isSuccessful()) {
                logger.error("Video PDU Creation failed with response code: {}", response.code());
                return "Video PDU Creation with response code: " + response.code();
            } else {
                String responseBody;
                try (ResponseBody responsebody = response.body()) {
                    responseBody = responsebody != null ? responsebody.string() : null;
                    logger.info("Video PDU Response: {}", responseBody);
                    ueSessionMap.computeIfAbsent(ueMsisdn, k -> new ArrayList<>()).add(locationHeader);
                }
                logger.info("Video PDU Created Successfully!");
                return "Video PDU Created Successfully!";
            }
        } catch (IOException e) {
            // Log and return error in case of failure.
            logger.error("Error while creating Video PDU: {}", e.getMessage(), e);
            return "Error occurred during Video PDU Creation: " + e.getMessage();
        }
    }

    /**
     * Deletes all active PDU sessions for a given UE (User Equipment) based on its
     * MSISDN.
     * This method retrieves session locations from the session map and attempts to
     * delete them.
     *
     * @param entity A map containing UE details, specifically the MSISDN used to
     *               identify active sessions.
     * @return A string message indicating the success or failure of session
     *         deletions.
     */
    public String deletePDU(Map<String, String> entity) {
        // Extract UE MSISDN from input entity, defaulting to "unknown" if missing.
        String ueMsisdn = entity.getOrDefault("fromUEMSISDN", "unknown");

        // List to track failed session deletions.
        List<String> failedDeletions = new ArrayList<>();

        // Retrieve session locations for the given UE MSISDN.
        List<String> locationHeaders = ueSessionMap.get(ueMsisdn);

        // If no active sessions are found, log and return an appropriate message.
        if (locationHeaders == null || locationHeaders.isEmpty()) {
            logger.error("No session found for UE MSISDN: {}", ueMsisdn);
            return "No session found for UE MSISDN: " + ueMsisdn;
        } else {

            // Log the number of active sessions found.
            logger.info("Found {} sessions for UE MSISDN: {}", locationHeaders.size(), ueMsisdn);

            // Iterate through each session and attempt to delete it.
            for (String sessionLocation : locationHeaders) {
                // Construct the deletion URL for the session.
                String deleteUrl = sessionLocation + "/delete";
                logger.info("Attempting to delete session: {}", sessionLocation);

                // Create an HTTP POST request to delete the session.
                Request request = new Request.Builder()
                        .url(deleteUrl)
                        .post(RequestBody.create(new byte[0], null)) // Empty body for POST
                        .addHeader(":method", "POST")
                        .addHeader(":scheme", "http")
                        .addHeader("Accept", "application/json")
                        .addHeader("accept", "application/problem+json")
                        .addHeader("content-type", "application/json")
                        .addHeader("user-agent", "AF")
                        .build();

                OkHttpClient client = getSbiClient();

                // Execute the request and handle response.
                try (Response response = client.newCall(request).execute()) {
                    // Log success or failure based on response code.
                    if (response.isSuccessful()) {
                        logger.info("Successfully deleted session: {} | Response Code: {}", sessionLocation,
                                response.code());
                    } else {
                        logger.error("Failed to delete session: {} | Response Code: {}", sessionLocation,
                                response.code());
                        failedDeletions.add(sessionLocation);
                    }
                } catch (IOException e) {
                    logger.error("Error while deleting session {}: {}", sessionLocation, e.getMessage(), e);
                    failedDeletions.add(sessionLocation);
                }
            }

            // If all sessions were successfully deleted, remove the UE's entry from the
            // session map.
            if (failedDeletions.isEmpty()) {
                ueSessionMap.remove(ueMsisdn);
                logger.info("All sessions successfully deleted for UE MSISDN: {}", ueMsisdn);
                return "All sessions successfully deleted for UE MSISDN: " + ueMsisdn;
            } else {
                // If some deletions failed, log and return the remaining sessions.
                logger.error("Some sessions could not be deleted for UE MSISDN: {}. Remaining sessions: {}", ueMsisdn,
                        failedDeletions);
                return "Some sessions could not be deleted. Remaining sessions: " + failedDeletions;
            }
        }
    }

    /**
     * Handles heartbeat failures by tracking consecutive failures.
     * If the failure count exceeds the allowed threshold, the heartbeat scheduler
     * is stopped,
     * and NF (Network Function) re-registration is triggered.
     */
    private void handleHeartbeatFailure() {
        // Increment the failure count on each unsuccessful heartbeat attempt.
        failedHeartbeatCount++;

        // If the failure count exceeds the defined threshold, take corrective action.
        if (failedHeartbeatCount >= MAX_FAILED_ATTEMPTS) {
            logger.error("Heartbeat failed {} times. Stopping heartbeat and re-registering NF.", failedHeartbeatCount);

            // Stop the current heartbeat scheduler to prevent further failed attempts.
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown();
            }

            // Start re-registration attempts every 10 seconds
            retryRegistration();
        }
    }

    /**
     * Initiates a scheduled task that repeatedly attempts NF re-registration every
     * 10 seconds
     * until successful. Once re-registration is successful, the failure count
     * resets and
     * heartbeat monitoring resumes.
     */
    private void retryRegistration() {
        // Create a new scheduler for handling retry attempts.
        ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();

        // Define the re-registration task.
        Runnable task = () -> {
            logger.info("Attempting NF re-registration...");

            // Perform NF registration and check the response message.
            String registrationResult = registerNF();

            // If registration is successful, reset failure count and stop retry attempts.
            if (registrationResult.contains("NF Registration and Heartbeat scheduling started successfully!")) {
                // Reset failure count
                failedHeartbeatCount = 0;
                logger.info("Re-registration successful. Heartbeat restarted.");
                // Stop retrying after success
                retryScheduler.shutdown();
            } else {
                // If registration fails, log an error and continue retrying.
                logger.error("Re-registration failed. Will retry in 10 seconds.");
            }
        };

        // Schedule the task to run immediately and then every 10 seconds.
        retryScheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * PATCH PDU for re-INVITE (upgrade / modify video)
     * Uses SAME videoqos.json as initial POST
     */
    public String patchPDU(Map<String, String> entity) {

        if (videoQosJsonContent == null) {
            logger.error("Video QoS JSON content not loaded");
            return "Video QoS JSON content not loaded";
        }

        String ueMsisdn = entity.getOrDefault("fromUEMSISDN", "unknown");
        List<String> sessionLocations = ueSessionMap.get(ueMsisdn);

        if (sessionLocations == null || sessionLocations.isEmpty()) {
            logger.warn("No existing PCF session found for PATCH VIDEO UE={}", ueMsisdn);
            return "No existing PCF session for PATCH VIDEO";
        }

        // PCF session already exists → PATCH on same resource
        String patchUrl = sessionLocations.get(0); // same app-session

        int ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUERTCPPORT", "0"));
        int ueVideoRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEVIDEORTCPPORT", "0"));
        String ueIp = entity.getOrDefault("fromUEIP", "0.0.0.0");
        String ueAudioPort = entity.getOrDefault("fromUEAUDIOPORT", "0");
        String ueVideoPort = entity.getOrDefault("fromUEVIDEOPORT", "0");
        String afIp = entity.getOrDefault("AFIP", config.getAfBindIp());
        String afPort = entity.getOrDefault("AFPORT", String.valueOf(config.getAfBindPort()));

        if (ueRtcpPort == 0) {
            if (!ueAudioPort.equals("0")) {
                ueRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEAUDIOPORT", "0")) + 1;
            } else {
                ueRtcpPort = 0;
            }
        }

        if (ueVideoRtcpPort == 0) {
            if (!ueVideoPort.equals(0)) {
                ueVideoRtcpPort = Integer.parseInt(entity.getOrDefault("fromUEVIDEOPORT", "0")) + 1;
            } else {
                ueVideoRtcpPort = 0;
            }
        }

        String jsonPayload = videoQosJsonContent
                .replace("##{UEIP}", ueIp)
                .replace("##{UEAUDIOPORT}", ueAudioPort)
                .replace("##{UEVIDEOPORT}", ueVideoPort)
                .replace("##{UERTCPPORT}", String.valueOf(ueRtcpPort))
                .replace("##{UEVIDEORTCPPORT}", String.valueOf(ueVideoRtcpPort))
                .replace("##{AFIP}", afIp)
                .replace("##{AFPORT}", afPort)
                .replace("##{UEMSISDN}", ueMsisdn);

        logger.debug("VIDEO PATCH Payload: {}", jsonPayload);

        RequestBody body = RequestBody.create(
                jsonPayload,
                MediaType.get("application/merge-patch+json"));

        Request request = new Request.Builder()
                .url(patchUrl)
                .patch(body)
                .addHeader("user-agent", "AF")
                .addHeader("accept", "application/json,application/problem+json")
                .addHeader("content-type", "application/merge-patch+json")
                .addHeader("3gpp-sbi-discovery-target-nf-type", "PCF")
                .addHeader("3gpp-sbi-discovery-service-names", "npcf-policyauthorization")
                .addHeader("3gpp-sbi-max-rsp-time", "10000")
                .build();

        OkHttpClient client = getSbiClient();

        try (Response response = client.newCall(request).execute()) {
            logger.info("VIDEO PATCH responseCode={}", response.code());

            if (response.isSuccessful()) {
                return "VIDEO PDU PATCH successful for UE: " + ueMsisdn;
            } else {
                return "VIDEO PDU PATCH failed code=" + response.code();
            }
        } catch (Exception e) {
            logger.error("VIDEO PATCH failed UE={}", ueMsisdn, e);
            return "VIDEO PDU PATCH failed: " + e.getMessage();
        }
    }

    private OkHttpClient getSbiClient() {
        return config.isIosMcnCore() ? okHttpClient : OkHttpPriorKnowledgeClient;
    }
}
