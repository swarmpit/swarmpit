import { beforeEach, afterEach, expect, it, vi } from "vitest";
import { act, render, screen, fireEvent, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import type { Node, Task } from "@/lib/types";
import { NodeDetail, TaskDetail } from "./detail";
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "operator", role: "user" },
    version: { statistics: false },
  }),
}));
vi.mock("@/lib/events", () => ({ connectEvents: () => () => {} }));
const baseNode: Node = {
  id: "n1",
  version: 3,
  nodeName: "worker-1",
  role: "worker",
  availability: "active",
  state: "ready",
  labels: [],
  address: "10.0.0.1",
  engine: "27",
  arch: "amd64",
  os: "linux",
  leader: false,
  resources: { cpu: 4, memory: 2048 },
};
const baseTask: Task = {
  id: "t1",
  taskName: "app.1",
  serviceName: "app",
  nodeId: "n1",
  nodeName: "worker-1",
  state: "running",
  desiredState: "running",
  repository: { image: "alpine:latest" },
  logdriver: "json-file",
};
let node: Node;
let task: Task;
beforeEach(() => {
  node = {
    ...baseNode,
    plugins: { networks: ["overlay", "bridge"], volumes: ["local"] },
    stats: {
      cpu: { cores: 4, usedPercentage: 25 },
      memory: { used: 1073741824, total: 2147483648, usedPercentage: 50 },
      disk: { used: 3221225472, total: 4294967296, usedPercentage: 75 },
    },
  };
  task = {
    ...baseTask,
    stats: {
      cpu: 0.5,
      cpuLimit: 2,
      cpuPercentage: 25,
      memory: 134217728,
      memoryLimit: 268435456,
      memoryPercentage: 50,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string) => {
      if (path === "/api/nodes/n1")
        return new Response(JSON.stringify(node), { status: 200 });
      if (path === "/api/tasks/t1")
        return new Response(JSON.stringify(task), { status: 200 });
      if (path === "/api/services/app/logs?since=15m")
        return new Response(JSON.stringify([
          { task: "t1", line: "Selected task output" },
          { task: "t2", line: "Other task output" },
        ]), { status: 200 });
      throw new Error("Unexpected request: " + path);
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
function Location() {
  const location = useLocation();
  return <output aria-label="Current query">{location.search}</output>;
}
function mount(kind: "nodes" | "tasks", search = "") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter
        initialEntries={[
          "/" + kind + "/" + (kind === "nodes" ? "n1" : "t1") + search,
        ]}
      >
        <Location />
        <Routes>
          <Route path="/nodes/:id" element={<NodeDetail />} />
          <Route path="/tasks/:id" element={<TaskDetail />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
function currentUsage() {
  return within(
    screen.getByRole("heading", { name: "Current usage" }).closest("section")!,
  );
}
it("shows node current CPU, memory and disk units independently of historical metrics", async () => {
  mount("nodes");
  await screen.findByRole("heading", { name: "worker-1" });
  const usage = currentUsage();
  expect(usage.getByRole("application", { name: "worker-1 CPU usage breakdown" })).toBeInTheDocument();
  expect(usage.getByRole("application", { name: "worker-1 Memory usage breakdown" })).toBeInTheDocument();
  expect(usage.getByRole("application", { name: "worker-1 Storage usage breakdown" })).toBeInTheDocument();
  expect(usage.getByText("1 vCPU / 4 vCPU")).toBeInTheDocument();
  expect(usage.getByText("1.0 GiB / 2.0 GiB")).toBeInTheDocument();
  expect(usage.getByText("3.2 GB / 4.3 GB")).toBeInTheDocument();
  expect(usage.getByText("25.0%")).toBeInTheDocument();
  expect(usage.getByText("50.0%")).toBeInTheDocument();
  expect(usage.getByText("75.0%")).toBeInTheDocument();
  expect(screen.getByText("overlay, bridge")).toBeInTheDocument();
  expect(screen.getByText("local")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("keeps MiB node capacity correct when current metrics and plugins are absent", async () => {
  node = { ...baseNode, stats: {} };
  mount("nodes");
  await screen.findByRole("heading", { name: "worker-1" });
  expect(screen.getByText("2.0 GiB")).toBeInTheDocument();
  expect(
    screen.getByText("Current resource statistics are not available."),
  ).toBeInTheDocument();
  expect(screen.queryByText("CPU usage")).not.toBeInTheDocument();
  expect(screen.getAllByText("None reported")).toHaveLength(2);
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("hides stale current usage for a down node", async () => {
  node = { ...node, state: "down" };
  mount("nodes");
  await screen.findByRole("heading", { name: "worker-1" });
  expect(currentUsage().queryByRole("application")).not.toBeInTheDocument();
  expect(
    screen.getByText("Current resource statistics are not available."),
  ).toBeInTheDocument();
});
it("shows running task vCPU and byte-based memory usage and limits", async () => {
  mount("tasks");
  await screen.findByRole("heading", { name: "app.1" });
  const usage = currentUsage();
  expect(usage.getByText("0.50 / 2.00 vCPU")).toBeInTheDocument();
  expect(usage.getByText("25.0%")).toBeInTheDocument();
  expect(usage.getByText("128.0 MiB")).toBeInTheDocument();
  expect(usage.getByText("256.0 MiB")).toBeInTheDocument();
  expect(usage.getByText("50.0%")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("reports unavailable running task metrics without inventing zero usage", async () => {
  task = { ...baseTask, stats: null };
  mount("tasks");
  await screen.findByRole("heading", { name: "app.1" });
  expect(
    screen.getByText("Current resource statistics are not available."),
  ).toBeInTheDocument();
  expect(screen.queryByText("CPU usage")).not.toBeInTheDocument();
});
it("omits stale task usage after the task stops running", async () => {
  task = { ...task, state: "failed" };
  mount("tasks");
  await screen.findByRole("heading", { name: "app.1" });
  expect(
    screen.queryByRole("heading", { name: "Current usage" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText("0.50 / 2.00 vCPU")).not.toBeInTheDocument();
});
it("opens legacy log=1 in a separate task Logs tab and restores Overview without losing other query values", async () => {
  mount("tasks", "?log=1&keep=yes");
  expect(await screen.findByText("Selected task output")).toBeInTheDocument();
  expect(screen.queryByText("Other task output")).not.toBeInTheDocument();
  expect(screen.getByRole("combobox", { name: "Task" })).toHaveTextContent("t1");
  expect(screen.getByRole("tab", { name: "Logs" })).toHaveAttribute("aria-selected", "true");
  expect(screen.queryByRole("heading", { name: "Task details" })).not.toBeInTheDocument();
  fireEvent.mouseDown(screen.getByRole("tab", { name: "Overview" }), { button: 0, ctrlKey: false });
  expect(await screen.findByRole("heading", { name: "Task details" })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "app" })).toHaveAttribute(
    "href",
    "/services/app",
  );
  expect(screen.getByRole("link", { name: "worker-1" })).toHaveAttribute(
    "href",
    "/nodes/n1",
  );
  expect(screen.queryByRole("heading", { name: "Service logs" })).not.toBeInTheDocument();
  expect(screen.getByLabelText("Current query")).toHaveTextContent("?keep=yes");
  fireEvent.keyDown(screen.getByRole("tab", { name: "Logs" }), { key: "Enter" });
  expect(await screen.findByText("Selected task output")).toBeInTheDocument();
  expect(screen.getByLabelText("Current query")).toHaveTextContent("log=1");
});
it("disables the log control for drivers unsupported by the legacy log view", async () => {
  task = { ...baseTask, logdriver: "syslog" };
  mount("tasks", "?log=1");
  await screen.findByRole("heading", { name: "app.1" });
  expect(screen.getByRole("tab", { name: "Logs" })).toBeDisabled();
  expect(screen.getByRole("tab", { name: "Overview" })).toHaveAttribute("aria-selected", "true");
  expect(screen.queryByRole("heading", { name: "Service logs" })).not.toBeInTheDocument();
});
it("cancels a pending task log request when returning to Overview", async () => {
  let signal: AbortSignal | undefined;
  const originalFetch = fetch;
  vi.stubGlobal("fetch", vi.fn((path: string, init?: RequestInit) => {
    if (path.startsWith("/api/services/app/logs")) {
      signal = init?.signal || undefined;
      return new Promise<Response>((_, reject) => {
        signal?.addEventListener("abort", () => reject(new DOMException("Aborted", "AbortError")));
      });
    }
    return originalFetch(path, init);
  }));
  mount("tasks", "?log=1");
  await screen.findByRole("heading", { name: "Service logs" });
  expect(signal?.aborted).toBe(false);
  await act(async () => {
    fireEvent.keyDown(screen.getByRole("tab", { name: "Overview" }), { key: "Enter" });
  });
  expect(signal?.aborted).toBe(true);
});
