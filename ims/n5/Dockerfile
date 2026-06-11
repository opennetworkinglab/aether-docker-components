# Use Debian 12 as base image
FROM debian:12

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive

# Install necessary packages
RUN apt update && apt install -y \
    openjdk-17-jdk \
    vim \
    maven \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy the project files
COPY . .

RUN mkdir -p config

RUN echo "[N5-MAPPING] \n\
    SCP-IP=127.0.0.200\n\
    SCP-PORT=7777\n\
    PCF-IP=127.0.0.13\n\
    PCF-PORT=7777\n\
    AF-BIND-IP=127.0.0.201\n\
    AF-BIND-PORT=7777\n\
    AF-BIND-SBI-IP=127.0.0.201\n\
    AF-BIND-SBI-PORT=7782" > /app/config/aether-ims-n5


RUN echo "[N5-MAPPING] \n\
    SCP-IP=127.0.0.200\n\
    SCP-PORT=7777\n\
    PCF-IP=127.0.0.13\n\
    PCF-PORT=7777\n\
    AF-BIND-IP=127.0.0.201\n\
    AF-BIND-PORT=7777\n\
    AF-BIND-SBI-IP=127.0.0.201\n\
    AF-BIND-SBI-PORT=7782" > /app/config/aether-ims-n5.test

RUN mv /app/defaults/n5-logging.xml /app/config

# Testing
RUN mvn test

# Build the project
RUN mvn clean package

RUN ls -lah target/

# Expose the application port (if applicable)
EXPOSE 8080

# Define the entrypoint to run the JAR file
CMD ["java", "-jar", "target/n5-0.4.0.jar"]
