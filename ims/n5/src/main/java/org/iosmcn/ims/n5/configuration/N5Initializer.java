package org.iosmcn.ims.n5.configuration;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.File;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;

public class N5Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    /**
     * Logger for the N5Initializer class.
     */

    private static final Logger logger = LogManager.getLogger(N5Initializer.class);
    /**
     * Initializes the application context with configuration settings.
     * Reads the INI file specified by the 'app.config' property and sets system properties.
     *
     * @param context The application context to be initialized.
     */

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        // Retrieve the environment to access properties.
        ConfigurableEnvironment environment = context.getEnvironment();
        // Get the configuration file path from the environment properties.
        String configFilePath = environment.getProperty("app.config");

        // Check if the configuration file path is provided.
        if (configFilePath == null) {
            logger.error("Missing 'app.config' property for N5 Interface Application.");
            return;
        }

        // Attempt to read the INI configuration file.
        try {
            // Log the initialization process.
            logger.info("Initializing N5 Interface Application with config file: {}", configFilePath);
            // Create an Ini object to read the configuration file.
            Ini ini = new Ini(new File(configFilePath));
            // Create a Preferences object to access the INI sections.
            Preferences prefs = new IniPreferences(ini);

            // Set system properties based on the INI configuration.
            String ip = prefs.node("N5-MAPPING").get("AF-BIND-SBI-IP", "127.0.0.1");
            int port = prefs.node("N5-MAPPING").getInt("AF-BIND-SBI-PORT", 7782);

            // Set the server address and port as system properties.
            System.setProperty("server.address", ip);
            System.setProperty("server.port", String.valueOf(port));

            logger.info("N5 Interface Application initialized with IP: {} and Port: {}", ip, port);

        } catch (Exception e) {
            // Log an error if the configuration file cannot be read.
            logger.error("Failed to read N5 Interface Application config: {}", e.getMessage());
        }
    }
}
