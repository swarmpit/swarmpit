# Configuration

Swarmpits behavior can be reconfigured via Environment Variables.

## `SWARMPIT_DOCKER_SOCK`
Docker SOCK location used by docker client. In case of socket proxy use following format: `http://TCPENDPOINT:2375`.
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
Keep credentials out of the URL; use the settings below for authenticated CouchDB.
See [deployment and upgrade instructions](COUCHDB_UPGRADE.md).

## `SWARMPIT_DB_USERNAME`
CouchDB administrator username. Set together with a password or password secret.
Default is `nil` (legacy unauthenticated connection).

## `SWARMPIT_DB_PASSWORD`
CouchDB password, supplied separately from the URL through HTTP Basic authentication.
Prefer a Docker secret in production. Default is `nil`.

## `SWARMPIT_DB_PASSWORD_SECRET`
Name of a Docker secret mounted at `/run/secrets/<name>`, read using the existing
Docker secret helper, which trims surrounding whitespace. Use a single-line
password without surrounding whitespace (the reference setup generates hex).
Takes precedence over `SWARMPIT_DB_PASSWORD`; a missing or
empty secret fails authentication setup instead of falling back to that password.
Default is `nil`.

## `SWARMPIT_INFLUXDB`
Swarmpit statistics database (InfluxDB) url. If `nil` statistics are disabled. 
Default is `nil`.

## `SWARMPIT_AGENT_URL`
Set address of agent. If `nil`, value is calculated dynamically. For DEV purpose only!!! 
Default is `nil`.

## `SWARMPIT_WORK_DIR`
Swarmpit working directory location.
Default is `/tmp`.

## `SWARMPIT_API_TOKEN_EXPIRY_DAYS`
Lifetime in days for personal API tokens generated in the UI. `nil` (the default) keeps the legacy behaviour where API tokens never expire. Set to a positive integer (e.g. `90`) to force rotation.
Default is `nil`.

## `SWARMPIT_AGENT_TOKEN`
Shared secret required on `POST /events`, the endpoint the swarmpit agent pushes node statistics and docker events to. `nil` (the default) accepts unauthenticated pushes, which is what released agents send. When set, point your agents at `EVENT_ENDPOINT=http://app:8080/events?token=<secret>`, or have them send an `X-Swarmpit-Agent-Token` header; pushes without a matching token get a 401 and statistics stop working.
Default is `nil`.

## `SWARMPIT_INSTANCE_NAME`
Custom name shown in place of the swarmpit logo in the sidebar and top bar, and prepended to the browser tab title as `{instance_name} :: {page} :: swarmpit`. Useful when running multiple swarmpit instances against different clusters so you can tell them apart at a glance.
Default is `nil` (shows the swarmpit logo).
