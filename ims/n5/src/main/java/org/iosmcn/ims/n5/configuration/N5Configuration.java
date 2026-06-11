package org.iosmcn.ims.n5.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration class for N5 interface.
 * Loads and manages configuration parameters from an INI file.
 */
@Configuration
public class N5Configuration {

    // Logger for recording configuration loading status and errors.
    private final static Logger logger = LogManager.getLogger(N5Configuration.class);

    // Configuration parameters for service bindings and mappings.
    private String afBindIp;
    private int afBindPort;
    private String scpIp;
    private int scpPort;
    private String pcfIp;
    private int pcfPort;
    private String afBindSbiIp;
    private int afBindSbiPort;
    private String instanceId;
    private boolean isiosmcncore = false;

    // Path to the configuration file, with a default fallback.
    @Value("${app.config}")
    private String configFilePath;

    /**
     * Loads configuration settings from the specified INI file.
     * This method is executed after the Spring context is initialized.
     */@PostConstruct
    public void loadConfiguration() {
        try {
            // Read the INI configuration file.
            Ini ini = new Ini(new File(configFilePath));
            java.util.prefs.Preferences configuration = new IniPreferences(ini);

            // Retrieve N5 interface settings from the "N5-MAPPING" section.
            this.scpIp = configuration.node("N5-MAPPING").get("SCP-IP", "127.0.0.200");
            this.scpPort = configuration.node("N5-MAPPING").getInt("SCP-PORT", 7777);
            this.pcfIp = configuration.node("N5-MAPPING").get("PCF-IP", "127.0.0.13");
            this.pcfPort = configuration.node("N5-MAPPING").getInt("PCF-PORT", 7777);
            this.afBindIp = configuration.node("N5-MAPPING").get("AF-BIND-IP", "127.0.0.201");
            this.afBindPort = configuration.node("N5-MAPPING").getInt("AF-BIND-PORT", 7777);
            this.afBindSbiIp = configuration.node("N5-MAPPING").get("AF-BIND-SBI-IP", "127.0.0.201");
            this.afBindSbiPort = configuration.node("N5-MAPPING").getInt("AF-BIND-SBI-PORT", 7782);

            // Generate a unique instance identifier for this configuration.
            this.instanceId = java.util.UUID.randomUUID().toString();

            boolean isIosMcnCoreEnabled = configuration.node("N5-MAPPING").getBoolean("IS-IOS-MCN-CORE", false);

            if (isIosMcnCoreEnabled) {
                this.isiosmcncore = true;
            } else {
                this.isiosmcncore = false;
            }
            // Log the loaded configuration for verification.
            logger.info("Configuration Loaded: SCP IP={}, SCP PORT={}, PCF IP={}, PCF PORT={}, AF BIND IP={}, AF BIND PORT={}, AF BIND SBI IP={}, AF BIND SBI PORT={}, Instance ID={}, IOS MCN CORE ENABLED={}",
                    scpIp, scpPort, pcfIp, pcfPort, afBindIp, afBindPort, afBindSbiIp, afBindSbiPort, instanceId, isiosmcncore);
        } catch (IOException e) {
            // Log an error if the configuration file cannot be read.
            logger.error("Error reading configuration file: {}", e.getMessage());
        }
    }

    // Getter methods for retrieving configuration values.
    public String getScpIp() { return scpIp; }
    public int getScpPort() { return scpPort; }
    public String getPcfIp() { return pcfIp; }
    public int getPcfPort() { return pcfPort; }
    public String getAfBindIp() { return afBindIp; }
    public int getAfBindPort() { return afBindPort; }
    public String getAfBindSbiIp() { return afBindSbiIp; }
    public int getAfBindSbiPort() { return afBindSbiPort; }
    public String getInstanceId() { return instanceId; }
    public boolean isIosMcnCore() { return isiosmcncore; }
}
