import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { apiKey } from "@/lib/queries";
import type { Node } from "@/lib/types";
import { NodesList } from "./lists";

const live = vi.hoisted(() => ({ snapshot: null as null | ((data: unknown) => void) }));
vi.mock("@/lib/events", () => ({
  connectEvents: (_subscription: unknown, snapshot: (data: unknown) => void) => {
    live.snapshot = snapshot;
    return () => { live.snapshot = null; };
  },
}));
vi.mock("@/features/session/session", () => ({ useSession: () => ({ me: { role: "admin" } }) }));

const node = (nodeName: string, role: string, overrides: Partial<Node> = {}): Node => ({
  id: nodeName, nodeName, role, availability: "active", state: "ready", address: "10.0.0.2",
  engine: "27.0", os: "linux", leader: role === "manager",
  stats: {
    cpu: { cores: 4, usedPercentage: 25 },
    memory: { used: 1073741824, total: 2147483648, usedPercentage: 50 },
    disk: { used: 1000000000, total: 10000000000, usedPercentage: 10 },
  }, ...overrides,
} as Node);
function mount(rows?: Node[]) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } });
  if (rows) client.setQueryData(apiKey("/api/nodes"), rows);
  render(<QueryClientProvider client={client}><MemoryRouter><NodesList /></MemoryRouter></QueryClientProvider>);
  return client;
}
beforeEach(() => {
  live.snapshot = null;
  vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>(() => {})));
});
afterEach(() => vi.unstubAllGlobals());

it("restores node metrics on the full Nodes page and applies live node snapshots", async () => {
  mount([node("zulu", "worker"), node("alpha", "manager")]);
  expect(screen.getAllByRole("article").map((card) => card.getAttribute("aria-label"))).toEqual(["alpha", "zulu"]);
  expect(within(screen.getByRole("article", { name: "alpha" })).getAllByRole("application")).toHaveLength(3);
  expect(screen.getAllByText("1 vCPU / 4 vCPU")).toHaveLength(2);
  act(() => live.snapshot?.([node("new-worker", "worker", { state: "down" })]));
  expect(await screen.findByRole("link", { name: "new-worker" })).toHaveAttribute("href", "/nodes/new-worker");
  expect(screen.queryByRole("link", { name: "alpha" })).not.toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
  expect(screen.getByText(/Statistics unavailable/)).toBeInTheDocument();
});

it("combines text search and role filtering, and can restore all roles", async () => {
  mount([node("alpha", "manager"), node("beta", "worker"), node("gamma", "worker", { address: "192.0.2.8" })]);
  const role = screen.getByRole("combobox", { name: "Role" });
  fireEvent.click(role);
  fireEvent.click(await screen.findByRole("option", { name: "worker" }));
  expect(screen.queryByRole("link", { name: "alpha" })).not.toBeInTheDocument();
  expect(screen.getAllByRole("article")).toHaveLength(2);
  const search = screen.getByRole("textbox", { name: "Filter nodes" });
  fireEvent.change(search, { target: { value: "192.0.2.8" } });
  expect(screen.getAllByRole("article")).toHaveLength(1);
  expect(screen.getByRole("link", { name: "gamma" })).toBeInTheDocument();
  fireEvent.change(search, { target: { value: "alpha" } });
  expect(screen.getByText("No matching nodes.")).toBeInTheDocument();
  fireEvent.click(role);
  fireEvent.click(await screen.findByRole("option", { name: "Default" }));
  expect(screen.getByRole("link", { name: "alpha" })).toBeInTheDocument();
});

it("retains cached node cards and selected filters when a background refresh fails", async () => {
  const client = mount([node("alpha", "manager"), node("beta", "worker")]);
  fireEvent.change(screen.getByRole("textbox", { name: "Filter nodes" }), { target: { value: "alpha" } });
  act(() => client.getQueryCache().find({ queryKey: apiKey("/api/nodes") })?.setState({
    error: new Error("Agent disconnected"), status: "error",
  }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Agent disconnected");
  expect(screen.getByRole("link", { name: "alpha" })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "beta" })).not.toBeInTheDocument();
  expect(screen.getAllByRole("application")).toHaveLength(3);
  expect(screen.queryByRole("status", { name: "Nodes loading" })).not.toBeInTheDocument();
});

it("keeps page controls and the live indicator mounted during the initial node load", () => {
  mount();
  expect(screen.getByRole("heading", { name: "Nodes" })).toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: "Filter nodes" })).toBeInTheDocument();
  expect(screen.getByRole("combobox", { name: "Role" })).toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Live updates connecting" })).toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Nodes loading" })).toBeInTheDocument();
});
