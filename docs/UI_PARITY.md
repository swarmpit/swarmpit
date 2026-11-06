# React UI parity and verification ledger

Baseline: upstream/master 65b2c99ffbf07ca986c3a5f4f5bd98ceecf8a8b9. Latest verification: 2026-11-06. Read this with AGENTS.md and UPSTREAM_UPDATES.md. Implementation and live verification are different; preserve the evidence and limits below when updating this ledger.

## Latest independent review and validation — 2026-11-06

A fresh independent reviewer compared the entire React branch with upstream/master, including the connected legacy pages, request specs, handlers and mappers. All five confirmed findings were fixed:

- Keep the prebuilt target/swarmpit.jar in the Docker context so the original Dockerfile remains buildable.
- Replace explicitly submitted containerLabels/sysctls maps on service updates, including removals; preserve omitted fields for older clients, mapper-generated stack metadata and unrelated Docker fields.
- Serialize config/secret file reads and disable switching back to text during reads/submission. Deferred-read regressions cover success, failure, recovery and exact binary payloads.
- Restore named clickable service links with safe URL protocols and opener isolation.
- Gate service and task logs through the shared supported-driver predicate, including legacy log=1 links.

An independent re-review identified the pending-upload text-switch edge, which was also fixed. No other actionable blocker was reported. CI now covers pull requests to upstream master, and CONTRIBUTING.md documents optional React development/builds.

Fresh verification of these fixes:

- Frontend: 237 tests passed, one opt-in integration test skipped in the default suite; strict TypeScript and production build passed.
- Real integration: separately enabled and passed in 53.40 seconds against fresh CouchDB and a single-node Docker-in-Docker Swarm with no host socket. All ten lifecycle phases passed: setup/auth/roles, parsed SSE snapshots/fresh tokens, resource CRUD, advanced service edits, container-label/sysctl replacement and clearing, service/stack actions and logout revocation. The initialization assertion waits for the existing one-second /version memoization.
- Backend: lein check passed; full lein test passed with 86 tests, 370 assertions and no failures/errors against disposable CouchDB.
- Helpers: all seven upstream-checker/review-seed tests passed. Both new workflows passed actionlint locally.
- Packaging: Dockerfile.react and the original Dockerfile both built successfully. React image swarmpit-react:check is sha256:2951d04a0e1b959a73858d62c595b8912bf5c7e203ba2d25dad81e390988107c. HTTP checks confirmed React root/direct entry/assets, the query-selected classic entry and the original image's default classic entry/bundle.
- Local review: the guarded startup helper refreshed the app at http://127.0.0.1:58019 to the fixed image and updated its real agent callback. Initialized/statistics capabilities and original CouchDB/nested-Swarm container identities were checked; review data volumes remain intact.
- No production Swarm or data was used. Disposable validation containers/network are removed after verification; the separate local review environment remains available.

Current limits: this pass did not add a fresh browser interaction/screenshot audit, cloud/private-registry credentials or multi-node testing. Earlier browser and real review-agent/InfluxDB evidence is recorded below. Admin node mutations, own-password and API-token actions still lack real integration-suite coverage. Hosted workflows have only local validation until GitHub runs them. Existing Clojure reflection warnings and the Vite main-chunk size warning remain. These results do not establish exhaustive feature parity.

The sections below preserve earlier implementation and verification evidence; their test counts and absence-of-agent notes describe their original stage, not this latest run.

## Local review additions

The initialized isolated review environment now runs a real Swarmpit agent and InfluxDB, with seeded services/tasks, stored compose/history, a local V2 registry, user roles and dashboard pins. Current API verification confirms real node/service samples and retained review data. See [dev/review/README.md](../dev/review/README.md).

The shell follows the shadcn sidebar-08 inset/icon pattern with the original purple theme. Its logo reads Swarmpit with the backend application version; collapsed navigation retains a named native link to https://swarmpit.io/ and version tooltip. The brand keeps its normal background/text on hover and press, with keyboard focus indication retained. Resource charts use official shadcn/Recharts components; initial data loads use accessible shadcn skeletons while background refreshes retain cached data. Current frontend verification: 178 tests pass, one opt-in destructive Swarm test remains skipped, strict TypeScript and production build pass. Desktop/mobile shell geometry and controls were checked in the browser earlier; the later UI changes initially had component-test evidence only. The new re-audit adds browser DOM/computed-style and interaction checks described below; full-page screenshot tools still time out. The preceding UI queue restored manager/worker counts, capacity donuts and four distinct node/service area charts, keeps Storage in the summary row, adds clickable breadcrumbs and a user dropdown/theme submenu, moves task logs into a tab, stabilizes initial stack layouts, adds a data-backed network topology and reduces Live to a compact radio-icon badge. Component tests include mobile Escape, theme persistence, task log cancellation and stack page continuity. The original port evidence below is retained as a historical baseline, not a claim that the review environment still lacks an agent or InfluxDB.

## Overview and node re-audit

The earlier port review missed per-node CPU/memory/storage gauges in pins, the full Nodes list and node detail; published service ports and deployment-state priority; utilization severity; and history graph controls. These are now restored through shared node cards/charts and service state handling. The source comparison also identified excessive empty panels and a five-card layout wrapping into three rows. Overview now has four compact summaries, paired desktop histories and hidden empty pin/history sections. The service count remains visible in the cluster summary.

Current graph controls include the original last-hour default, longer range presets, zoom/pan/reset, keyboard series toggles and themed PNG export. Charts use purple/olive theme-derived colors; utilization warnings follow original thresholds. The user dropdown follows the shadcn sidebar-08 NavUser composition. Swagger API and Classic UI use a secondary sidebar group above the user item; this placement is covered by component tests and postdates the browser review recorded here. All 178 frontend tests pass, one opt-in destructive test is skipped, and strict TypeScript/production build pass. Browser checks at 1280px and 390px confirm four aligned desktop summaries, no horizontal overflow, actual node charts in pins/list/detail, themed light/dark strokes, a bounded mobile user menu and Escape focus restoration, zoom/reset and a real PNG download. Inspecting that PNG exposed a Recharts tick-selector mismatch; the shared wrapper now applies the theme text token to the actual tick-value class. Full-page screenshots timed out and the desktop browser host was unavailable, so no complete pixel-level screenshot review is claimed. Preserve these limits; do not infer visual verification from the tests.

UI icon controls now use Hugeicons with named free-icon imports and the official renderer, preserving labels, sizes, selected indicators and reduced-motion spinners. Shadcn is configured for Hugeicons and the unused Lucide dependency is removed. Source and component checks cover shell/menu/select/dialog, forms, charts and resource controls; this is an icon-family migration, not a new functionality-parity claim.

## Initial port feature coverage and historical verification

| Area | React implementation | Verification |
| --- | --- | --- |
| Session/setup | First administrator, login, return route, current user, persisted authorization, immediate local logout with server revocation, expiry, cross-tab changes and storage failures | API/session tests; real setup/auth/roles/revocation suite |
| Shell | Responsive navigation, original hash routes, accessible skip navigation, loading/empty/error states, system/light/dark theme, instance name and classic UI access | HashRouter accessibility test, typecheck/build; packaged entry and assets checked by HTTP |
| Dashboard | Cluster counts/current statistics, personalized pins, node/service history charts, optional-statistics states/errors | Correct unit/chart/cache tests; real pin/unpin; no live InfluxDB/agent deployed |
| Services | Lists/details, image wizard and provider browsing, repository/tag/port selection, native advanced create/edit, redeploy/tag/rollback/stop/delete, logs/search/follow/download/task filtering, syntax-highlighted generated Compose and compose export, tasks/networks/metrics, pins | Payload/form/normalization/permissions/dirty-state tests; real create/edit/log/task/compose/redeploy/rollback/stop/delete and pin tests |
| Service fields | Ports, bind/volume/tmpfs/options, network aliases, secrets/configs, environment, hosts, separate labels, command/entrypoint, hostname/isolation/sysctls, resources, healthcheck, deployment/restart/update/rollback/placement, links/immutability | Compared with actual create/update specs; real advanced edit includes aliases/resources/labels/mounts. Some fields are upstream update-only. Backend mapper does not write user/dir, so these are read-only rather than misleading editable controls |
| Stacks | Deployed/inactive lists, related tabs, syntax-highlighted stored/reconstructed/historical Compose and editable YAML, highlighted engine comparison blocks, file upload/save/deploy, comments/skip resolution, history/previous/last, engine comparison/selective restore, redeploy/rollback/deactivate/reactivate/delete | 21 focused stack tests plus real deploy/edit/history/deactivate/reactivate/delete round trip. History intentionally deduplicates changes that do not alter reconstructed engine compose |
| Nodes/tasks | Lists/details, live state/removal, canonical/name aliases, current node CPU/memory/disk, plugins, task-table CPU Usage (% and vCPU), Memory Usage (% and binary capacity), Last update and final State column, running-task resource usage, historical charts, node administration/pins, legacy task log links | Focused live/metrics/log tests; node edit preserves original optimistic version and offers confirmed reload; real user node-update denial. Actual agent metrics and admin node mutations not exercised live |
| Networks/volumes | Lists/details, plugin drivers/options, network IPAM/flags, related services, native creation, deletion confirmations/errors | Real network/volume CRUD. Service volumes verified mounted; cleanup waits for retained Docker task containers before deletion |
| Secrets/configs | Lists/details, byte-preserving text/file upload, UTF-8/base64 payloads, syntax-highlighted config Value editor and read-only viewer with YAML/JSON/plain-text selection, metadata/content as supported, related services, deletion | Config editor/flow tests cover highlighting, required values, Unicode payloads, binary uploads and read-only details; real create/list/detail/delete previously verified. Docker intentionally does not return secret contents |
| Registries | Docker Hub, V2 (GHCR via V2), ECR, ACR, GitLab; provider fields, create/edit/delete, ownership/public boundaries, repositories/tags/ports and service handoff | UI/payload/credential-preservation tests and shared-public image-selection regression. Live private/cloud credentials not supplied |
| Users/account | Admin user CRUD/roles, password change, API token generation/replacement/revocation and one-time display | Account/user UI/payload tests; real admin/operator/viewer creation, permissions and JWT revocation. Password/API-token actions covered by UI tests, not the real Swarm suite |
| Live updates | Route-owned EventSource, exact base64 EDN, fresh single-use tokens, reconnect/navigation cleanup, snapshots, removed tasks and alias/canonical cache keys | Event/cache tests; real consumed/fresh SLT and parsed service-list snapshots via authenticated agent ingress. No actual agent event emitter deployed |
| Packaging | Dockerfile.react bundles both UIs; SWARMPIT_UI selects default; original Dockerfile/default stay classic; classpath /react/ assets and query rollback | Image built; actual root React/direct React/classic fallback, hashed JS/CSS and icon requests pass. Missing assets covered by backend 503 tests |
| Upstream updates | Root agent instructions, exact manual procedure, baseline, non-mutating checker, scheduled/manual PR proposal and explicit reusable verification | Four Git fixture tests and current baseline no-op pass; workflows pass actionlint. Hosted Actions not activated/run |

## Initial port proof (historical)

- Frontend: 86 tests passed; one opt-in real integration test skipped in the default suite. Strict TypeScript and production Vite build pass.
- Opt-in real integration: separately enabled and passed, one full lifecycle test with ten phases, 67.56 seconds, against a fresh isolated Docker-in-Docker Swarm and CouchDB. Covers setup/auth/roles, consumed/fresh SSE tokens, parsed live snapshots, network/volume/config/secret operations, service lifecycle, stack lifecycle and logout revocation. Uses the same upstream API backend; final UI entry changes were subsequently checked in the final packaged image.
- Backend: lein check passes. Full lein test with a disposable CouchDB: 85 tests, 334 assertions, zero failures/errors. UI entry-specific tests: five tests, 18 assertions.
- Upstream checker: four fixture tests pass (no-op, divergent clean merge, conflicts, invalid refs). Baseline check returns changed=false/conflict=false and leaves checkout/HEAD unchanged.
- Workflows: actionlint passes for react-ui.yml and upstream-sync.yml; no hosted workflow run is claimed.
- Packaging: final Dockerfile.react build passes. Image swarmpit-react:check, SHA 7ffdf7223c7045cd4bb293c3bb6608e57b3eff81adaa461c0570ac236648c2d8. HTTP checks pass for React default without legacy script injection, identical /react/index.html, classic /?ui=classic with its original scripts, hashed JS/CSS, and brand icon.
- Review: independent full review findings were addressed; an independent scoped re-review found no actionable residual defect in those fixes.
- Existing upstream Clojure reflection warnings remain. Vite reports the main JS chunk slightly over 500 kB (about 158 kB gzip); no functional build errors.

## Initial port runtime/activation limits (historical)

- No real browser interaction/accessibility audit, private/cloud registry credentials, live InfluxDB, or actual Swarm agent deployment was used. Current metrics, logs and optional-absence handling have source-based and component-test evidence; agent/native backend behavior is retained.
- Admin node changes, own-password and API-token actions were not exercised in the real Swarm suite; relevant backend behavior remains upstream and UI tests cover the forms/requests.
- Hosted workflows remain local code until the branch is published. Scheduling additionally requires installing the updater on the default branch, Actions permissions and manual authorization to enable repository settings. See UPSTREAM_UPDATES.md.
- No production Swarm or data was touched. Disposable test containers/networks were removed after verification; the built image remains available locally. A separate isolated local review environment is left running when the user requests it, with its own database and nested Swarm.
- The user authorized committing and pushing the implementation on 2026-11-05. The implementation branch is react-ui in https://github.com/sldone/swarmpit. Verify its actual remote HEAD before reporting publication; no PR, production deployment or hosted workflow activation is implied.

Future agents: update this ledger and upstream-baseline.json only after inspecting the actual upstream change and running its checks. Preserve unverified items explicitly; a static build or clean merge does not establish full runtime parity.
