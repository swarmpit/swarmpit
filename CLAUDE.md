# Swarmpit

Lightweight Docker Swarm management UI built with Clojure/ClojureScript.

## Tech Stack

- **Backend:** Clojure (Ring, Reitit, http-kit)
- **Frontend:** ClojureScript (Rum/React, Material-UI, Recharts)
- **Data:** CouchDB (persistence), InfluxDB (metrics)
- **Build:** Leiningen
- **Runtime:** Java 17, Docker

## Project Structure

```
src/clj/       # Backend Clojure code
src/cljs/      # Frontend ClojureScript code
src/cljc/      # Shared code (backend + frontend)
src/java/      # Java helpers (Unix socket)
test/clj/      # Backend tests
dev/            # Dev REPL setup and scripts
resources/public/ # Static frontend assets
```

## Build Commands

```bash
lein deps                       # Install dependencies
lein repl                       # Start dev REPL (then call (fig-start))
lein test                       # Run unit tests (excludes :integration)
lein test :integration          # Run integration tests (needs Docker socket)
lein test :all                  # Run all tests
lein with-profile prod uberjar  # Build production JAR -> target/swarmpit.jar
docker build -t swarmpit .      # Build Docker image (requires JAR first)
```

## Running

The app runs on port 8080. Docker Compose stack includes: app, agent, CouchDB (5984), InfluxDB (8086).

Key env vars: `SWARMPIT_DB` (CouchDB URL), `SWARMPIT_INFLUXDB` (InfluxDB URL).

## Entry Points

- Backend main: `swarmpit.server` (project.clj :main)
- Frontend main: `swarmpit.app` (ClojureScript entry)
- Dev REPL ns: `repl.user`
