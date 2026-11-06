Merge the upstream Swarmpit commits into the React fork while preserving both histories. Frontend, backend and image checks run in the upstream-sync workflow before this PR is opened, because PRs created with GITHUB_TOKEN do not trigger ordinary pull_request workflows.

Review instructions: [docs/UPSTREAM_UPDATES.md](../blob/react-ui/docs/UPSTREAM_UPDATES.md).

- [ ] Inspect API routes/specs, authentication/authorization, SSE subscriptions, handlers and mappers.
- [ ] Port any changed/new upstream UI behavior into React; do not assume a clean Git merge means compatibility.
- [ ] Update docs/UI_PARITY.md and docs/upstream-baseline.json only after compatibility verification.
- [ ] Verify Docker Swarm operations in a disposable environment; review database/agent changes and migration safety.
- [ ] Review the complete diff and merge manually when checks pass. Do not auto-deploy.
