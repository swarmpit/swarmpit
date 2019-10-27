# Configuration

Swarmpits behavior can be reconfigured via Environment Variables.

## `SWARMPIT_DOCKER_SOCK`
Docker SOCK location used by docker client.
Default is `/var/run/docker.sock`.

## `SWARMPIT_DOCKER_API`
Docker API version used by docker client.
Default is `1.30`.

## `SWARMPIT_DOCKER_HTTP_TIMEOUT`
Docker client http timeout in ms.
Default is `5000`.

## `SWARMPIT_DB`
Swarmpit database (CouchDB) url. 
Default is `http://localhost:5984`.

## `SWARMPIT_INFLUXDB`
Swarmpit statistics database (InfluxDB) url. If `nil` statistics are disabled. 
Default is `nil`.

## `SWARMPIT_AGENT_URL`
Set address of agent. If `nil`, value is calculated dynamically. For DEV purpose only!!! 
Default is `nil`.

## `SWARMPIT_WORK_DIR`
Swarmpit working directory location.
Default is `/tmp`.