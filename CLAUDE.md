# swarmpit

lightweight docker swarm management ui. clojure backend + clojurescript (rum/react) frontend, couchdb for app data, influxdb 1.x (optional) for stats. deployed via `docker stack deploy`. see `docker-compose.yml` for the reference stack: `app` + [`agent`](https://github.com/swarmpit/agent) + `db` (couchdb) + `influxdb:1.8`.

## build / test / dev

- `lein check` — quick compile sanity gate, run after any clj edit
- `lein test` — unit tests (`:default` selector excludes `^:integration`)
- `lein test :integration` — integration tests; need dind running (`dev/script/start-dind.sh`)
- `lein test :all` — everything
- `lein uberjar` — production jar (`target/swarmpit.jar`, entrypoint `swarmpit.server/-main`)
- `lein figwheel` — dev: starts nrepl + figwheel, auto-reloads cljs on save. backend reloaded via ring-reload (so most handler changes hot-reload too). the figwheel bootstrap runs in `dev/repl/user.clj`: `fig-start` calls `dev/script/init-db.sh`, `init-agent.sh`, `init-influx.sh` to bring up supporting containers, then `db/init`, `agent/init`, `setup/docker`, `setup/log`.
- repl-driven: `(fig-start)` / `(fig-stop)` / `(cljs-repl)` in `repl.user`.

build chain: `cljsbuild` with two builds — `app` (dev, figwheel-hooked) and `min` (advanced-compile for uberjar). `:main` is `swarmpit.server`. `uberjar` profile inherits `prod` which AOTs everything and runs `cljsbuild once min`.

## source layout

```
src/clj/swarmpit/          backend
  api.clj                  top-level api fns (services, stacks, registries, users, …)
  handler.clj              ring handlers, resp-ok / resp-error helpers, login
  server.clj               -main, middleware stack, reitit router
  database.clj             boot init for couch + influx; influx wrapped in try/catch
  setup.clj                docker api version negotiation via /_ping
  config.clj               config atoms: @default ← environment ← @dynamic (later wins)
  version.clj              /api/version response body
  authorization.clj        access rules (buddy-auth, regex per path)
  agent.clj                schedules stats collection, event push
  stats.clj                stats collector; influx writes wrapped in try/catch
  slt.clj                  short-lived tokens for SSE auth
  docker/engine/
    http.clj               docker http client over unix socket, per-request conn-mgr
    client.clj             docker api methods; ping uses :unversioned? option
    cli.clj                shells out to docker cli for `stack deploy`, `stack rm`
    mapper/inbound.clj     docker spec → swarmpit domain
    mapper/outbound.clj    swarmpit domain → docker spec
    mapper/compose.clj     swarmpit domain → compose yaml
  couchdb/
    client.clj             couchdb http client
    migration.clj          run-once migrations, tracked in db
  influxdb/
    client.clj             influxdb 1.x client; queries raw influxql
  event/
    handler.clj            SSE endpoint via http-kit with-channel
    channel.clj            subscriber hub (atom of channel → request)
    processor.clj          event dispatch
  agent/                   agent client (used by service-agent-logs etc.)

src/cljs/swarmpit/         frontend (rum components)
  app.cljs                 entry
  ajax.cljs                wraps cljs-ajax, attaches Authorization: <jwt> from localStorage
  storage.cljs             localStorage wrappers
  event/source.cljs        js/EventSource client, auto-reconnects every 5s
  component/
    state.cljs             single app atom + cursors (docker-api-cursor, layout-cursor, form-value-cursor, …)
    layout.cljs            document-title + page routing switch
    menu.cljs              drawer / sidebar
    header.cljs            appbar
    common.cljs            title-logo, title-version, misc
    service/               service views (edit, info, form-*)
    stack/                 stack create / compose edit
    node/ network/ volume/ secret/ config/ registry/ user/

src/cljc/swarmpit/         shared
  routes.cljc              reitit routes; two top-level vars — `backend` (full reitit, with :handler) and `frontend` (plain [path name] pairs for cljs routing). helpers: path-for-backend, path-for-frontend.
  routes_spec.cljc         request/response schemas (spec-tools data-spec)
  yaml.cljc                yaml encode/decode (snakeyaml on jvm, js-yaml on cljs)
  utils.cljc               clean, clean-nils, name-value↔map

test/clj/swarmpit/         unit + ^:integration tests
resources/public/          css (main.css, ~1700 lines), fonts, img/{logo,icon}.svg
doc/                       configuration.md, USER_CONFIG.md, user_types.md
dev/script/                init-db.sh, init-agent.sh, init-influx.sh, start-dind.sh, …
dev/repl/user.clj          figwheel bootstrap (fig-start, cljs-repl)
.github/workflows/         build.yml (PR/push to master), release.yml (tag-driven)
```

## domain model + non-obvious behaviour

- **service image string**: build as `repo:tag@digest` (not `repo@digest`) so the tag stays visible in the ui even when digest-pinned. both `redeploy-service` in `api.clj` and `->service-image` in `mapper/outbound.clj` must agree.
- **compose rendering** walks the service map through `utils/clean` which recursively removes nil / empty collections. if a value becomes empty after cleaning, `->yaml` emits `{}\n`. guard against that when adding a new renderer branch.
- **editing stacks** goes through `docker stack deploy` cli (via `docker/engine/cli.clj`), not the service api. when a stored `stackfile` exists in couch, it's the source of truth; otherwise compose is reconstructed from the live docker state.
- **mounts**: `bind` (has host path), `volume` (host is volume name; may be named or anonymous), `tmpfs` (no host, target only). request specs treat `:host` as optional on mount payloads.
- **ports**: short syntax `"host:container"` when `mode=ingress` and `protocol=tcp`; long syntax (target/published/protocol/mode) otherwise.
- **influxdb**: 1.x only. boot wraps init in try/catch, runtime writes wrap in try/catch → a broken or v2 influx degrades silently to "no stats", swarmpit still runs. reference compose uses `influxdb:1.8`.
- **docker api version**: default `1.44`, negotiated at boot via `/_ping` clamped to our max. `SWARMPIT_DOCKER_API` overrides.
- **entrypoint vs command naming confusion**: compose `entrypoint` ↔ docker `ContainerSpec.Command`, compose `command` ↔ docker `ContainerSpec.Args`. remember whenever you touch the mappers.
- **containerLabels vs labels** are separate: `containerLabels` → `ContainerSpec.Labels` (per-container), `labels` → top-level `Spec.Labels` (per-service). both round-trip.
- **swarmpit-specific docker labels** (on services swarmpit deploys/manages):
  - `swarmpit.service.deployment.autoredeploy` — bool, drives auto-redeploy on new digest
  - `swarmpit.service.immutable` — bool, disables edits in ui
  - `swarmpit.agent` — bool, service is a swarmpit agent
  - `swarmpit.service.link.<name>` — user-defined link, key/value stored as label
- **memoization**: `clojure.core.memoize/ttl` used to coalesce request bursts. inherited caches:
  - `tasks-memo`, `services-memo`, `admin-exists?` — 1s ttl
  - `df-memo` — 10s ttl
  - `broadcast-memo` (events) — 1s ttl
  if a change "doesn't show up", it's often a cache — wait up to the ttl or restart.
- **couchdb migrations**: map in `couchdb/migration.clj` under `migrations`. ran once, tracked in db; to add one, register a new keyword + fn. never mutate existing migrations in place.

## frontend conventions

- state lives in one atom (`swarmpit.component.state/state`); access via cursors (`docker-api-cursor`, `docker-engine-cursor`, `instance-name-cursor`, `form-value-cursor`, `form-state-cursor`, `layout-cursor`). react via `state/react` inside `rum/defc`.
- on page load `version-handler` (`component/layout.cljs`) fetches `/api/version` and `(state/set-value response)` merges the response at the root — so `:docker`, `:initialized`, `:instanceName` sit at top level and are readable via single-key cursors.
- **auth**: `POST /login` with `Authorization: Basic <base64>` returns `{:token <jwt>}`. frontend persists in `localStorage` via `swarmpit.storage`; `swarmpit.ajax` attaches `Authorization: <raw-jwt>` on every request (no `Bearer` prefix — intentional, matches backend parser).
- **SSE**: `/events` uses http-kit `with-channel`, returns `text/event-stream`. client (`event/source.cljs`) uses `js/EventSource`; reconnects every 5s on error. SSE auth uses a short-lived token (`/slt`), not the JWT, because EventSource can't set headers.
- material-ui v4 via `@material-ui/core`. popper modifiers follow popper.js v1 shape (`{:flip {:enabled false}}`, not v2 array form).
- css naming: `.Swarmpit-<area>-<element>`. dark-mode variants live further down the same file under `html.dark .Swarmpit-...`.
- sablono `[:<>]` fragments inside `rum/defc` are unreliable — wrap in a `[:div]`.
- `rum/defc` does not support multi-arity; use default args via a plain fn wrapper.

## backend conventions

- middleware stack (`server.clj`, in order): muuntaja format → authentication → authorization → swagger → parameters → exception → **request-coerce ON, response-coerce OFF**. so responses don't get schema-stripped; request bodies do.
- authorization rules (`authorization.clj`): regex + method, `buddy.auth.accessrules`. baseline is `^/api/.*` → authenticated, with narrower rules for admin endpoints, per-registry ownership, etc.
- the default exception handler switches on `:type` in ex-data (`:http-client`, `:aws-client`, `:docker-cli`, `:api`) to produce useful bodies; unmatched exceptions become `{:status 500 :body (Throwable->map e)}`.
- **reitit specs don't hot-reload**: edits to `routes_spec.cljc` require a full jvm restart — the router is compiled at startup. repl `fig-stop` + `fig-start` rebinds it.
- service logs: agent path preferred (`service-agent-logs`), falls back to native docker `/logs` (`service-native-logs`). detected via `swarmpit.agent` task label.

## env vars

see `doc/configuration.md`. commonly touched:

- `SWARMPIT_DOCKER_API` — override negotiated api version (otherwise auto-negotiated via `/_ping`)
- `SWARMPIT_DOCKER_SOCK` — `/var/run/docker.sock` or `http://host:2375`
- `SWARMPIT_DOCKER_HTTP_TIMEOUT` — ms, default 5000
- `SWARMPIT_DB` — couchdb url
- `SWARMPIT_INFLUXDB` — stats url; nil disables stats (v2 incompatible — use 1.x)
- `SWARMPIT_AGENT_URL` — static agent address, otherwise discovered dynamically; dev-only
- `SWARMPIT_WORK_DIR` — temp dir, default `/tmp`
- `SWARMPIT_INSTANCE_NAME` — custom sidebar/header/tab title (replaces swarmpit wordmark)

## ci / release

- `.github/workflows/build.yml`: on push/PR to master. PRs from the same repo push `swarmpit/swarmpit:pr-<n>` to docker hub; master pushes `:latest`. PRs from forks build-only (no secrets). multi-arch build (amd64/arm64/armv7/armv5).
- `.github/workflows/release.yml`: triggered by version tag (`1.11`, `1.11.0`). builds, pushes `swarmpit/swarmpit:<tag>`, creates github release with jar.
- uberjar is the ci artifact; docker stage only copies it and sets the entrypoint.
