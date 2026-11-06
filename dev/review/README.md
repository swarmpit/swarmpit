# Local populated review

This targets the existing, initialized review app at http://127.0.0.1:58019/react/. It preserves the admin account and existing database.

Run from the repository root with Docker Desktop and Node.js 24+ available:

```sh
node dev/review/start.mjs
SWARMPIT_REVIEW_CREDENTIALS_FILE=/private/path/review-credentials node dev/review/seed.mjs
```

The private file must have mode 600 and contain one `username:password` line for the existing administrator. Use a private editor to create it; do not put credentials in command arguments, shell history, repository files, or logs. The script prints counts and phases, never credentials, response bodies, or Docker inspection output. Its temporary API session is revoked on exit.

The startup helper configures the isolated Docker-in-Docker daemon, real Swarmpit agent and InfluxDB. Registry port 5000 and agent port 58020 are internal to the review environment; only app port 58019 is published on the host loopback interface. Required workload images are `nginx:alpine`, `redis:alpine`, and `alpine:3.20`; the local registry uses `registry:2`.

The seed uses the normal authenticated backend API and leaves these resources for review:

| View | Data |
| --- | --- |
| Dashboard | Current node and web/cache service pins, preserving previous pins |
| Metrics | Real agent CPU/memory samples, persisted in InfluxDB |
| Stacks | Active `review-demo` and inactive saved `review-demo-saved` |
| Compose/history | Exact stored YAML, previous revision and two commented deployments |
| Services/tasks | Two nginx replicas, Redis and a logging worker; healthchecks, environment, service/container labels and resource limits |
| Networks/volumes | Attachable overlay and named Redis data volume; worker tmpfs |
| Configs/secrets | Mounted nginx configuration and a public dummy secret |
| Logs | Worker heartbeat and internal HTTP traffic/access logs |
| Users | `review-demo-user` and `review-demo-viewer`, with generated passwords that are never printed or saved |
| Registries | Real unauthenticated local V2 registry linked to the admin; no external account credentials |

The workload limits total 0.45 CPU and 224 MiB across four replicas. The worker holds a 16 MiB tmpfs file and performs intermittent bounded hashing. No demo workload publishes a host port.

The script checks the exact loopback origin, app name, review label, Docker host and bound port, then compares API node IDs with the nested daemon before writing any fixture. It never initializes the app, deletes resources, changes existing users, or performs cleanup. Rerunning reuses named resources and does not add another compose revision. It refuses to overwrite a review stack whose stored compose was edited. To inspect after manual edits, use the UI; verification expects the seeded revision.

Verify again after restart without changing fixtures, deployment history or dashboard pins:

```sh
SWARMPIT_REVIEW_CREDENTIALS_FILE=/private/path/review-credentials node dev/review/seed.mjs --verify
```

Verification checks running replicas, stored compose/history, heartbeat logs, current cluster statistics and at least two real node/service metric samples. Initial collection may take a few minutes. When service graphs are still empty, normal seeding runs the backend's existing aggregation formulas over the actual collected samples and restarts only the review app to clear its 30-minute empty service-selection cache. It preserves every data volume. The `--verify` mode only reads data after login and never performs this refresh. History continues accumulating; the helper never fabricates samples or a past 24-hour history.

Check script guards without contacting Docker or the app:

```sh
node --test dev/review/seed.test.mjs
```

This is review data, not full UI parity or production validation. Cloud registry account types and multi-node scheduling require their own real environments.
