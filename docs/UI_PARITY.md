# React UI parity and verification ledger

Baseline: upstream/master 65b2c99ffbf07ca986c3a5f4f5bd98ceecf8a8b9. Verified locally on 2026-11-05. Read this with AGENTS.md and UPSTREAM_UPDATES.md. Implementation and live verification are different; preserve the evidence and limits below when updating this ledger.

## Implemented feature coverage

| Area | React implementation | Verification |
| --- | --- | --- |
| Session/setup | First administrator, login, return route, current user, persisted authorization, immediate local logout with server revocation, expiry, cross-tab changes and storage failures | API/session tests; real setup/auth/roles/revocation suite |
| Shell | Responsive navigation, original hash routes, accessible skip navigation, loading/empty/error states, system/light/dark theme, instance name and classic UI access | HashRouter accessibility test, typecheck/build; packaged entry and assets checked by HTTP |
| Dashboard | Cluster counts/current statistics, personalized pins, node/service history charts, optional-statistics states/errors | Correct unit/chart/cache tests; real pin/unpin; no live InfluxDB/agent deployed |
| Services | Lists/details, image wizard and provider browsing, repository/tag/port selection, native advanced create/edit, redeploy/tag/rollback/stop/delete, logs/search/follow/download/task filtering, compose export, tasks/networks/metrics, pins | Payload/form/normalization/permissions/dirty-state tests; real create/edit/log/task/compose/redeploy/rollback/stop/delete and pin tests |
| Service fields | Ports, bind/volume/tmpfs/options, network aliases, secrets/configs, environment, hosts, separate labels, command/entrypoint, hostname/isolation/sysctls, resources, healthcheck, deployment/restart/update/rollback/placement, links/immutability | Compared with actual create/update specs; real advanced edit includes aliases/resources/labels/mounts. Some fields are upstream update-only. Backend mapper does not write user/dir, so these are read-only rather than misleading editable controls |
| Stacks | Deployed/inactive lists, related tabs, stored/reconstructed compose, file upload/save/deploy, comments/skip resolution, history/previous/last, engine comparison/selective restore, redeploy/rollback/deactivate/reactivate/delete | 21 focused stack tests plus real deploy/edit/history/deactivate/reactivate/delete round trip. History intentionally deduplicates changes that do not alter reconstructed engine compose |
| Nodes/tasks | Lists/details, live state/removal, canonical/name aliases, current node CPU/memory/disk, plugins, running-task resource usage, historical charts, node administration/pins, legacy task log links | Focused live/metrics/log tests; node edit preserves original optimistic version and offers confirmed reload; real user node-update denial. Actual agent metrics and admin node mutations not exercised live |
| Networks/volumes | Lists/details, plugin drivers/options, network IPAM/flags, related services, native creation, deletion confirmations/errors | Real network/volume CRUD. Service volumes verified mounted; cleanup waits for retained Docker task containers before deletion |
| Secrets/configs | Lists/details, byte-preserving text/file upload, UTF-8/base64 payloads, metadata/content as supported, related services, deletion | Unicode payload tests and real create/list/detail/delete. Docker intentionally does not return secret contents |
| Registries | Docker Hub, V2 (GHCR via V2), ECR, ACR, GitLab; provider fields, create/edit/delete, ownership/public boundaries, repositories/tags/ports and service handoff | UI/payload/credential-preservation tests and shared-public image-selection regression. Live private/cloud credentials not supplied |
| Users/account | Admin user CRUD/roles, password change, API token generation/replacement/revocation and one-time display | Account/user UI/payload tests; real admin/operator/viewer creation, permissions and JWT revocation. Password/API-token actions covered by UI tests, not the real Swarm suite |
| Live updates | Route-owned EventSource, exact base64 EDN, fresh single-use tokens, reconnect/navigation cleanup, snapshots, removed tasks and alias/canonical cache keys | Event/cache tests; real consumed/fresh SLT and parsed service-list snapshots via authenticated agent ingress. No actual agent event emitter deployed |
| Packaging | Dockerfile.react bundles both UIs; SWARMPIT_UI selects default; original Dockerfile/default stay classic; classpath /react/ assets and query rollback | Image built; actual root React/direct React/classic fallback, hashed JS/CSS and icon requests pass. Missing assets covered by backend 503 tests |
| Upstream updates | Root agent instructions, exact manual procedure, baseline, non-mutating checker, scheduled/manual PR proposal and explicit reusable verification | Four Git fixture tests and current baseline no-op pass; workflows pass actionlint. Hosted Actions not activated/run |

## Final proof

- Frontend: 86 tests passed; one opt-in real integration test skipped in the default suite. Strict TypeScript and production Vite build pass.
- Opt-in real integration: separately enabled and passed, one full lifecycle test with ten phases, 67.56 seconds, against a fresh isolated Docker-in-Docker Swarm and CouchDB. Covers setup/auth/roles, consumed/fresh SSE tokens, parsed live snapshots, network/volume/config/secret operations, service lifecycle, stack lifecycle and logout revocation. Uses the same upstream API backend; final UI entry changes were subsequently checked in the final packaged image.
- Backend: lein check passes. Full lein test with a disposable CouchDB: 85 tests, 334 assertions, zero failures/errors. UI entry-specific tests: five tests, 18 assertions.
- Upstream checker: four fixture tests pass (no-op, divergent clean merge, conflicts, invalid refs). Baseline check returns changed=false/conflict=false and leaves checkout/HEAD unchanged.
- Workflows: actionlint passes for react-ui.yml and upstream-sync.yml; no hosted workflow run is claimed.
- Packaging: final Dockerfile.react build passes. Image swarmpit-react:check, SHA 7ffdf7223c7045cd4bb293c3bb6608e57b3eff81adaa461c0570ac236648c2d8. HTTP checks pass for React default without legacy script injection, identical /react/index.html, classic /?ui=classic with its original scripts, hashed JS/CSS, and brand icon.
- Review: independent full review findings were addressed; an independent scoped re-review found no actionable residual defect in those fixes.
- Existing upstream Clojure reflection warnings remain. Vite reports the main JS chunk slightly over 500 kB (about 158 kB gzip); no functional build errors.

## Remaining runtime/activation limits

- No real browser interaction/accessibility audit, private/cloud registry credentials, live InfluxDB, or actual Swarm agent deployment was used. Current metrics, logs and optional-absence handling have source-based and component-test evidence; agent/native backend behavior is retained.
- Admin node changes, own-password and API-token actions were not exercised in the real Swarm suite; relevant backend behavior remains upstream and UI tests cover the forms/requests.
- Hosted workflows remain local code until the branch is published. Scheduling additionally requires installing the updater on the default branch, Actions permissions and manual authorization to enable repository settings. See UPSTREAM_UPDATES.md.
- No production Swarm or data was touched. Disposable test containers/networks were removed after verification; the built image remains available locally. A separate isolated local review environment is left running when the user requests it, with its own database and nested Swarm.
- The user authorized committing and pushing the implementation on 2026-11-05. The implementation branch is react-ui in https://github.com/sldone/swarmpit. Verify its actual remote HEAD before reporting publication; no PR, production deployment or hosted workflow activation is implied.

Future agents: update this ledger and upstream-baseline.json only after inspecting the actual upstream change and running its checks. Preserve unverified items explicitly; a static build or clean merge does not establish full runtime parity.
