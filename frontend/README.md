# Swarmpit React frontend

React + strict TypeScript + Tailwind CSS 4 + generated shadcn/ui components + TanStack Query. The Clojure backend and Swarm agent remain Swarmpit's source of truth.

## Develop

Use Node 24 and run the backend on http://127.0.0.1:8080 using the existing Swarmpit development configuration. The frontend proxies APIs and SSE to that backend.

```sh
npm --prefix frontend ci
npm --prefix frontend run dev
```

Open the Vite address at /react/. Hash routes preserve Swarmpit's existing URL structure. Credentials and API tokens are never configured in frontend build variables. Login returns the complete authorization header value, which is forwarded unchanged.

## Checks

```sh
npm --prefix frontend test
npm --prefix frontend run typecheck
npm --prefix frontend run build
node --test scripts/upstream-update.test.mjs
```

The production build writes generated assets to resources/public/react/, which is ignored by Git and bundled into the JAR by Dockerfile.react. Do not commit generated assets or node_modules. The lockfile records exact dependency versions. Install actual shadcn components using npx shadcn@latest add <component> in frontend/; components.json owns aliases and the CSS entry. Keep Tailwind on major 4.

## Package and use

```sh
docker build -f Dockerfile.react -t swarmpit-react:local .
```

The custom image bundles both UIs and sets SWARMPIT_UI=react. The default upstream Dockerfile and default backend configuration retain the classic UI. In the custom image, / serves React and /react/index.html also works. /?ui=classic selects the original UI; its original hash routes remain usable. Set SWARMPIT_UI=classic when deploying the custom image to restore the classic default. This selection does not alter data or database volumes.

Use the original docker-compose.yml as the deployment reference, replacing only the app image with your built custom image; keep the agent, CouchDB, optional InfluxDB 1.x, volumes and Docker socket configuration. Do not deploy the local :check image or change production infrastructure as part of a test. If using an HTTP Docker endpoint in development, set both SWARMPIT_DOCKER_SOCK=http://<daemon>:2375 for the engine client and DOCKER_HOST=tcp://<daemon>:2375 for Docker CLI stack actions.

If React assets are missing while SWARMPIT_UI=react, the backend returns an explicit 503 with a classic-UI fallback. Rebuild with Dockerfile.react rather than serving an incomplete bundle.

## Disposable integration suite

The real integration test is skipped during ordinary npm test. It creates and destroys real Swarm resources and refuses an initialized instance. Use a new empty, isolated Docker-in-Docker daemon, a fresh CouchDB and a new application container. Do not mount a production Docker socket. The app must have exactly one node and no pre-existing services. The default test workload is busybox:1.36.1; pre-pull it inside the isolated daemon if needed.

```sh
SWARMPIT_TEST_URL=http://127.0.0.1:<disposable-app-port> \
SWARMPIT_DISPOSABLE_TEST=1 \
SWARMPIT_TEST_DIND_CONTAINER=<disposable-dind-container-name> \
npm --prefix frontend test -- src/integration/swarm.test.ts
```

The suite generates its test credentials in memory, uses unique resource names and attempts cleanup. The explicit DIND container name permits read-only container-reference checks before deleting a test-mounted volume; Docker can retain stopped task containers briefly after service removal. The suite does not retry destructive operations blindly. It tests the actual backend API used by the UI, not every visual interaction. A failed run may initialize the DB, so recreate only the disposable app/DB before a clean rerun. Never bypass the initialized-instance guard. Keep the isolated daemon only if cleanup succeeded and it is empty; otherwise recreate that disposable daemon too. Cloud provider credentials and hosted CI activation are separate verification requirements.

## Future updates

Start with [../AGENTS.md](../AGENTS.md), follow [../docs/UPSTREAM_UPDATES.md](../docs/UPSTREAM_UPDATES.md), and maintain [../docs/UI_PARITY.md](../docs/UI_PARITY.md). Fetching/merging upstream does not automatically port new UI behavior. Keep authorization, schemas, concurrency versions, SSE subscriptions, resource units and packaging aligned with upstream source code.
