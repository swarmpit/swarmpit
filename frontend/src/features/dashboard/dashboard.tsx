import { useApi } from "@/lib/queries";
import type { Service, Node, Stats, Timeseries } from "@/lib/types";
import { useSession } from "@/features/session/session";
import { Page, Section, ErrorMessage, Loading } from "@/components/common";
import { ServicesTable, NodesTable } from "@/features/resources/tables";
import { Live } from "@/features/resources/lists";
import { TimeseriesChart } from "./chart";
import { bytes } from "@/lib/format";
export function Dashboard() {
  const { me, version } = useSession();
  const services = useApi<Service[]>("/api/services");
  const nodes = useApi<Node[]>("/api/nodes");
  const stats = useApi<Stats | null>("/api/stats");
  const series = useApi<Timeseries[]>("/api/nodes/ts", !!version?.statistics);
  const cpu = useApi<Timeseries[]>(
    "/api/services/ts/cpu",
    !!version?.statistics,
  );
  const memory = useApi<Timeseries[]>(
    "/api/services/ts/memory",
    !!version?.statistics,
  );
  const s = stats.data;
  return (
    <Page
      title="Overview"
      description="A live view of your Docker Swarm"
      actions={<Live handler="index" />}
    >
      <ErrorMessage error={services.error || nodes.error || stats.error} />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ["Nodes", nodes.data?.length ?? "—", "Connected hosts"],
          ["Services", services.data?.length ?? "—", "Deployed workloads"],
          [
            "CPU",
            s?.cpu?.usage != null ? s.cpu.usage.toFixed(1) + "%" : "—",
            "Cluster usage",
          ],
          [
            "Memory",
            s?.memory
              ? bytes(s.memory.used) + " / " + bytes(s.memory.total)
              : "—",
            "Cluster memory",
          ],
        ].map(([label, value, caption]) => (
          <div key={label} className="rounded-xl border bg-card p-5">
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="my-2 text-2xl font-semibold tracking-tight">
              {value}
            </p>
            <p className="text-xs text-muted-foreground">{caption}</p>
          </div>
        ))}
      </div>
      {s?.disk && (
        <Section title="Storage">
          <p className="text-sm">
            {bytes(s.disk.used)} of {bytes(s.disk.total)} used ·{" "}
            {s.disk.usage.toFixed(1)}%
          </p>
        </Section>
      )}
      {services.isPending || nodes.isPending ? (
        <Loading />
      ) : (
        <>
          <Section title="Pinned services">
            <ServicesTable
              rows={(services.data || []).filter((s) =>
                me?.["service-dashboard"]?.includes(s.id),
              )}
            />
          </Section>
          <Section title="Pinned nodes">
            <NodesTable
              rows={(nodes.data || []).filter((n) =>
                me?.["node-dashboard"]?.includes(n.id),
              )}
            />
          </Section>
        </>
      )}
      <TimeseriesChart title="Node resource usage" series={series.data || []} />
      <TimeseriesChart
        title="Service CPU usage"
        series={cpu.data || []}
        metric="cpu"
        cpuUnit="vcpu"
      />
      <TimeseriesChart
        title="Service memory usage"
        series={memory.data || []}
        metric="memory"
        memoryUnit="mib"
      />
      {!version?.statistics && (
        <p className="text-sm text-muted-foreground">
          Historical statistics are disabled. Configure InfluxDB to view
          resource charts.
        </p>
      )}
      <ErrorMessage error={series.error || cpu.error || memory.error} />
    </Page>
  );
}
