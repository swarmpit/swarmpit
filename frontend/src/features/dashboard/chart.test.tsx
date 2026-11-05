import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { TimeseriesChart } from "./chart";
it("displays task memory in MiB without clipping it to a percentage", () => {
  render(
    <TimeseriesChart
      title="Usage"
      series={[{ task: "task", time: ["a", "b"], memory: [256, 512] }]}
      memoryUnit="mib"
      metric="memory"
    />,
  );
  expect(screen.getByText("Memory (MiB)")).toBeInTheDocument();
  expect(screen.getByRole("img", { name: /Memory/ })).toHaveTextContent("512");
});
it("reports missing samples rather than inventing zero usage", () => {
  render(<TimeseriesChart title="Usage" series={[]} />);
  expect(
    screen.getByText("Statistics are not available yet."),
  ).toBeInTheDocument();
});
it("labels task CPU in vCPU rather than percent", () => {
  render(
    <TimeseriesChart
      title="Task"
      series={[{ task: "one", time: ["a"], cpu: [0.4] }]}
      metric="cpu"
      cpuUnit="vcpu"
    />,
  );
  expect(screen.getByText("CPU (vCPU)")).toBeInTheDocument();
});
