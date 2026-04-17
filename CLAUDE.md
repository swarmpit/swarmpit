# swarmpit

lightweight docker swarm management ui. clojure backend + clojurescript (rum/react) frontend, couchdb for app data, influxdb 1.x (optional) for stats. deployed via `docker stack deploy`.

## build / test

- `lein check` — validate compilation
- `lein test` — run clj tests (some marked `^:integration` need a live dind)
- `lein uberjar` — production jar
- frontend hot-reloads via shadow-cljs / figwheel during dev
- full stack runs via `docker-compose.yml` (app + agent + db + influxdb:1.8)

## source layout

```
src/clj/swarmpit/          backend
  api.clj                  top-level api functions (services, stacks, registries, …)
  handler.clj              ring handlers, resp-ok / resp-error helpers
  server.clj               ring middleware; request-coercion ON, response-coercion OFF
  database.clj             init couch + influx at boot (influx wrapped in try/catch)
  setup.clj                docker api version negotiation via /_ping
  config.clj               config atoms: @default ← environment ← @dynamic (later wins)
  version.clj              /api/version response body
  authorization.clj        access rules (buddy-auth)
  stats.clj                stats collector; influx writes wrapped in try/catch
  docker/engine/
    http.clj               docker http client (unix socket via per-request conn-mgr)
    client.clj             docker api methods; ping is :unversioned?
    mapper/inbound.clj     docker spec → domain
    mapper/outbound.clj    domain → docker spec
    mapper/compose.clj     domain → compose yaml
  couchdb/client.clj
  influxdb/client.clj      influxdb 1.x only (v2 not supported)
src/cljs/swarmpit/         frontend (rum components)
  component/state.cljs     single app atom + cursors
  component/layout.cljs    document-title, page routing
  component/menu.cljs      drawer / sidebar
  component/header.cljs    appbar
  component/common.cljs    title-logo, title-version, shared helpers
  component/service/       service views (edit, info, form-*)
  component/stack/         stack create / compose edit
src/cljc/swarmpit/         shared
  routes.cljc              reitit routes (backend + frontend)
  routes_spec.cljc         request/response schemas (spec-tools)
  yaml.cljc                yaml encode/decode (snakeyaml on jvm)
  utils.cljc               clean, clean-nils, map↔name-value
test/clj/swarmpit/
resources/public/          css, fonts, img/{logo,icon}.svg
doc/                       env-var docs (configuration.md), user config docs
```

## domain model

- **service image** is built as `repo:tag@digest` (not `repo@digest`) so the tag stays visible in the ui even when digest-pinned. both `redeploy-service` in `api.clj` and `->service-image` in `mapper/outbound.clj` must agree.
- **compose rendering** walks the service map through `utils/clean` which recursively removes nil / empty collections. if a value becomes empty after cleaning, `->yaml` emits `{}\n` — prefer guarding against this when you add a new renderer.
- **editing stacks** goes through `docker stack deploy` cli (via `docker/engine/cli.clj`), not the service api. stored `stackfile` in couch is the source of truth when present; otherwise compose is reconstructed from the running docker state.
- **mounts**: three types — `bind` (has host path), `volume` (has volume name as host; may be named or anonymous), `tmpfs` (no host, target only). request specs treat `:host` as optional on mounts.
- **ports**: short syntax `"host:container"` when `mode=ingress` and `protocol=tcp`; long syntax (target/published/protocol/mode) otherwise.
- **influxdb**: only v1.x supported. boot wraps init in try/catch, runtime writes wrap in try/catch → a broken/missing influx degrades silently to "no stats", swarmpit still runs.
- **docker api version**: default `1.44`, negotiated at boot via `/_ping` clamped to our max. override with `SWARMPIT_DOCKER_API`.

## frontend conventions

- state lives in one atom (`swarmpit.component.state/state`); access via cursors (`docker-api-cursor`, `docker-engine-cursor`, `instance-name-cursor`, `form-value-cursor`, `form-state-cursor`, `layout-cursor`).
- on page load `version-handler` fetches `/api/version` and `(state/set-value response)` merges it at the root — so `:docker`, `:initialized`, `:instanceName`, etc. sit at top level.
- material-ui v4 via `@material-ui/core`. popper modifiers follow popper.js v1 shape (`{:flip {:enabled false}}`).
- sablono `[:<>]` fragments inside `rum/defc` are unreliable — wrap in a `[:div]`.
- `rum/defc` does not support multi-arity; use default args via a plain fn wrapper.
- all user-visible icons come from `material.icon` (font-awesome via svg paths under `material.icon`); images from `img/*.svg`.

## gotchas

- **reitit specs don't hot-reload** — edits to `routes_spec.cljc` require a full jvm restart (the router is compiled at startup).
- **`lein check` is the quick sanity gate** — run it after any clj edit.
- **docker service spec confusingly swaps names**: compose `entrypoint` ↔ docker `ContainerSpec.Command`, compose `command` ↔ docker `ContainerSpec.Args`. remember this whenever you touch the mappers.
- **container labels vs service labels** are separate: `containerLabels` → `ContainerSpec.Labels`, `labels` → top-level service `Labels`.
- **the edit service form sends only a subset of keys** via `general-keys` in `service/edit.cljs:41`. new fields that must round-trip through the form need to be added there.

## env vars

see `doc/configuration.md`. commonly touched:

- `SWARMPIT_DOCKER_API` — override negotiated api version
- `SWARMPIT_DOCKER_SOCK` — `/var/run/docker.sock` or `http://host:2375`
- `SWARMPIT_INFLUXDB` — stats url; nil disables stats
- `SWARMPIT_DB` — couchdb url
- `SWARMPIT_INSTANCE_NAME` — custom sidebar/header/tab title (replaces swarmpit wordmark)
