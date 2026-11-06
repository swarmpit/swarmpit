import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { expect, it } from "vitest";
import type { Node, Service } from "@/lib/types";
import { PinnedNodes, PinnedServices } from "./pinned-resources";

const node = (overrides: Partial<Node> = {}): Node => ({
  id: "n1", version: 1, nodeName: "worker", role: "worker", availability: "active",
  state: "ready", address: "10.0.0.2", engine: "27.0", arch: "amd64", os: "linux",
  leader: false, labels: [], resources: { cpu: 8, memory: 4096 },
  stats: {
    cpu: { cores: 8, usedPercentage: 25 },
    memory: { used: 1073741824, total: 4294967296, usedPercentage: 30 },
    disk: { used: 1000000000, total: 10000000000, usedPercentage: 12 },
  }, ...overrides,
});
const service = (overrides: Partial<Service> = {}): Service => ({
  id: "s1", serviceName: "web", repository: { name: "nginx", tag: "stable", image: "nginx:stable", imageDigest: "sha256:abc" },
  mode: "replicated", state: "running", ports: [{ containerPort: 80, hostPort: 8080, protocol: "tcp", mode: "ingress" }],
  status: { tasks: { running: 2, total: 3 } }, ...overrides,
} as Service);
const mount = (children: React.ReactNode) => render(<MemoryRouter>{children}</MemoryRouter>);

it("restores each pinned node's real capacities and backend percentages without converting bytes to MiB again", () => {
  mount(<PinnedNodes rows={[node({ role: "manager", leader: true })]} />);
  const card = within(screen.getByRole("article", { name: "worker" }));
  expect(card.getByRole("link", { name: "worker" })).toHaveAttribute("href", "/nodes/n1");
  expect(card.getByText("linux")).toBeInTheDocument();
  expect(card.getByText("10.0.0.2")).toBeInTheDocument();
  expect(card.getByText("Docker 27.0")).toBeInTheDocument();
  expect(card.getByText("manager")).toBeInTheDocument();
  expect(card.getByText("leader")).toBeInTheDocument();
  expect(card.getByText("2 vCPU / 8 vCPU")).toBeInTheDocument();
  expect(card.getByText("1.0 GiB / 4.0 GiB")).toBeInTheDocument();
  expect(card.getByText("1.0 GB / 10.0 GB")).toBeInTheDocument();
  expect(card.getByText("30.0%")).toBeInTheDocument();
  expect(card.getByText("12.0%")).toBeInTheDocument();
  expect(card.getAllByRole("application")).toHaveLength(3);
});

it.each(["down", "unknown"])("does not present stale metrics for a node that is %s", (state) => {
  mount(<PinnedNodes rows={[node({ state })]} />);
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
  expect(screen.queryByText("2 vCPU / 8 vCPU")).not.toBeInTheDocument();
  expect(screen.getByText(/Statistics unavailable/)).toBeInTheDocument();
});

it("keeps ready nodes with missing samples visible without inventing zero usage", () => {
  mount(<PinnedNodes rows={[node({ stats: null })]} />);
  expect(screen.getByRole("link", { name: "worker" })).toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
  expect(screen.queryByText(/0 vCPU/)).not.toBeInTheDocument();
});

it("sorts copied pin lists by name and omits empty pin sections", () => {
  const rows = [node({ id: "z", nodeName: "zulu" }), node({ id: "a", nodeName: "alpha" })];
  const { rerender } = mount(<PinnedNodes rows={rows} />);
  expect(within(screen.getByRole("region", { name: "Pinned nodes" })).getAllByRole("article").map((card) => card.getAttribute("aria-label"))).toEqual(["alpha", "zulu"]);
  expect(rows.map((row) => row.nodeName)).toEqual(["zulu", "alpha"]);
  rerender(<MemoryRouter><PinnedNodes rows={[]} /><PinnedServices rows={[]} /></MemoryRouter>);
  expect(screen.queryByRole("region")).not.toBeInTheDocument();
});

it("shows published port/protocol as text, full image identity and deployment state before running state", () => {
  mount(<PinnedServices rows={[
    service({ id: "z", serviceName: "zulu", mode: "global", status: { tasks: { running: 3, total: 3 }, update: "rollback_started" } }),
    service({ status: { tasks: { running: 2, total: 3 }, update: "updating" } }),
  ]} />);
  const cards = screen.getAllByRole("article");
  expect(cards.map((card) => card.getAttribute("aria-label"))).toEqual(["web", "zulu"]);
  const web = within(cards[0]);
  expect(web.getByRole("link", { name: "web" })).toHaveAttribute("href", "/services/web");
  expect(web.getByText("nginx:stable")).toBeInTheDocument();
  expect(web.getByText("sha256:abc")).toBeInTheDocument();
  expect(web.getByText("8080 [tcp]")).toBeInTheDocument();
  expect(web.getByText("updating")).toBeInTheDocument();
  expect(web.queryByText("running")).not.toBeInTheDocument();
  expect(web.getByText("2 / 3 tasks")).toBeInTheDocument();
  expect(within(cards[1]).getByText("rollback")).toBeInTheDocument();
  expect(within(cards[1]).getByText("global")).toBeInTheDocument();
  expect(screen.getAllByRole("link")).toHaveLength(2);
});
