import { expect, it } from "vitest";
import { QueryClient } from "@tanstack/react-query";
import { applySnapshot } from "./live";
import { apiKey } from "./queries";
it("publishes every stack resource collection to its related view cache", () => {
  const c = new QueryClient();
  c.setQueryData(apiKey("/api/stacks"), [
    { stackName: "app", state: "deployed" },
  ]);
  const data = {
    stackName: "app",
    services: [{ id: "s" }],
    tasks: [{ id: "t" }],
    networks: [{ id: "n" }],
    volumes: [],
    configs: [],
    secrets: [],
    stats: null,
  };
  applySnapshot(c, { handler: "stack-info", params: { name: "app" } }, data);
  for (const key of [
    "services",
    "tasks",
    "networks",
    "volumes",
    "configs",
    "secrets",
  ] as const)
    expect(c.getQueryData(apiKey("/api/stacks/app/" + key))).toEqual(data[key]);
  expect(c.getQueryData(apiKey("/api/stacks"))).toMatchObject([
    { services: [{ id: "s" }] },
  ]);
});
it("updates individual task detail entries from service task snapshots", () => {
  const c = new QueryClient();
  applySnapshot(c, { handler: "task-info", params: { serviceName: "app" } }, [
    { id: "t", state: "failed" },
  ]);
  expect(c.getQueryData(apiKey("/api/tasks/t"))).toMatchObject({
    state: "failed",
  });
});
it("ignores malformed list events rather than poisoning resource caches", () => {
  const c = new QueryClient();
  c.setQueryData(apiKey("/api/services"), []);
  applySnapshot(c, { handler: "service-list", params: {} }, { error: "bad" });
  expect(c.getQueryData(apiKey("/api/services"))).toEqual([]);
});
it("marks removed tasks absent from a fresh service snapshot", () => {
  const c = new QueryClient();
  c.setQueryData(apiKey("/api/tasks/gone"), {
    id: "gone",
    serviceName: "app",
    state: "running",
  });
  applySnapshot(
    c,
    { handler: "task-info", params: { serviceName: "app" } },
    [],
  );
  expect(c.getQueryData(apiKey("/api/tasks/gone"))).toMatchObject({
    state: "removed",
  });
});
it("updates canonical service task views when subscribed by name", () => {
  const c = new QueryClient();
  const tasks = [{ id: "t" }];
  applySnapshot(
    c,
    { handler: "service-info", params: { id: "app-web" } },
    {
      service: { id: "canonical", serviceName: "app-web" },
      tasks,
      networks: [],
    },
  );
  expect(c.getQueryData(apiKey("/api/services/canonical/tasks"))).toEqual(
    tasks,
  );
  expect(c.getQueryData(apiKey("/api/services/app-web/tasks"))).toEqual(tasks);
});
