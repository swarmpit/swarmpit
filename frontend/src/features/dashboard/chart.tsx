import { useId } from "react";
import type { Timeseries } from "@/lib/types";
import { Section, Empty } from "@/components/common";
const colors = ["#10b981", "#6366f1", "#f59e0b", "#06b6d4", "#f43f5e"];
function Plot({
  metric,
  unit,
  series,
}: {
  metric: "cpu" | "memory";
  unit: string;
  series: Timeseries[];
}) {
  const id = useId();
  const samples = series
    .flatMap((s) => s[metric] || [])
    .filter((v) => typeof v === "number" && Number.isFinite(v));
  const maximum = Math.max(unit === "%" ? 100 : 1, ...samples);
  const label = (metric === "cpu" ? "CPU" : "Memory") + " (" + unit + ")";
  return (
    <div className="space-y-3">
      <h3 className="text-sm font-medium">{label}</h3>
      <svg
        role="img"
        aria-labelledby={id}
        viewBox="0 0 640 200"
        className="h-48 w-full"
      >
        <title id={id}>{label}: latest resource samples</title>
        {[0, 0.25, 0.5, 0.75, 1].map((v) => (
          <g key={v}>
            <line
              x1="50"
              x2="630"
              y1={170 - v * 150}
              y2={170 - v * 150}
              stroke="currentColor"
              className="text-border"
            />
            <text x="0" y={174 - v * 150} fontSize="10" fill="currentColor">
              {Number((v * maximum).toFixed(1))}
            </text>
          </g>
        ))}
        {series.flatMap((s, i) => {
          const values = s[metric] || [];
          const paths: string[][] = [[]];
          values.forEach((v, k) => {
            if (typeof v !== "number" || !Number.isFinite(v)) {
              if (paths.at(-1)?.length) paths.push([]);
              return;
            }
            paths
              .at(-1)!
              .push(
                50 +
                  (k / Math.max(values.length - 1, 1)) * 580 +
                  "," +
                  (170 - (v / maximum) * 150),
              );
          });
          return paths
            .filter((p) => p.length)
            .map((points, j) => (
              <polyline
                key={i + "-" + j}
                fill="none"
                stroke={colors[i % colors.length]}
                strokeWidth="2"
                points={points.join(" ")}
              >
                <title>
                  {s.name || s.service || s.task || "Resource"} {metric}
                </title>
              </polyline>
            ));
        })}
      </svg>
    </div>
  );
}
export function TimeseriesChart({
  title,
  series,
  metric,
  memoryUnit = "percent",
  cpuUnit = "percent",
}: {
  title: string;
  series: Timeseries[];
  metric?: "cpu" | "memory";
  memoryUnit?: "percent" | "mib";
  cpuUnit?: "percent" | "vcpu";
}) {
  const usable = series.filter(
    (s) =>
      s?.time?.length &&
      (s.cpu?.some((v) => typeof v === "number" && Number.isFinite(v)) ||
        s.memory?.some((v) => typeof v === "number" && Number.isFinite(v))),
  );
  return (
    <Section title={title}>
      {!usable.length ? (
        <Empty>Statistics are not available yet.</Empty>
      ) : (
        <>
          <div className={metric ? "" : "grid gap-6 lg:grid-cols-2"}>
            {(!metric || metric === "cpu") && (
              <Plot
                metric="cpu"
                unit={cpuUnit === "vcpu" ? "vCPU" : "%"}
                series={usable}
              />
            )}
            {(!metric || metric === "memory") && (
              <Plot
                metric="memory"
                unit={memoryUnit === "mib" ? "MiB" : "%"}
                series={usable}
              />
            )}
          </div>
          <div className="flex flex-wrap gap-4 text-xs">
            {usable.map((s, i) => (
              <span key={i} style={{ color: colors[i % colors.length] }}>
                {s.name || s.service || s.task || "Resource"}
              </span>
            ))}
          </div>
        </>
      )}
    </Section>
  );
}
