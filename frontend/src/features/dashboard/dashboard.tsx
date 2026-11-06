import { Link } from "react-router";
import { HugeiconsIcon } from "@hugeicons/react";
import { CubeIcon, HierarchySquare01Icon, ServerStack01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { useApi } from "@/lib/queries";
import type { Service, Node, Stats, Timeseries } from "@/lib/types";
import { useSession } from "@/features/session/session";
import { Page, ErrorMessage } from "@/components/common";
import { Live } from "@/features/resources/lists";
import { ResourceUsageChart, TimeseriesChart } from "./chart";
import { buildChartData } from "./chart-data";
import { PinnedNodes, PinnedServices } from "./pinned-resources";
import { bytes } from "@/lib/format";
import { Skeleton } from "@/components/ui/skeleton";

const cores = (value: number) => value.toLocaleString(undefined, { maximumFractionDigits: 2 }) + " vCPU";
const storage = (value: number) => bytes(value, false);
const count = (value: number, name: string) => value.toLocaleString() + " " + name + (value === 1 ? "" : "s");
const hasSamples = (rows: Timeseries[] | undefined, metric: "cpu" | "memory") =>
  buildChartData(rows || [], metric).data.length > 0;

function PinLoading({ title }: { title: string }) {
  return (
    <section className="space-y-3" aria-label={title}>
      <h2 className="text-sm font-medium text-muted-foreground">{title}</h2>
      <div role="status" aria-busy="true" aria-label={title + " loading"} className="grid gap-3 lg:grid-cols-2">
        <span className="sr-only">Loading…</span>
        <Skeleton aria-hidden="true" className="h-32" />
        <Skeleton aria-hidden="true" className="h-32" />
      </div>
    </section>
  );
}

export function Dashboard() {
  const { me, version } = useSession();
  const services = useApi<Service[]>("/api/services");
  const nodes = useApi<Node[]>("/api/nodes");
  const stats = useApi<Stats | null>("/api/stats");
  const series = useApi<Timeseries[]>("/api/nodes/ts", !!version?.statistics);
  const cpu = useApi<Timeseries[]>("/api/services/ts/cpu", !!version?.statistics);
  const memory = useApi<Timeseries[]>("/api/services/ts/memory", !!version?.statistics);
  const s = stats.data;
  const cpuUsed = s?.cpu && Number.isFinite(s.cpu.cores) && Number.isFinite(s.cpu.usage)
    ? s.cpu.cores * s.cpu.usage / 100 : undefined;
  const resources = [
    { label: "Storage", used: s?.disk?.used, total: s?.disk?.total, usage: s?.disk?.usage, format: storage },
    { label: "Memory", used: s?.memory?.used, total: s?.memory?.total, usage: s?.memory?.usage, format: bytes },
    { label: "CPU", used: cpuUsed, total: s?.cpu?.cores, usage: s?.cpu?.usage, format: cores },
  ];
  const nodeCpu = hasSamples(series.data, "cpu");
  const nodeMemory = hasSamples(series.data, "memory");
  const serviceCpu = hasSamples(cpu.data, "cpu");
  const serviceMemory = hasSamples(memory.data, "memory");
  const historyError = series.error || cpu.error || memory.error;
  const emptyHistory = version?.statistics && !series.isPending && !cpu.isPending && !memory.isPending &&
    !nodeCpu && !nodeMemory && !serviceCpu && !serviceMemory && !historyError;
  return (
    <Page title="Overview" description="A live view of your Docker Swarm" actions={<Live handler="index" />}>
      <ErrorMessage error={services.error || nodes.error || stats.error} />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="min-w-0 rounded-xl border bg-card p-4">
          <h2 className="text-sm font-medium text-muted-foreground">Cluster</h2>
          {nodes.isPending ? (
            <div role="status" aria-busy="true" aria-label="Nodes loading" className="my-2">
              <span className="sr-only">Loading…</span><Skeleton aria-hidden="true" className="h-9 w-28" />
            </div>
          ) : <Link to="/nodes" className="my-2 inline-block text-3xl font-semibold tracking-tight underline-offset-4 hover:underline">{nodes.data ? count(nodes.data.length, "node") : "—"}</Link>}
          <div className="flex min-h-7 flex-wrap gap-1.5">
            {nodes.isPending ? (
              <>
                <Skeleton aria-hidden="true" className="h-7 w-28" />
                <Skeleton aria-hidden="true" className="h-7 w-28" />
              </>
            ) : nodes.data ? [
              { role: "manager", icon: HierarchySquare01Icon, amount: nodes.data.filter((node) => node.role === "manager").length },
              { role: "worker", icon: ServerStack01Icon, amount: nodes.data.filter((node) => node.role === "worker").length },
            ].map(({ role, icon, amount }) => (
              <Badge key={role} variant="secondary" className="gap-1.5 py-0.5 pl-1 pr-2">
                <HugeiconsIcon icon={icon} strokeWidth={2} aria-hidden="true" className="size-3 shrink-0" />
                <span className="inline-flex min-w-5 items-center justify-center rounded-sm bg-card px-1 py-0.5 font-semibold tabular-nums">{amount.toLocaleString()}</span>{" "}
                {role}{amount === 1 ? "" : "s"}
              </Badge>
            )) : <p className="text-xs text-muted-foreground">Connected hosts</p>}
          </div>
          {services.isPending ? (
            <div role="status" aria-busy="true" aria-label="Services loading" className="mt-2">
              <span className="sr-only">Loading services…</span><Skeleton aria-hidden="true" className="h-7 w-28" />
            </div>
          ) : services.data ? (
            <Badge asChild variant="secondary" className="mt-2 gap-1.5 py-0.5 pl-1 pr-2">
              <Link to="/services">
                <HugeiconsIcon icon={CubeIcon} strokeWidth={2} aria-hidden="true" className="size-3 shrink-0" />
                <span className="inline-flex min-w-5 items-center justify-center rounded-sm bg-card px-1 py-0.5 font-semibold tabular-nums">{services.data.length.toLocaleString()}</span>{" "}
                service{services.data.length === 1 ? "" : "s"}
              </Link>
            </Badge>
          ) : <Link to="/services" className="mt-2 inline-block text-xs text-muted-foreground underline-offset-4 hover:text-foreground hover:underline">
            Services unavailable
          </Link>}
        </div>
        {resources.map(({ label, ...resource }) => (
          <div key={label} className="min-w-0 rounded-xl border bg-card p-4">
            <h2 className="text-sm font-medium text-muted-foreground">{label}</h2>
            {stats.isPending ? (
              <div role="status" aria-busy="true" aria-label={label + " loading"} className="mt-4 flex min-h-20 items-center gap-3">
                <span className="sr-only">Loading…</span>
                <Skeleton aria-hidden="true" className="size-16 shrink-0 rounded-full" />
                <div aria-hidden="true" className="min-w-0 flex-1 space-y-2">
                  <Skeleton className="h-5 w-full" /><Skeleton className="h-3 w-3/4" />
                </div>
              </div>
            ) : <ResourceUsageChart title={label} {...resource} />}
          </div>
        ))}
      </div>
      {nodes.isPending && !!me?.["node-dashboard"]?.length ? <PinLoading title="Pinned nodes" /> : (
        <PinnedNodes rows={(nodes.data || []).filter((node) => me?.["node-dashboard"]?.includes(node.id))} />
      )}
      {!!version?.statistics && (series.isPending || nodeCpu || nodeMemory) && (
        <div className="grid gap-4 lg:grid-cols-2">
          {(series.isPending || nodeCpu) && <TimeseriesChart title="CPU utilization by Node" series={series.data || []} pending={series.isPending} metric="cpu" />}
          {(series.isPending || nodeMemory) && <TimeseriesChart title="Memory utilization by Node" series={series.data || []} pending={series.isPending} metric="memory" />}
        </div>
      )}
      {services.isPending && !!me?.["service-dashboard"]?.length ? <PinLoading title="Pinned services" /> : (
        <PinnedServices rows={(services.data || []).filter((service) => me?.["service-dashboard"]?.includes(service.id))} />
      )}
      {!!version?.statistics && (cpu.isPending || memory.isPending || serviceCpu || serviceMemory) && (
        <div className="grid gap-4 lg:grid-cols-2">
          {(cpu.isPending || serviceCpu) && <TimeseriesChart title="CPU usage by Service" series={cpu.data || []} pending={cpu.isPending} metric="cpu" cpuUnit="vcpu" />}
          {(memory.isPending || serviceMemory) && <TimeseriesChart title="Memory usage by Service" series={memory.data || []} pending={memory.isPending} metric="memory" memoryUnit="mib" />}
        </div>
      )}
      {!version?.statistics && (
        <p className="rounded-lg border border-dashed px-4 py-3 text-sm text-muted-foreground">
          Historical statistics are disabled. Configure InfluxDB to view resource charts.
        </p>
      )}
      {emptyHistory && <p className="rounded-lg border border-dashed px-4 py-3 text-sm text-muted-foreground">
        Historical samples have not arrived yet. Check the Swarmpit agent and InfluxDB.
      </p>}
      <ErrorMessage error={historyError} />
    </Page>
  );
}
