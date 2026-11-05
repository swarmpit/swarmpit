# Swarmpit React fork — instructions for future agents

## Scope and architecture

Read this file before changing the repository. Read [docs/UPSTREAM_UPDATES.md](docs/UPSTREAM_UPDATES.md) before any upstream sync, and [docs/UI_PARITY.md](docs/UI_PARITY.md) before adding/removing UI behavior. Approved design: [docs/superpowers/specs/2026-11-05-react-ui-design.md](docs/superpowers/specs/2026-11-05-react-ui-design.md).

This is a public fork of https://github.com/swarmpit/swarmpit. The product must retain Swarmpit's functionality and backend; only its UI is being rebuilt with React, strict TypeScript, Tailwind CSS **4**, current shadcn/ui and TanStack Query. React code lives in frontend/. Legacy code remains in src/cljs/ and is the behavior reference until each parity item is verified. Do not report a static build or a dashboard as full feature parity.

## Non-negotiable invariants

- Keep backend authorization, request validation, error handling, database migrations, Docker/agent behavior and optional InfluxDB integration intact. Never move authorization into the client.
- Trace old page → request/spec → handler → mapper before changing a flow. Source code wins over stale comments in CLAUDE.md.
- POST /login returns a complete Bearer authorization value. Forward it unchanged. Version/bootstrap is GET /version, not /api/version. Retain first-admin initialization and real logout/revocation.
- SSE /events consumes a fresh /slt token on every connection. subscription is base64 EDN with legacy handler/params, not JSON. Close old streams and retries on navigation/logout.
- Preserve separate command/entrypoint and labels/containerLabels, image tags and digests, optional mount host for tmpfs/anonymous volumes, stored stack compose/history, role/owner restrictions and immutable services.
- Forms must preserve dirty values across query refreshes. Never auto-retry destructive mutations. Confirm destructive actions and show backend failures.
- Preserve the original license and attribution. No production data/destructive Swarm testing.

## Upstream updates

Follow docs/UPSTREAM_UPDATES.md in order. Use normal merges preserving upstream history. Never reset the custom branch to upstream, force-push, blindly accept one side of conflicts, overwrite frontend/, or merge the unrelated bootstrap main branch.

For every update: record old/new upstream SHA; inspect changed route/spec, auth, event, handler and mapper contracts; port changed/new UI behavior into React; update the parity checklist and baseline; run the required checks; review the complete merge diff. Receiving upstream commits does not port upstream UI changes automatically. Do not auto-merge or auto-deploy updates.

## Tools and verification

Use the configured filesystem MCP for reads/writes when available, read_multiple_files for independent reads, and edit_file with dryRun before applying edits. Use rg for content searches. Use Context7 resolve-library-id then query-docs for current library/API/CLI documentation. Use installed dependencies and existing helpers; keep changes at the responsible layer.

Frontend checks: npm --prefix frontend ci; npm --prefix frontend test; npm --prefix frontend run typecheck; npm --prefix frontend run build. Backend changes: lein check and lein test; Docker/mapper changes additionally require the documented disposable integration suite. Packaging changes require building Dockerfile.react and verifying /react/index.html plus streamed /events against a disposable Swarm. Missing runtimes/credentials are unverified checks, never passing checks.

Never stage, commit, push, publish, merge PRs or alter repository settings unless the user authorizes it. Use Conventional Commits when authorized. Do not expose credentials in commands, logs or documents.
