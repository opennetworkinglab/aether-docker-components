package org.iosmcn.ims.n5.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iosmcn.ims.n5.service.NFService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for handling Network Function (NF) operations.
 * Provides endpoints for creating and deleting PDUs (Protocol Data Units)
 * for audio and video services.
 */
@RestController
@RequestMapping("/api/nf")
public class NFController {

    // Logger for recording request processing and errors.
    private final static Logger logger = LogManager.getLogger(NFController.class);
    // Service layer dependency for handling NF-related operations.
    private final NFService nfService;

    /**
     * Constructor for NFController.
     * Injects an instance of NFService for handling business logic.
     *
     * @param nfService Service responsible for NF operations.
     */
    public NFController(NFService nfService) {
        this.nfService = nfService;
    }

    /**
     * Creates a PDU session for audio services.
     *
     * @param entity Request body containing required parameters.
     * @return Success message or an error description.
     */
    @PostMapping("/createPDUAudio")
    public String createPDUAudio(@RequestBody Map<String, String> entity) {
        try {
            logger.info("Creating PDU Audio...");
            String result = nfService.createPDUAudio(entity);
            return result;
        } catch (Exception e) {
            logger.error("Error occurred during PDU Audio creation: {}", e.getMessage(), e);
            return "Error occurred during PDU Audio creation: " + e.getMessage();
        }
    }

    /**
     * Creates a PDU session for video services.
     *
     * @param entity Request body containing required parameters.
     * @return Success message or an error description.
     */
    @PostMapping("/createPDUVideo")
    public String createPDUVideo(@RequestBody Map<String, String> entity) {
        try {
            logger.info("Creating PDU Video...");
            String result = nfService.createPDUVideo(entity);
            return result;
        } catch (Exception e) {
            logger.error("Error occurred during PDU Video creation: {}", e.getMessage(), e);
            return "Error occurred during PDU Video creation: " + e.getMessage();
        }
    }

    /**
     * Deletes an existing PDU session.
     *
     * @param entity Request body containing necessary identifiers for deletion.
     * @return Success message or an error description.
     */
    @PostMapping("/deletePDU")
    public String deletePDU(@RequestBody Map<String, String> entity) {
        try {
            logger.info("Deleting PDU...");
            String result = nfService.deletePDU(entity);
            return result;
        } catch (Exception e) {
            logger.error("Error occurred during PDU deletion: {}", e.getMessage(), e);
            return "Error occurred during PDU deletion: " + e.getMessage();
        }
    }

    @PostMapping("/patchPDU")
    public String patchPDU(@RequestBody Map<String, String> entity) {
        try {
            logger.info("Patching PDU (re-INVITE)...");
            return nfService.patchPDU(entity);
        } catch (Exception e) {
            logger.error("Error during PDU PATCH", e);
            return "Error during PDU PATCH: " + e.getMessage();
        }
    }

}
