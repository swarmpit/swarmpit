import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { act, render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiKey } from "@/lib/queries";
import { Dashboard } from "./dashboard";

const session = vi.hoisted(() => ({ statistics: true }));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "operator", role: "user", "node-dashboard": ["n1"], "service-dashboard": ["s1"] },
    version: { statistics: session.statistics },
  }),
}));
vi.mock("@/lib/events", () => ({ connectEvents: () => () => {} }));

let respond: Map<string, (data: unknown) => void>;
beforeEach(() => {
  session.statistics = true;
  respond = new Map();
  vi.stubGlobal("fetch", vi.fn((path: string) => new Promise<Response>((resolve) => {
    respond.set(path, (data) => resolve(new Response(JSON.stringify(data), { status: 200 })));
  })));
});
afterEach(() => vi.unstubAllGlobals());

function mount(nodesReady = true) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  if (nodesReady) client.setQueryData(apiKey("/api/nodes"), [{
    id: "n1", nodeName: "worker-1", role: "worker", availability: "active", state: "ready",
    address: "10.0.0.1", engine: "27",
  }]);
  render(<QueryClientProvider client={client}><MemoryRouter><Dashboard /></MemoryRouter></QueryClientProvider>);
  return client;
}
function nodeSummary() {
  return within(screen.getByRole("heading", { name: "Cluster" }).parentElement!);
}
function pinned(title: string) {
  return within(screen.getByRole("heading", { name: title }).closest("section")!);
}
async function answer(path: string, data: unknown) {
  await act(async () => respond.get(path)!(data));
}

it("shows ready pins and counts while other queries load, then reveals their real values", async () => {
  mount();
  expect(pinned("Pinned nodes").getByRole("link", { name: "worker-1" })).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "Nodes loading" })).not.toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Services loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.getByRole("status", { name: "CPU loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.getByRole("status", { name: "Memory loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.getByRole("status", { name: "Storage loading" })).toHaveAttribute("aria-busy", "true");
  expect(pinned("Pinned services").getByRole("status")).toHaveAttribute("aria-busy", "true");
  expect(screen.queryByText("Statistics are not available yet.")).not.toBeInTheDocument();

  await answer("/api/services", [{ id: "s1", serviceName: "web", repository: { image: "nginx:latest" }, mode: "replicated" }]);
  await screen.findByRole("link", { name: "web" });
  expect(pinned("Pinned services").getByRole("link", { name: "web" })).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "Services loading" })).not.toBeInTheDocument();
  expect(nodeSummary().getByRole("link", { name: "1 service" })).toHaveAttribute("href", "/services");
  expect(screen.getByRole("status", { name: "CPU loading" })).toBeInTheDocument();

  await answer("/api/stats", {
    cpu: { usage: 25, cores: 8 }, memory: { used: 1073741824, total: 2147483648, usage: 60 },
    disk: { used: 3221225472, total: 4294967296, usage: 75 },
  });
  expect(await screen.findByText("25.0%")).toBeInTheDocument();
  expect(screen.getByText("1.0 GiB / 2.0 GiB")).toBeInTheDocument();
  expect(screen.getByText("3.2 GB / 4.3 GB")).toBeInTheDocument();
  expect(screen.getByText("2 vCPU / 8 vCPU")).toBeInTheDocument();
  expect(nodeSummary().getByRole("link", { name: "1 node" })).toHaveAttribute("href", "/nodes");
  expect(nodeSummary().getByText("managers").closest('[data-slot="badge"]')).toHaveTextContent(/^0\s*managers$/);
  expect(nodeSummary().getByText("worker").closest('[data-slot="badge"]')).toHaveTextContent(/^1\s*worker$/);
  expect(screen.getByRole("application", { name: "CPU usage breakdown" })).toBeInTheDocument();
  expect(screen.getByRole("application", { name: "Memory usage breakdown" })).toBeInTheDocument();
  expect(screen.getByRole("application", { name: "Storage usage breakdown" })).toBeInTheDocument();
  expect(screen.getByText("60.0%")).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "Storage loading" })).not.toBeInTheDocument();
});

it("shows live role counts, including zero, without inventing counts while nodes load", async () => {
  const client = mount(false);
  const summary = nodeSummary();
  expect(summary.getByRole("status", { name: "Nodes loading" })).toHaveAttribute("aria-busy", "true");
  expect(summary.queryByText(/^(?:managers?|workers?)$/)).not.toBeInTheDocument();
  expect(summary.queryByRole("link", { name: /\d+ nodes?/ })).not.toBeInTheDocument();

  const manager = { id: "m1", nodeName: "manager-1", role: "manager", state: "ready", availability: "active" };
  await answer("/api/nodes", [manager]);
  expect(await summary.findByRole("link", { name: "1 node" })).toHaveAttribute("href", "/nodes");
  expect(summary.getByText("manager").closest('[data-slot="badge"]')).toHaveTextContent(/^1\s*manager$/);
  expect(summary.getByText("workers").closest('[data-slot="badge"]')).toHaveTextContent(/^0\s*workers$/);

  await act(async () => client.setQueryData(apiKey("/api/nodes"), [
    manager,
    { ...manager, id: "m2", nodeName: "manager-2" },
    { ...manager, id: "w1", nodeName: "worker-1", role: "worker" },
  ]));
  expect(await summary.findByRole("link", { name: "3 nodes" })).toBeInTheDocument();
  expect(summary.getByText("managers").closest('[data-slot="badge"]')).toHaveTextContent(/^2\s*managers$/);
  expect(summary.getByText("worker").closest('[data-slot="badge"]')).toHaveTextContent(/^1\s*worker$/);

  await act(async () => { void client.invalidateQueries({ queryKey: apiKey("/api/nodes") }); });
  expect(summary.queryByRole("status", { name: "Nodes loading" })).not.toBeInTheDocument();
  expect(summary.getByRole("link", { name: "3 nodes" })).toBeInTheDocument();
  expect(summary.getByText("managers").closest('[data-slot="badge"]')).toBeInTheDocument();
  expect(summary.getByText("worker").closest('[data-slot="badge"]')).toBeInTheDocument();

  await answer("/api/nodes", []);
  expect(await summary.findByRole("link", { name: "0 nodes" })).toBeInTheDocument();
  expect(summary.getByText("managers").closest('[data-slot="badge"]')).toHaveTextContent(/^0\s*managers$/);
  expect(summary.getByText("workers").closest('[data-slot="badge"]')).toHaveTextContent(/^0\s*workers$/);
});

it("loads enabled history charts independently and keeps samples during background refresh", async () => {
  const client = mount();
  expect(screen.getAllByRole("status", { name: /loading/ })).not.toHaveLength(0);
  await answer("/api/services/ts/cpu", [{ service: "web", time: ["2026-11-05T10:00:00Z"], cpu: [0.4] }]);
  expect(await screen.findByRole("application", { name: /CPU.*vCPU/ })).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "CPU (vCPU) loading" })).not.toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Memory (MiB) loading" })).toBeInTheDocument();

  await act(async () => { void client.invalidateQueries({ queryKey: apiKey("/api/services/ts/cpu") }); });
  expect(screen.getByRole("application", { name: /CPU.*vCPU/ })).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: "CPU (vCPU) loading" })).not.toBeInTheDocument();
});

it("omits unpinned resources and empty histories instead of showing repeated empty cards", async () => {
  const client = mount();
  await act(async () => client.setQueryData(apiKey("/api/nodes"), []));
  await answer("/api/services", []);
  expect(await nodeSummary().findByRole("link", { name: "0 services" })).toHaveAttribute("href", "/services");
  await answer("/api/nodes/ts", []);
  await answer("/api/services/ts/cpu", []);
  await answer("/api/services/ts/memory", []);
  expect(screen.queryByRole("heading", { name: "Pinned nodes" })).not.toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Pinned services" })).not.toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
  expect(screen.queryByText("Statistics are not available yet.")).not.toBeInTheDocument();
  expect(await screen.findByText(/Historical samples have not arrived/)).toBeInTheDocument();
});

it("does not show history skeletons for disabled statistics", () => {
  session.statistics = false;
  mount();
  expect(screen.getByText(/Historical statistics are disabled/)).toBeInTheDocument();
  expect(screen.queryByRole("status", { name: /\(.+\) loading/ })).not.toBeInTheDocument();
  expect(screen.queryByText("Statistics are not available yet.")).not.toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "CPU utilization by Node" })).not.toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Memory utilization by Node" })).not.toBeInTheDocument();
});
