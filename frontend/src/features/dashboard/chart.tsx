import { useMemo, useRef, useState } from "react";
import { Area, AreaChart, CartesianGrid, Dot, Label, Pie, PieChart, XAxis, YAxis } from "recharts";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft02Icon, ArrowRight02Icon, Download01Icon, SearchAddIcon, SearchMinusIcon, Undo02Icon } from "@hugeicons/core-free-icons";
import type { Timeseries } from "@/lib/types";
import { Empty, Loading } from "@/components/common";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent,
} from "@/components/ui/chart";
import { buildChartData } from "./chart-data";
import { exportChartImage } from "./chart-export";

const numberFormat = new Intl.NumberFormat(undefined, { maximumSignificantDigits: 4 });
const formatTime = (time: number) =>
  new Date(time).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

function Plot({
  metric, unit, data, config, pending, showHeading,
}: {
  metric: "cpu" | "memory";
  unit: string;
  pending: boolean;
  showHeading: boolean;
} & ReturnType<typeof buildChartData>) {
  const label = (metric === "cpu" ? "CPU" : "Memory") + " (" + unit + ")";
  const [hours, setHours] = useState("1");
  const [selection, setSelection] = useState<[number, number] | null>(null);
  const [hidden, setHidden] = useState<string[]>([]);
  const occurrences = new Map<string, number>();
  const identities = Object.fromEntries(Object.entries(config).map(([key, value]) => {
    const name = String(value.label);
    const occurrence = occurrences.get(name) || 0;
    occurrences.set(name, occurrence + 1);
    return [key, JSON.stringify([name, occurrence])];
  }));
  const hiddenKeys = Object.keys(config).filter((key) => hidden.includes(identities[key]));
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState("");
  const chartRef = useRef<HTMLDivElement>(null);
  const latest = data.at(-1)?.time ?? 0;
  const startTime = selection?.[0] ?? latest - Number(hours) * 60 * 60 * 1000;
  const endTime = selection?.[1] ?? latest;
  const firstIndex = data.findIndex((row) => row.time >= startTime);
  const startIndex = firstIndex < 0 ? Math.max(0, data.length - 1) : firstIndex;
  const endIndex = Math.max(startIndex, data.findLastIndex((row) => row.time <= endTime));
  const visibleData = data.slice(startIndex, endIndex + 1);
  const hasWindow = data.length > 1;
  const resizeWindow = (factor: number) => {
    const width = Math.max(1, endIndex - startIndex);
    const nextWidth = Math.max(0, Math.min(data.length - 1, Math.floor(width * factor)));
    const start = Math.max(0, Math.min(data.length - 1 - nextWidth, Math.round((startIndex + endIndex - nextWidth) / 2)));
    setSelection([data[start].time, data[start + nextWidth].time]);
  };
  const moveWindow = (direction: -1 | 1) => {
    const width = endIndex - startIndex;
    const start = Math.max(0, Math.min(data.length - 1 - width, startIndex + direction * Math.max(1, width)));
    setSelection([data[start].time, data[start + width].time]);
  };
  const saveImage = async () => {
    setExportError("");
    setExporting(true);
    try {
      await exportChartImage(chartRef.current, metric);
    } catch {
      setExportError("Image export failed. Try again once the chart is visible.");
    } finally {
      setExporting(false);
    }
  };
  return (
    <div className="min-w-0 space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        {showHeading ? <h3 className="text-sm font-medium">{label}</h3>
          : <span className="text-xs text-muted-foreground">{unit}</span>}
        {!pending && data.length > 0 && <div className="flex flex-wrap items-center gap-1">
          <Select value={selection ? "custom" : hours} onValueChange={(value) => { setHours(value); setSelection(null); }}>
            <SelectTrigger size="sm" aria-label={label + " time range"}><SelectValue /></SelectTrigger>
            <SelectContent>
              {selection && <SelectItem value="custom" disabled>Custom range</SelectItem>}
              <SelectItem value="1">Last hour</SelectItem>
              <SelectItem value="6">Last 6 hours</SelectItem>
              <SelectItem value="24">Last 24 hours</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="ghost" size="icon-sm" title="Zoom in" aria-label={label + " zoom in"} disabled={!hasWindow || startIndex === endIndex} onClick={() => resizeWindow(0.5)}><HugeiconsIcon icon={SearchAddIcon} strokeWidth={2} aria-hidden="true" /></Button>
          <Button variant="ghost" size="icon-sm" title="Zoom out" aria-label={label + " zoom out"} disabled={!hasWindow || (startIndex === 0 && endIndex === data.length - 1)} onClick={() => resizeWindow(2)}><HugeiconsIcon icon={SearchMinusIcon} strokeWidth={2} aria-hidden="true" /></Button>
          <Button variant="ghost" size="icon-sm" title="Pan earlier" aria-label={label + " pan earlier"} disabled={!hasWindow || startIndex === 0} onClick={() => moveWindow(-1)}><HugeiconsIcon icon={ArrowLeft02Icon} strokeWidth={2} aria-hidden="true" /></Button>
          <Button variant="ghost" size="icon-sm" title="Pan later" aria-label={label + " pan later"} disabled={!hasWindow || endIndex === data.length - 1} onClick={() => moveWindow(1)}><HugeiconsIcon icon={ArrowRight02Icon} strokeWidth={2} aria-hidden="true" /></Button>
          <Button variant="ghost" size="icon-sm" title="Reset view" aria-label={label + " reset view"} onClick={() => { setSelection(null); setHours("1"); }}><HugeiconsIcon icon={Undo02Icon} strokeWidth={2} aria-hidden="true" /></Button>
          <Button variant="ghost" size="icon-sm" title="Export PNG" aria-label={label + " export PNG"} disabled={exporting} onClick={saveImage}><HugeiconsIcon icon={Download01Icon} strokeWidth={2} aria-hidden="true" /></Button>
        </div>}
      </div>
      {exportError && <p role="alert" className="text-sm text-destructive">{exportError}</p>}
      {pending ? <Loading variant="chart" label={label + " loading"} /> : !data.length ? <Empty>Statistics are not available yet.</Empty> : (
        <ChartContainer ref={chartRef} config={config} className="h-64 w-full min-w-0 aspect-auto [&_.recharts-surface:focus-visible]:outline-ring">
          <AreaChart
            accessibilityLayer
            aria-label={label + ": resource samples from " + new Date(data[startIndex].time).toLocaleString() + " to " + new Date(data[endIndex].time).toLocaleString()}
            data={visibleData}
            margin={{ top: 8, right: 12, bottom: 0, left: 0 }}
          >
            <CartesianGrid vertical={false} />
            <XAxis
              dataKey="time"
              type="number"
              scale="time"
              domain={["dataMin", "dataMax"]}
              tickFormatter={formatTime}
              tickLine={false}
              axisLine={false}
              minTickGap={32}
              tickMargin={10}
            />
            <YAxis
              width={48}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value: number) => numberFormat.format(value)}
              domain={[0, (maximum: number) => Math.max(unit === "%" ? 100 : 1, maximum)]}
            />
            <ChartTooltip
              content={
                <ChartTooltipContent
                  labelFormatter={(_value, payload) => {
                    const time = payload[0]?.payload?.time;
                    return typeof time === "number" ? new Date(time).toLocaleString() : "";
                  }}
                  formatter={(value, name, item) => (
                    <>
                      <span
                        aria-hidden="true"
                        className="size-2 shrink-0 rounded-sm"
                        style={{ backgroundColor: item.color }}
                      />
                      <span className="text-muted-foreground">{config[String(name)]?.label ?? name}</span>
                      <span className="ml-auto font-mono tabular-nums">
                        {typeof value === "number" ? numberFormat.format(value) : String(value)} {unit}
                      </span>
                    </>
                  )}
                />
              }
            />
            <ChartLegend content={<ChartLegendContent
              className="max-h-24 flex-wrap gap-x-4 gap-y-2 overflow-y-auto [&>button]:max-w-full [&>button]:break-all"
              hiddenKeys={hiddenKeys}
              onToggleSeries={(key) => setHidden((keys) => keys.includes(identities[key]) ? keys.filter((item) => item !== identities[key]) : [...keys, identities[key]])}
            />} />
            {Object.keys(config).map((key) => (
              <Area
                key={key}
                dataKey={key}
                hide={hiddenKeys.includes(key)}
                type="linear"
                stroke={`var(--color-${key})`}
                strokeWidth={2}
                fill={`var(--color-${key})`}
                fillOpacity={0.12}
                connectNulls={false}
                dot={({ cx, cy, index }) => {
                  if (cx == null || cy == null ||
                    visibleData[index - 1]?.[key] != null || visibleData[index + 1]?.[key] != null) return null;
                  return <Dot key={index} cx={cx} cy={cy} r={3} strokeWidth={0} fill={`var(--color-${key})`} />;
                }}
                activeDot={{ r: 4 }}
                isAnimationActive={false}
              />
            ))}
          </AreaChart>
        </ChartContainer>
      )}
    </div>
  );
}

export function ResourceUsageChart({
  title, used, total, usage, format, color = "var(--primary)",
}: {
  title: string;
  used: number | undefined;
  total: number | undefined;
  usage: number | undefined;
  format: (value: number) => string;
  color?: string;
}) {
  const capacityAvailable = typeof used === "number" && Number.isFinite(used) && used >= 0 &&
    typeof total === "number" && Number.isFinite(total) && total >= 0;
  const chartAvailable = capacityAvailable && total > 0 && used <= total &&
    typeof usage === "number" && Number.isFinite(usage) && usage >= 0 && usage <= 100;
  const config = {
    used: { label: "Used", color: typeof usage === "number" && usage > 90 ? "var(--destructive)"
      : typeof usage === "number" && usage >= 75 ? "var(--warning)" : color },
    free: { label: "Free", color: "var(--border)" },
  };
  return (
    <div className="mt-3 flex min-w-0 items-center gap-3">
      {chartAvailable && (
        <ChartContainer config={config} initialDimension={{ width: 64, height: 64 }}
          className="size-16 shrink-0 aspect-square [&_.recharts-surface:focus-visible]:outline-ring">
          <PieChart accessibilityLayer aria-label={title + " usage breakdown"}>
            <ChartTooltip content={
              <ChartTooltipContent hideLabel nameKey="name" formatter={(value, name, item) => (
                <>
                  <span aria-hidden="true" className="size-2 shrink-0 rounded-sm" style={{ backgroundColor: item.payload.fill }} />
                  <span className="text-muted-foreground">{config[name as keyof typeof config]?.label}</span>
                  <span className="ml-auto font-mono tabular-nums">
                    {format(item.payload.amount)} · {Number(value).toFixed(1)}%
                  </span>
                </>
              )} />
            } />
            <Pie data={[
              { name: "used", value: usage, amount: used, fill: "var(--color-used)" },
              { name: "free", value: 100 - usage, amount: total - used, fill: "var(--color-free)" },
            ]} dataKey="value" nameKey="name" innerRadius={23} outerRadius={29}
              startAngle={90} endAngle={-270} strokeWidth={0} isAnimationActive={false}>
              <Label position="center" className="fill-foreground text-xs font-semibold" value={usage.toFixed(1) + "%"} />
            </Pie>
          </PieChart>
        </ChartContainer>
      )}
      <div className="min-w-0">
        <p className="sr-only">{capacityAvailable ? format(used) + " / " + format(total) : "—"}</p>
        <p aria-hidden="true" className="text-sm font-semibold tabular-nums">{capacityAvailable ? format(used) : "—"}</p>
        {capacityAvailable && <p aria-hidden="true" className="text-xs text-muted-foreground tabular-nums">/ {format(total)} total</p>}
        <p className="text-xs text-muted-foreground">{!chartAvailable ? "Statistics unavailable."
          : usage > 90 ? "Critical usage" : usage >= 75 ? "High usage" : "Used"}</p>
      </div>
    </div>
  );
}

export function TimeseriesChart({
  title, series, metric, memoryUnit = "percent", cpuUnit = "percent", pending = false,
}: {
  title: string;
  series: Timeseries[];
  pending?: boolean;
  metric?: "cpu" | "memory";
  memoryUnit?: "percent" | "mib";
  cpuUnit?: "percent" | "vcpu";
}) {
  const plots = useMemo(() => {
    const metrics: ("cpu" | "memory")[] = metric ? [metric] : ["cpu", "memory"];
    return metrics.map((metric) => ({ metric, ...buildChartData(series, metric) }));
  }, [series, metric]);
  return (
    <Card>
      <CardHeader>
        <CardTitle role="heading" aria-level={2}>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {!pending && plots.every((plot) => !plot.data.length) ? (
          <Empty>Statistics are not available yet.</Empty>
        ) : (
          <div className={metric ? "" : "grid gap-6 lg:grid-cols-2"}>
            {plots.map((plot) => (
              <Plot
                key={plot.metric}
                {...plot}
                pending={pending}
                showHeading={!metric}
                unit={plot.metric === "cpu"
                  ? cpuUnit === "vcpu" ? "vCPU" : "%"
                  : memoryUnit === "mib" ? "MiB" : "%"}
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
