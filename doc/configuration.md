# Configuration

<p class="description">Swarmpit behavior can be reconfigured via following Environment Variables.</p>

## Environment variables

<p class="description">Swarmpit behavior can be reconfigured via following Environment Variables.</p>

| Variable                       | Default                 | Description                                                                               |
|:-------------------------------|:------------------------|:------------------------------------------------------------------------------------------|
| `SWARMPIT_DOCKER_SOCK`         | `/var/run/docker.sock`  | Docker SOCK location used by docker client                                                |
| `SWARMPIT_DOCKER_API`          | `1.30`                  | Docker API version used by docker client                                                  |
| `SWARMPIT_DOCKER_HTTP_TIMEOUT` | `5000`                  | Docker client http timeout in ms.                                                         |
| `SWARMPIT_DB`                  | `http://localhost:5984` | Swarmpit application database (CouchDB) url.                                              |
| `SWARMPIT_INFLUXDB`            | `nil`                   | Swarmpit statistics database (InfluxDB) url. If `nil` statistics are disabled.            |
| `SWARMPIT_AGENT_URL`           | `nil`                   | Swarmpit agent url. If `nil`, value is calculated dynamically. For DEV purpose only!!!    |
| `SWARMPIT_WORK_DIR`            | `/tmp`                  | Swarmpit working directory location.                                                      |

## Service deployment labels

### Autoredeploy
Periodically check for new image version and update service accordingly.

```yaml
swarmpit.service.deployment.autoredeploy: 'true'
```

### Custom link
Swarmpit will add Button/CTA on service detail with the URL.

In order to use word separator use `_`.

```yaml
swarmpit.service.link.application: https://swarmpit.io/
swarmpit.service.link.git_repository: https://github.com/swarmpit/swarmpit
```

### Immutable mode
Disable service configuration/editing via UI.

Typical use case for this is an automation via IaC when you want to have audit-able
and versioned changes to your stacks.

```yaml
swarmpit.service.immutable: 'true'
```