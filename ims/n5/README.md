# N5 Interface for Policy Authorization

## Overview

**This project is an implementation of N5 Interface in a 5G Core Network using Spring Boot and OkHttp for HTTP/2 interactions. The service interacts with PCF (Policy Control Function) and NRF (Network Repository Function) using 3GPP-defined RESTful HTTP/2 APIs.**

### It provides functionalities for:
- NF Registration & Heartbeat Management
- PDU Session Creation for Audio & Video
- PDU Session Deletion

## Features

### NF Registration & Heartbeat:
- Registers the NF instance with the NRF (Network Repository Function)
- Maintains an active heartbeat to indicate service availability
- If the heartbeat fails multiple times, the NF re-registers automatically

### PDU Session Management
- Audio PDU Session Creation
- Video PDU Session Creation
- Session Deletion
- Supports dynamic JSON payloads for QoS configuration

### Note: The boilerplate for dynamic JSON payload can be found under `src/main/resources/dynamic` and must be tweaked according to the user's configurations.


## 3GPP Defined APIs Used
- NRF API - `/nnrf-nfm/v1/nf-instances`
- PCF API - `/npcf-policyauthorization/v1/app-sessions`

## 3GPP Technical Specifications Referred
| Specification Number | Release | Title | Purpose |
| ------ | ------- |  ------ | ------- |
| 29.510 | Release 16 | Network function repository services | To register a new NF Instance |
| 29.514 | Release 16 | Policy Authorization Service | To manage Audio/Video PDUs |


## Contributing
**Feature contributions are always welcome!**

**To Contribute-**

1. Fork the repo
2. Create a feature branch `git checkout -b feature-name`
3. Commit changes `git commit -m "Added feature X"`
4. Push to your fork `git push origin feature-name`
5. Open a pull request

## Configurations
Default configurations are placed under `defaults` folder.

### Logging
Log4j2 is used for logging the default configuration is available in `defaults/n5-logging.xml`

### Parameters
Default running parameters are available in `defaults/aether-ims-n5`

#### IP Address of SCP HTTP2 SBI Interface in 5G Core
SCP-IP=127.0.0.200  

#### Port of SCP HTTP2 SBI Interface in 5G Core
SCP-PORT=7777  

#### IP Address of PCF HTTP2 SBI Interface in 5G Core
PCF-IP=127.0.0.13  

#### Port of PCF HTTP2 SBI Interface in 5G Core
PCF-PORT=7777  

#### IP Address of N5-Interface HTTP2 SBI Interface
AF-BIND-IP=127.0.0.201  

#### Port of N5-Interface HTTP2 SBI Interface in 5G Core
AF-BIND-PORT=7777  

## Docker

### Build

From source code folder run

docker build -t n5-service .

### Run

docker run -d --name n5-container -p 8080:8080 n5-service

### Run Tests
docker exec -it n5-container mvn test -Dserver.port=8081 -Dapp.config=/app/config/aether-ims-n5.test

```plaintext
[INFO] Results:
[INFO]
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.627 s
[INFO] Finished at: 2025-03-29T14:11:44Z
[INFO] ------------------------------------------------------------------------```
```

### Gain access
docker exec -it n5-container /bin/bash

## License

| Library | License |
| ------ | ------- |
| Spring Framework | Apache License 2.0 |
| Spring Boot | Apache License 2.0 |
| Apache Log4j | Apache License 2.0 |
| OkHttp | Apache License 2.0 |
| Okio | Apache License 2.0 |
| INI4J | Apache License 2.0 |
| Jakarta Annotations | Eclipse Public License - v 2.0 |
| Javax Annotation | Eclipse Public License - v 2.0 |


## Acknowledgments
Special thanks to the **3GPP community** and **Spring Boot ecosystem** for making this possible.
