# JVM Profiling

Swarmpit bundle support for JVM profiling either via **JMX** (VisualVM, JConsole, ...) or **YourKit Java Profiler**.

## JMX

The following options should be added to java executable command via `JAVA_TOOL_OPTIONS`

```
-Djava.rmi.server.logCalls=true
-Djava.rmi.server.hostname=<host ip>
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=<port>
-Dcom.sun.management.jmxremote.rmi.port=<port>
-Dcom.sun.management.jmxremote.local.only=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```

### Example

Expose metrics on `localhost` with port `3333`. This is typical use-case if you are using **Docker for Mac** with
**Hyperkit** under the hood.

```yaml
services:
  app:
    image: swarmpit/swarmpit:latest
    environment:
     JAVA_TOOL_OPTIONS: |
       -Djava.rmi.server.logCalls=true
       -Djava.rmi.server.hostname=localhost
       -Dcom.sun.management.jmxremote=true
       -Dcom.sun.management.jmxremote.port=3333
       -Dcom.sun.management.jmxremote.rmi.port=3333
       -Dcom.sun.management.jmxremote.local.only=false
       -Dcom.sun.management.jmxremote.authenticate=false
       -Dcom.sun.management.jmxremote.ssl=false
      SWARMPIT_DB=http://db:5984
      SWARMPIT_INFLUXDB=http://influxdb:8086
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    ports:
      - 888:8080
      - 3333:3333
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080"]
      interval: 60s
      timeout: 10s
      retries: 3
    networks:
      - net
    deploy:
      resources:
        limits:
          cpus: '0.50'
          memory: 1024M
        reservations:
          cpus: '0.25'
          memory: 512M
      placement:
        constraints:
          - node.role == manager
```

Run `visualvm` or `jconsole`

1. File -> `Add JMX Connection...`
2. Enter the `host` and `port` (e.g. `localhost:8849`).

and you are set.

## YourKit Java Profiler

In order to attach YourKit Java Profiler entry command of swarmpit container must include YourKit agent configuration
corresponding with your architecture.

For `x86` use:
```
-agentpath:/usr/local/YourKit-JavaProfiler-2020.9/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all
```

For `ARM` use:
```
-agentpath:/usr/local/YourKit-JavaProfiler-2020.9/bin/linux-arm-64/libyjpagent.so=port=10001,listen=all
```

### Example

Setup YourKit agent for `x86` architecture and expose for example on port `889`.

```yaml
services:
  app:
    image: swarmpit/swarmpit:latest
    command:
      - java
      - -agentpath:/usr/local/YourKit-JavaProfiler-2020.9/bin/linux-x86-64/libyjpagent.so=port=10001,listen=all
      - -jar
      - swarmpit.jar
    environment:
      SWARMPIT_DB=http://db:5984
      SWARMPIT_INFLUXDB=http://influxdb:8086
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    ports:
      - 888:8080
      - 889:10001
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080"]
      interval: 60s
      timeout: 10s
      retries: 3
    networks:
      - net
    deploy:
      resources:
        limits:
          cpus: '0.50'
          memory: 1024M
        reservations:
          cpus: '0.25'
          memory: 512M
      placement:
        constraints:
          - node.role == manager
```

Run `YourKit-JavaProfiler-2020.9`

1. File -> `Add Remote Application...`
2. Choose simple discovery method with `port` (e.g. `889`).

and you are set.