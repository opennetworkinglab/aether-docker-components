# docker_aether_ims
This repository contains docker files to deploy an IMS solution using following projects:
- Core Network (4G/5G) - aether SD-Core - https://github.com/opennetworkinglab/aether-onramp
- IMS - kamailio - https://github.com/kamailio/kamailio
- IMS HSS - https://github.com/nickvsnetworking/pyhss

## Tested Setup

Docker host machine

- Ubuntu 22.04 or above

Over-The-Air setups:

- OCUDU Project gNB using Ettus USRP x310

## Prepare Docker images

* Mandatory requirements:
	* [docker-ce](https://docs.docker.com/install/linux/docker-ce/ubuntu) - Version 22.0.5 or above
	* [docker compose](https://docs.docker.com/compose) - Version 2.14 or above

You can either pull the pre-built docker images or build them from the source.

### Get Pre-built Docker images
TODO: Update procedure to use pre-built images

### Build Docker images from source

```
# Build docker image for open5gs EPC/5GC components
git clone https://github.com/opennetworkinglab/aether-docker-components.git

# Build docker image for kamailio IMS components
cd aether-docker-components/ims/ims_base
docker build --no-cache --force-rm -t docker_kamailio .

# Build docker images for additional components
cd ..
docker compose -f sa-vonr-ims-deploy.yaml build
```

## Network and deployment configuration

The IMS setup can be mainly deployed in two ways:

1. Single host setup where IMS/5GC are deployed on a single host machine
2. Multi host setup where IMS is deployed on a separate host machine than 5GC

### Deploy IMS

Edit only the following parameters in **.env** as per your setup
```
MCC
MNC
DOCKER_HOST_IP --> This is the IP address of the host running IMS
UPF_IP --> Change this to value of UPF DATA_IFACE IP
IMS_IP --> This is the IP address of the host running IMS
```

Edit only the following parameters in **aether-ims-n5** as per your setup
```
SCP-IP=10.206.1.30             --> aether SD-Core nrf EXTERNAL-IP (kubectl get svc -n aether-5gc)
SCP-PORT=29510                 --> aether SD-Core nrf PORT
PCF-IP=10.206.1.30             --> aether SD-Core pcf EXTERNAL-IP
PCF-PORT=29507                 --> aether SD-Core pcf PORT
AF-BIND-IP=10.206.1.100        --> same as IMS_IP configured above
AF-BIND-PORT=7783
AF-BIND-SBI-IP=10.206.1.100    --> same as IMS_IP configured above
AF-BIND-SBI=PORT=7782
```

Copy the file to /tmp folder,
```
cp aether-ims-n5 /tmp/
```

Deploy IMS,
```
cd docker_aether_ims
source .env
docker compose -f sa-vonr-ims-deploy.yaml up
```

NOTE: If IMS deployed on separate server than 5GC, add below route in IMS server (IMS UE SUBNET is assumed as 192.168.200.0/24),
```
sudo ip route add 192.168.200.0/24 via <DATA_IFACE IP OF UPF>
```
### Deploy SD-Core
Deploy SD-Core using IMS blueprint.

Add below route in SD-Core server (IMS UE SUBNET is assumed as 192.168.200.0/24),
```
sudo ip route add 192.168.200.0/24 via 192.168.250.3 dev core
```

## Provisioning of SIM information

### Provisioning of SIM information in pyHSS is as follows:

1. Goto http://<DOCKER_HOST_IP>:8080/docs/
2. Select **apn** -> **Create new APN** -> Press on **Try it out**. Then, in payload section use the below JSON and then press **Execute**

```
{
  "apn": "internet",
  "apn_ambr_dl": 0,
  "apn_ambr_ul": 0
}
```

Take note of **apn_id** specified in **Response body** under **Server response** for **internet** APN

Repeat creation step for following payload

```
{
  "apn": "ims",
  "apn_ambr_dl": 0,
  "apn_ambr_ul": 0
}
```

Take note of **apn_id** specified in **Response body** under **Server response** for **ims** APN

**Execute this step of APN creation only once**

3. Next, select **auc** -> **Create new AUC** -> Press on **Try it out**. Then, in payload section use the below example JSON to fill in ki, opc and amf for your SIM and then press **Execute**

```
{
  "ki": "8baf473f2f8fd09487cccbd7097c6862",
  "opc": "8E27B6AF0E692E750F32667A3B14605D",
  "amf": "8000",
  "sqn": 0,
  "imsi": "001010123456790"
}
```

Take note of **auc_id** specified in **Response body** under **Server response**

**Replace imsi, ki, opc and amf as per your programmed SIM**

4. Next, select **subscriber** -> **Create new SUBSCRIBER** -> Press on **Try it out**. Then, in payload section use the below example JSON to fill in imsi, auc_id and apn_list for your SIM and then press **Execute**

```
{
  "imsi": "001010123456790",
  "enabled": true,
  "auc_id": 1,
  "default_apn": 1,
  "apn_list": "1,2",
  "msisdn": "9076543210",
  "ue_ambr_dl": 0,
  "ue_ambr_ul": 0
}
```

- **auc_id** is the ID of the **AUC** created in the previous steps
- **default_apn** is the ID of the **internet** APN created in the previous steps
- **apn_list** is the comma separated list of APN IDs allowed for the UE i.e. APN ID for **internet** and **ims** APN created in the previous steps

**Replace imsi and msisdn as per your programmed SIM**

5. Finally, select **ims_subscriber** -> **Create new IMS SUBSCRIBER** -> Press on **Try it out**. Then, in payload section use the below example JSON to fill in imsi, msisdn, msisdn_list, scscf_peer, scscf_realm and scscf for your SIM/deployment and then press **Execute**

```
{
    "imsi": "001010123456790",
    "msisdn": "9076543210",
    "sh_profile": "string",
    "scscf_peer": "scscf.ims.mnc001.mcc001.3gppnetwork.org",
    "msisdn_list": "[9076543210]",
    "ifc_path": "default_ifc.xml",
    "scscf": "sip:scscf.ims.mnc001.mcc001.3gppnetwork.org:6060",
    "scscf_realm": "ims.mnc001.mcc001.3gppnetwork.org"
}
```

**Replace imsi, msisdn and msisdn_list as per your programmed SIM**

**Replace scscf_peer, scscf and scscf_realm as per your deployment**

## Not supported
- IPv6 usage in Docker
