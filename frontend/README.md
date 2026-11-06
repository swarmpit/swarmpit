# Swarmpit React frontend

React + strict TypeScript + Tailwind CSS 4 + generated shadcn/ui components + TanStack Query. The Clojure backend and Swarm agent remain Swarmpit's source of truth.

## Develop

Use Node 24 and run the backend on http://127.0.0.1:8080 using the existing Swarmpit development configuration. The frontend proxies APIs and SSE to that backend.

```sh
npm --prefix frontend ci
npm --prefix frontend run dev
```

Open the Vite address at /react/. Hash routes preserve Swarmpit's existing URL structure. Credentials and API tokens are never configured in frontend build variables. Login returns the complete authorization header value, which is forwarded unchanged.

The shell uses the shadcn sidebar-08 inset/icon pattern: the header toggle (or Ctrl/Cmd+B) collapses desktop navigation to an icon rail with tooltips, and mobile navigation opens a dismissible drawer. Desktop preference is stored in the `sidebar_state` cookie. Keep the local sidebar's cookie restore, mobile focus restoration, scrolling and role-filtered navigation when updating generated components. Sidebar colors reuse the original Swarmpit semantic theme in `src/styles.css`. The sidebar footer has one user item opening a shadcn dropdown with username/role, Theme → Light/Dark/System radio choices, account settings, Classic UI and sign-out; it works from the collapsed rail and mobile drawer. It follows the actual sidebar-08 NavUser composition, including an Avatar identity header and trigger-matched dropdown width. Breadcrumbs use the generated shadcn Breadcrumb components with React Router links to Overview and the current resource list; the current section remains clickable. Home displays a single Overview breadcrumb; other pages start with Overview followed by their resource section. Tooltip content mounts only in the collapsed desktop rail so hidden layers cannot intercept mobile Escape. The sidebar brand always reads Swarmpit with the application version from `/version.version`; the collapsed logo exposes both in its tooltip and accessible home link. Custom instance names remain in the document title.

UI and button icons use official `HugeiconsIcon` with named imports from `@hugeicons/core-free-icons`, `strokeWidth={2}` and semantic theme colors. Preserve decorative `aria-hidden`, button names, control sizes and loading/reduced-motion classes. `components.json` selects Hugeicons for future shadcn additions. The original Swarmpit logo remains its SVG asset.

All resource-history charts use the generated shadcn/ui chart components backed by Recharts: interactive area charts, themed legends/tooltips and keyboard navigation. Overview retains manager/worker counts, CPU capacity, CPU/memory/storage donuts, and separate node/service CPU/memory history charts. Four compact summary cards keep Storage beside Nodes, CPU and Memory; the service count lives with the cluster summary. Real node gauges are shared across the full Nodes page, Overview pins and node detail via `NodeCards`/`NodeResourceCharts`. Storage capacities use decimal units and memory uses binary units, matching the original UI. The shared chart adapter aligns each series by real timestamps, keeps missing samples as gaps, and preserves the backend units (node percentages, task/service vCPU and MiB). Charts retain last-hour/6-hour/24-hour presets, zoom/pan/reset, accessible series toggles and PNG export. Hidden series stay tied to resource identities through live reordering. Purple/olive strokes follow theme variables; resource donuts use warning/critical thresholds and a subtle neutral free-capacity track. Empty pins/history sections are omitted, with one useful disabled/no-history notice.

Initial data loads use shadcn Skeleton through the shared `Loading` component (`content`, `table`, `chart`). Dashboard values and chart queries load independently; cached results stay visible during background refetches. Disabled history queries and real empty/error states do not display indefinite skeletons. Loading regions have accessible names; decorative placeholders use a steady muted surface without pulse or shimmer. Their reserved dimensions stay unchanged.

Fixed-choice dropdowns use the generated shadcn Select component. Shared `Choice` preserves empty Default values and existing values absent from the current option list; encoding stays internal and API payloads retain their original values. Registry providers load before opening the menu, and compose-source selection preserves draft confirmation and disabled sources. Editable text suggestions continue accepting custom values. Native fieldset disabled states are also passed explicitly to Select roots.

Service cards and shared service tables retain published host ports/protocols and update/rollback status precedence. The full Nodes page keeps text search and a shadcn role selector alongside its resource charts.

Task details use Overview and Logs tabs, retaining `?log=1` links and supported logging-driver checks. Stack detail and compose pages keep their header/tabs/editor space during initial loading with matching Skeleton layouts; background fetches retain cached data and dirty drafts. Network details include a selectable topology of actual attached services with scoped aliases, task counts and service links.

For the populated local review environment with a real agent, InfluxDB, demo workloads and saved history, use [dev/review/README.md](../dev/review/README.md). Keep its named data volumes and existing administrator.

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
