import { expect, it } from "vitest";
import { buildChartData } from "./chart-data";
const first = "2026-11-05T10:00:00Z";
const second = "2026-11-05T10:01:00Z";
const third = "2026-11-05T10:02:00Z";

it("aligns unequal and out-of-order resource samples by their actual timestamps", () => {
  const chart = buildChartData([
    { name: "a", time: [third, first], cpu: [30, 10] },
    { name: "b", time: [second, third], cpu: [20, 40] },
  ], "cpu");
  expect(chart.data).toEqual([
    { time: Date.parse(first), series0: 10, series1: null },
    { time: Date.parse(second), series0: null, series1: 20 },
    { time: Date.parse(third), series0: 30, series1: 40 },
  ]);
});

it("keeps missing and nonfinite samples as gaps while preserving real zero", () => {
  const chart = buildChartData([{
    name: "node",
    time: [first, second, third, "2026-11-05T10:03:00Z", "2026-11-05T10:04:00Z"],
    cpu: [0, null, NaN, Infinity],
  }], "cpu");
  expect(chart.data.map((row) => row.series0)).toEqual([0, null, null, null, null]);
});

it("preserves backend vCPU and MiB units and keeps duplicate display names independent", () => {
  const series = [
    { task: "same", time: [first], cpu: [0.4], memory: [512] },
    { task: "same", time: [first], cpu: [0.8], memory: [256] },
  ];
  expect(buildChartData(series, "cpu").data).toEqual([{ time: Date.parse(first), series0: 0.4, series1: 0.8 }]);
  const chart = buildChartData(series, "memory");
  expect(chart.data).toEqual([{ time: Date.parse(first), series0: 512, series1: 256 }]);
  expect(Object.values(chart.config).map((entry) => entry.label)).toEqual(["same", "same"]);
});

it("omits invalid timestamps and metrics that have no valid timed sample", () => {
  expect(buildChartData([{ time: ["invalid", second], cpu: [5, 10] }], "cpu").data).toEqual([
    { time: Date.parse(second), series0: 10 },
  ]);
  expect(buildChartData([{ time: [first], memory: [25] }], "cpu").data).toEqual([]);
});
