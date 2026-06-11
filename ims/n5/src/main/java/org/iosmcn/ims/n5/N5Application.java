package org.iosmcn.ims.n5;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iosmcn.ims.n5.configuration.N5Initializer;

/**
 * Entry point for the N5 application.
 * This class initializes and runs the Spring Boot application.
 */
@SpringBootApplication
public class N5Application {
    private static final Logger logger = LogManager.getLogger(N5Application.class);

    /**
     * Main method to launch the Spring Boot application.
     *
     * @param args Command-line arguments passed during application startup.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(N5Application.class);
        app.addInitializers(new N5Initializer());
        app.run(args);
        logger.info("N5Application started");
    }
}
