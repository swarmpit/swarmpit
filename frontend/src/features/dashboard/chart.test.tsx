import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { expect, it } from "vitest";
import { ResourceUsageChart, TimeseriesChart } from "./chart";
import { bytes } from "@/lib/format";
const time = ["2026-11-05T10:00:00Z", "2026-11-05T10:01:00Z"];

it("announces real memory quantities and backend percentages when navigating a donut", async () => {
  render(<ResourceUsageChart title="Memory" used={1073741824} total={2147483648}
    usage={60} format={bytes} color="var(--chart-2)" />);
  expect(screen.getByText("1.0 GiB / 2.0 GiB")).toBeInTheDocument();
  expect(screen.getByText("60.0%")).toBeInTheDocument();
  const chart = screen.getByRole("application", { name: "Memory usage breakdown" });
  expect(chart).toHaveAttribute("tabindex", "0");
  fireEvent.focus(chart);
  const tooltip = await screen.findByRole("status");
  expect(tooltip).toHaveTextContent("Used");
  expect(tooltip).toHaveTextContent("1.0 GiB · 60.0%");
  fireEvent.keyDown(chart, { key: "ArrowRight" });
  await waitFor(() => expect(tooltip).toHaveTextContent("Free"));
  expect(tooltip).toHaveTextContent("1.0 GiB · 40.0%");
});

it.each([
  { used: undefined, total: undefined, usage: undefined },
  { used: 1, total: 0, usage: 25 },
  { used: 1, total: 2, usage: NaN },
  { used: 1, total: 2, usage: 101 },
])("does not invent donut usage for unavailable or invalid statistics: %j", (stats) => {
  render(<ResourceUsageChart title="CPU" {...stats} format={String} color="var(--chart-1)" />);
  expect(screen.getByText("Statistics unavailable.")).toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
});


it("keeps both metric units visible while loading and replaces skeletons with samples", () => {
  const { rerender } = render(
    <TimeseriesChart title="Usage" series={[]} pending cpuUnit="vcpu" memoryUnit="mib" />,
  );
  expect(screen.getByText("CPU (vCPU)")).toBeInTheDocument();
  expect(screen.getByText("Memory (MiB)")).toBeInTheDocument();
  expect(screen.getByRole("status", { name: "CPU (vCPU) loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.getByRole("status", { name: "Memory (MiB) loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.queryByText("Statistics are not available yet.")).not.toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();

  rerender(
    <TimeseriesChart title="Usage" series={[{ task: "worker", time, cpu: [0.2, 0.4], memory: [256, 512] }]}
      cpuUnit="vcpu" memoryUnit="mib" />,
  );
  expect(screen.queryByRole("status", { name: /loading/ })).not.toBeInTheDocument();
  expect(screen.getByRole("application", { name: /CPU \(vCPU\)/ })).toBeInTheDocument();
  expect(screen.getByRole("application", { name: /Memory \(MiB\)/ })).toBeInTheDocument();
});

it("provides a keyboard-accessible chart labelled with task memory units", () => {
  render(
    <TimeseriesChart
      title="Usage"
      series={[{ task: "task", time, memory: [256, 512] }]}
      memoryUnit="mib"
      metric="memory"
    />,
  );
  expect(screen.getByText("MiB")).toBeInTheDocument();
  expect(screen.queryByRole("heading", { name: "Memory (MiB)" })).not.toBeInTheDocument();
  expect(screen.getByRole("application", { name: /Memory \(MiB\)/ })).toHaveAttribute("tabindex", "0");
});
it("paints isolated samples on both sides of a missing-data gap", async () => {
  render(<TimeseriesChart title="CPU" metric="cpu" series={[{
    name: "node", time: [...time, "2026-11-05T10:02:00Z"], cpu: [10, null, 30],
  }]} />);
  const chart = screen.getByRole("application", { name: /CPU/ });
  expect(chart.querySelector(".recharts-area")).not.toBeNull();
  await waitFor(() => expect(chart.querySelectorAll("circle[r='3']")).toHaveLength(2));
});
it("announces tooltip values with units during keyboard navigation", async () => {
  render(<TimeseriesChart title="Memory" metric="memory" memoryUnit="mib"
    series={[{ task: "worker", time, memory: [256, 512] }]} />);
  const chart = screen.getByRole("application", { name: /Memory/ });
  fireEvent.focus(chart);
  fireEvent.keyDown(chart, { key: "ArrowRight" });
  fireEvent.keyDown(chart, { key: "ArrowRight" });
  const tooltip = await screen.findByRole("status");
  expect(tooltip).toHaveTextContent("worker");
  expect(tooltip).toHaveTextContent("512 MiB");
});
it("reports missing samples rather than inventing zero usage", () => {
  render(<TimeseriesChart title="Usage" series={[]} />);
  expect(screen.getByText("Statistics are not available yet.")).toBeInTheDocument();
});
it("labels task CPU in vCPU rather than percent", () => {
  render(
    <TimeseriesChart
      title="Task"
      series={[{ task: "one", time: [time[0]], cpu: [0.4] }]}
      metric="cpu"
      cpuUnit="vcpu"
    />,
  );
  expect(screen.getByText("vCPU")).toBeInTheDocument();
});
it("shows the empty state when the selected metric has no timed samples", () => {
  render(
    <TimeseriesChart title="CPU" metric="cpu" series={[{ name: "node", time, memory: [25, 30] }]} />,
  );
  expect(screen.getByText("Statistics are not available yet.")).toBeInTheDocument();
  expect(screen.queryByRole("application")).not.toBeInTheDocument();
});

it.each([
  [74.9, "var(--primary)", "Used"],
  [75, "var(--warning)", "High usage"],
  [90, "var(--warning)", "High usage"],
  [90.1, "var(--destructive)", "Critical usage"],
])("shows semantic severity at %s percent", (usage, color, status) => {
  const { container } = render(<ResourceUsageChart title="CPU" used={1} total={2} usage={usage as number} format={String} />);
  expect(screen.getByText(status as string)).toBeInTheDocument();
  expect(container.querySelector("style")?.textContent).toContain("--color-used: " + color);
  expect(container.querySelector("style")?.textContent).toContain("--color-free: var(--border)");
  expect(container.querySelector(".recharts-legend-wrapper")).toBeNull();
});

const day = Array.from({ length: 11 }, (_, hour) => "2026-11-05T" + String(hour).padStart(2, "0") + ":00:00Z");
const samples = [{ name: "worker", time: day, cpu: day.map((_, index) => index) }];

const rangeName = (start: number, end: number) => "CPU (%): resource samples from " + new Date(day[start]).toLocaleString() + " to " + new Date(day[end]).toLocaleString();

it("defaults to the last hour, pans, selects longer history, and resets the view", async () => {
  render(<TimeseriesChart title="CPU" metric="cpu" series={samples} />);
  const select = screen.getByRole("combobox", { name: "CPU (%) time range" });
  expect(select).toHaveTextContent("Last hour");
  const earlier = screen.getByRole("button", { name: "CPU (%) pan earlier" });
  const later = screen.getByRole("button", { name: "CPU (%) pan later" });
  const chart = screen.getByRole("application");
  expect(chart).toHaveAccessibleName(rangeName(9, 10));
  expect(earlier).toBeEnabled();
  expect(later).toBeDisabled();
  fireEvent.click(earlier);
  expect(chart).toHaveAccessibleName(rangeName(8, 9));
  expect(select).toHaveTextContent("Custom range");
  expect(later).toBeEnabled();
  fireEvent.click(screen.getByRole("button", { name: "CPU (%) reset view" }));
  expect(chart).toHaveAccessibleName(rangeName(9, 10));

  fireEvent.click(select);
  fireEvent.click(await screen.findByRole("option", { name: "Last 6 hours" }));
  expect(select).toHaveTextContent("Last 6 hours");
  expect(chart).toHaveAccessibleName(rangeName(4, 10));
  fireEvent.click(select);
  fireEvent.click(await screen.findByRole("option", { name: "Last 24 hours" }));
  expect(chart).toHaveAccessibleName(rangeName(0, 10));
  expect(earlier).toBeDisabled();
  fireEvent.click(screen.getByRole("button", { name: "CPU (%) reset view" }));
  expect(select).toHaveTextContent("Last hour");
});

it("zooms to isolated samples, restores wider history, and preserves the selected window on refresh", async () => {
  const { rerender } = render(<TimeseriesChart title="CPU" metric="cpu" series={samples} />);
  const chart = screen.getByRole("application");
  const zoomIn = screen.getByRole("button", { name: "CPU (%) zoom in" });
  fireEvent.click(zoomIn);
  expect(chart).toHaveAccessibleName(rangeName(10, 10));
  expect(zoomIn).toBeDisabled();
  await waitFor(() => expect(chart.querySelectorAll("circle[r='3']")).toHaveLength(1));
  fireEvent.click(screen.getByRole("button", { name: "CPU (%) zoom out" }));
  expect(chart).toHaveAccessibleName(rangeName(8, 10));
  rerender(<TimeseriesChart title="CPU" metric="cpu" series={[{ ...samples[0], cpu: day.map((_, index) => index + 1) }]} />);
  expect(chart).toHaveAccessibleName(rangeName(8, 10));
  fireEvent.focus(chart);
  expect(await screen.findByRole("status")).toHaveTextContent("9 %");
});

it("toggles individual series with accessible legend buttons and restores them", async () => {
  render(<TimeseriesChart title="CPU" metric="cpu" series={[
    ...samples, { name: "manager", time: day, cpu: day.map(() => 4) },
  ]} />);
  const worker = await screen.findByRole("button", { name: "worker" });
  const chart = screen.getByRole("application", { name: /CPU/ });
  expect(worker).toHaveAttribute("aria-pressed", "true");
  expect(chart.querySelectorAll(".recharts-area")).toHaveLength(2);
  fireEvent.click(worker);
  expect(worker).toHaveAttribute("aria-pressed", "false");
  await waitFor(() => expect(chart.querySelectorAll(".recharts-area")).toHaveLength(1));
  expect(screen.getByRole("button", { name: "manager" })).toHaveAttribute("aria-pressed", "true");
  fireEvent.click(worker);
  await waitFor(() => expect(chart.querySelectorAll(".recharts-area")).toHaveLength(2));
});

it("keeps a hidden resource hidden when live snapshots reorder the series", async () => {
  const manager = { name: "manager", time: day, cpu: day.map(() => 4) };
  const { rerender } = render(<TimeseriesChart title="CPU" metric="cpu" series={[samples[0], manager]} />);
  fireEvent.click(await screen.findByRole("button", { name: "worker" }));
  rerender(<TimeseriesChart title="CPU" metric="cpu" series={[manager, samples[0]]} />);
  expect(await screen.findByRole("button", { name: "worker" })).toHaveAttribute("aria-pressed", "false");
  expect(screen.getByRole("button", { name: "manager" })).toHaveAttribute("aria-pressed", "true");
});

it("reports an export error when the rendered chart has no visible dimensions", async () => {
  render(<TimeseriesChart title="CPU" metric="cpu" series={samples} />);
  fireEvent.click(screen.getByRole("button", { name: "CPU (%) export PNG" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Image export failed");
});
