import { render, screen, fireEvent, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { expect, it } from "vitest";
import type { Service, Task } from "@/lib/types";
import { ServicesTable, TasksTable } from "./tables";

const service = (overrides: Partial<Service> = {}): Service => ({
  id: "s1", serviceName: "web", repository: { name: "nginx", tag: "stable", image: "nginx:stable" },
  mode: "replicated", state: "running", stack: "site",
  ports: [
    { containerPort: 80, hostPort: 8080, protocol: "tcp", mode: "ingress" },
    { containerPort: 53, hostPort: 5353, protocol: "udp", mode: "host" },
  ],
  status: { tasks: { running: 2, total: 3 } }, ...overrides,
} as Service);
const mount = (rows: Service[]) => render(<MemoryRouter><ServicesTable rows={rows} /></MemoryRouter>);

const task = (overrides: Partial<Task> = {}): Task => ({
  id: "task-1", taskName: "web.1", serviceName: "web", nodeId: "node-1",
  nodeName: "manager", state: "running", desiredState: "running",
  updatedAt: "2026-11-06T00:30:00Z", ...overrides,
});

it("restores task usage and last update columns with State last, including refreshed metrics", () => {
  const current = task({
    stats: { cpuPercentage: 12.5, cpu: 0.25, memoryPercentage: 25, memory: 1048576 },
    status: { error: "Previous task failed" },
  });
  const view = render(<MemoryRouter><TasksTable rows={[current]} /></MemoryRouter>);
  expect(screen.getAllByRole("columnheader").map((cell) => cell.textContent)).toEqual([
    "Task", "Service", "Node", "CPU Usage", "Memory Usage", "Last update", "Desired state", "Error", "State",
  ]);
  const cells = within(screen.getAllByRole("row")[1]!).getAllByRole("cell");
  expect(cells[3]).toHaveTextContent("12.50%");
  expect(cells[3]).toHaveTextContent("0.25 vCPU");
  expect(cells[4]).toHaveTextContent("25.00%");
  expect(cells[4]).toHaveTextContent("1.0 MiB");
  expect(cells[5]).toHaveTextContent(new Date(current.updatedAt!).toLocaleString());
  expect(cells.at(-1)).toHaveTextContent("running");
  expect(screen.getByRole("link", { name: "web.1" })).toHaveAttribute("href", "/tasks/task-1");
  expect(screen.getByRole("link", { name: "web" })).toHaveAttribute("href", "/services/web");
  expect(screen.getByRole("link", { name: "manager" })).toHaveAttribute("href", "/nodes/node-1");
  expect(screen.getByText("Previous task failed")).toBeInTheDocument();

  view.rerender(<MemoryRouter><TasksTable rows={[{
    ...current, stats: { cpuPercentage: 20, cpu: 0.4, memoryPercentage: 50, memory: 2097152 },
  }]} /></MemoryRouter>);
  expect(screen.getByText("20.00%")).toBeInTheDocument();
  expect(screen.getByText("0.40 vCPU")).toBeInTheDocument();
  expect(screen.getByText("50.00%")).toBeInTheDocument();
  expect(screen.getByText("2.0 MiB")).toBeInTheDocument();
});

it("keeps zero task usage distinct from unavailable metrics and dates", () => {
  render(<MemoryRouter><TasksTable rows={[
    task({ stats: { cpu: 0, cpuPercentage: 0, memory: 0, memoryPercentage: 0 } }),
    task({ id: "task-2", taskName: "web.2", stats: null, updatedAt: undefined }),
    task({ id: "task-3", taskName: "web.3", stats: {} }),
  ]} /></MemoryRouter>);
  const rows = screen.getAllByRole("row").slice(1);
  const zero = within(rows[0]!).getAllByRole("cell");
  expect(zero[3]).toHaveTextContent("0.00%");
  expect(zero[3]).toHaveTextContent("0.00 vCPU");
  expect(zero[4]).toHaveTextContent("0.00%");
  expect(zero[4]).toHaveTextContent("0 B");
  for (const row of rows.slice(1)) {
    const cells = within(row).getAllByRole("cell");
    expect(cells[3]).toHaveTextContent("—");
    expect(cells[4]).toHaveTextContent("—");
    expect(row).not.toHaveTextContent("NaN");
    expect(row).not.toHaveTextContent("undefined");
  }
  expect(within(rows[1]!).getAllByRole("cell")[5]).toHaveTextContent("—");
});

it.each([
  [undefined, "running"],
  ["updating", "updating"],
  ["rollback_started", "rollback"],
  ["completed", "running"],
])("shows deployment state %s before task state", (update, expected) => {
  mount([service({ status: { tasks: { running: 2, total: 3 }, update } })]);
  const row = within(screen.getByRole("row", { name: /web/ }));
  expect(row.getByText(expected)).toBeInTheDocument();
  if (expected !== "running") expect(row.queryByText("running")).not.toBeInTheDocument();
  expect(row.getByRole("link", { name: "web" })).toHaveAttribute("href", "/services/s1");
  expect(row.getByText("2 / 3")).toBeInTheDocument();
});

it("retains service columns and shows every published port with its protocol", () => {
  mount([service(), service({ id: "s2", serviceName: "internal", ports: [] })]);
  expect(screen.getAllByRole("columnheader").map((cell) => cell.textContent)).toEqual([
    "Service", "Image", "Tasks", "Ports", "Mode", "Stack", "State",
  ]);
  const web = within(screen.getByRole("row", { name: /web/ }));
  expect(web.getByText("8080 [tcp]")).toBeInTheDocument();
  expect(web.getByText("5353 [udp]")).toBeInTheDocument();
  expect(web.queryByRole("link", { name: /8080/ })).not.toBeInTheDocument();
  expect(within(screen.getByRole("row", { name: /internal/ })).getByText("—")).toBeInTheDocument();
});

it.each(["updating", "rollback", "5353", "udp"])("filters on displayed service state or port %s", (filter) => {
  mount([
    service({ id: "s1", serviceName: "web", ports: [], status: { update: "updating" } }),
    service({ id: "s2", serviceName: "dns", status: { update: "rollback_started" } }),
    service({ id: "s3", serviceName: "internal", ports: [] }),
  ]);
  fireEvent.change(screen.getByRole("textbox", { name: "Filter resources" }), { target: { value: filter } });
  const expected = filter === "updating" ? "web" : "dns";
  expect(screen.getByRole("link", { name: expected })).toBeInTheDocument();
  expect(screen.getAllByRole("row")).toHaveLength(2);
});

it("sorts displayed service names naturally without changing row associations or cached source order", () => {
  const rows = ["Zulu", "api10", "Alpha", "aPi2"].map((serviceName, index) => service({
    id: "id-" + index,
    serviceName,
    repository: { name: "registry.local/" + serviceName, tag: "latest", image: "registry.local/" + serviceName },
    stack: "stack-" + serviceName,
    ports: [],
  }));
  const original = [...rows];
  Object.freeze(rows);
  mount(rows);
  const displayed = screen.getAllByRole("row").slice(1);
  expect(displayed.map((row) => within(row).getAllByRole("cell")[0]?.textContent)).toEqual([
    "Alpha", "aPi2", "api10", "Zulu",
  ]);
  for (const row of displayed) {
    const cells = within(row);
    const name = cells.getAllByRole("cell")[0]!.textContent!;
    const source = rows.find((item) => item.serviceName === name)!;
    expect(cells.getByRole("link", { name })).toHaveAttribute("href", "/services/" + source.id);
    expect(cells.getByText("registry.local/" + name)).toBeInTheDocument();
    expect(cells.getByRole("link", { name: "stack-" + name })).toHaveAttribute("href", "/stacks/stack-" + name);
  }
  fireEvent.change(screen.getByRole("textbox", { name: "Filter resources" }), { target: { value: "api" } });
  expect(screen.getAllByRole("row").slice(1).map((row) => within(row).getAllByRole("cell")[0]?.textContent)).toEqual(["aPi2", "api10"]);
  expect(rows).toEqual(original);
});
