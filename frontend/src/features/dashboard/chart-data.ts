import type { ChartConfig } from "@/components/ui/chart";
import type { Timeseries } from "@/lib/types";

export type ChartRow = { time: number; [key: string]: number | null };

export function buildChartData(series: Timeseries[], metric: "cpu" | "memory") {
  const config: ChartConfig = {};
  const rows = new Map<number, ChartRow>();

  series.forEach((resource, index) => {
    if (!Array.isArray(resource?.time)) return;
    const points = resource.time.flatMap((time, sample) => {
      const timestamp = Date.parse(time);
      if (!Number.isFinite(timestamp)) return [];
      const value = resource[metric]?.[sample];
      return [{
        time: timestamp,
        value: typeof value === "number" && Number.isFinite(value) ? value : null,
      }];
    });
    if (!points.some((point) => point.value !== null)) return;

    // Synthetic keys keep duplicate names separate and resource names out of generated CSS.
    const key = "series" + index;
    config[key] = {
      label: resource.name || resource.service || resource.task || "Resource",
      color: `var(--chart-${(index % 5) + 1})`,
    };
    for (const point of points) {
      const row = rows.get(point.time) || { time: point.time };
      row[key] = point.value;
      rows.set(point.time, row);
    }
  });

  const data = [...rows.values()].sort((a, b) => a.time - b.time);
  for (const row of data) {
    for (const key of Object.keys(config)) row[key] ??= null;
  }
  return { data, config };
}
