import { HugeiconsIcon } from "@hugeicons/react";
import { ChartLineData01Icon, DashboardSquare01Icon, Delete02Icon, File01Icon, PencilEdit01Icon, PinIcon, PinOffIcon, Task01Icon } from "@hugeicons/core-free-icons";
import type { ReactNode } from "react";
import { useNavigate, useParams, useSearchParams, Link } from "react-router";
import { useApi } from "@/lib/queries";
import { segment } from "@/lib/api";
import { date, bytes, decodeText } from "@/lib/format";
import {
  Page,
  Section,
  Properties,
  PairValues,
  Action,
  Loading,
  ErrorMessage,
  Status,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { ConfigValueField } from "@/components/config-value-field";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { useSession } from "@/features/session/session";
import type {
  Network,
  Volume,
  Secret,
  Config,
  Node,
  Task,
  Service,
  Timeseries,
} from "@/lib/types";
import { ServicesTable, TasksTable } from "./tables";
import { Live } from "./lists";
import { TimeseriesChart } from "@/features/dashboard/chart";
import { ServiceLogs } from "@/features/services/detail";
import { supportsLogs } from "@/features/services/logs";
import { NetworkMap } from "./network-map";
import { NodeResourceCharts } from "./node-cards";
export function RelatedServices({ path }: { path: string }) {
  const query = useApi<Service[]>(path);
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading variant="table" />
      ) : (
        <ServicesTable rows={query.data || []} />
      )}
    </>
  );
}
export function RelatedTasks({ path }: { path: string }) {
  const query = useApi<Task[]>(path);
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading variant="table" />
      ) : (
        <TasksTable rows={query.data || []} />
      )}
    </>
  );
}
export function DetailLoader<T>({
  path,
  children,
  loadingVariant = "content",
}: {
  path: string;
  children: (data: T) => ReactNode;
  loadingVariant?: "content" | "table";
}) {
  const query = useApi<T>(path);
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading variant={loadingVariant} />
      ) : query.data ? (
        children(query.data)
      ) : null}
    </>
  );
}
export function NetworkDetail() {
  const { id = "" } = useParams();
  const path = "/api/networks/" + segment(id);
  const services = useApi<Service[]>(path + "/services");
  const { me } = useSession();
  const nav = useNavigate();
  return (
    <DetailLoader<Network> path={path}>
      {(n) => (
        <Page
          title={n.networkName}
          description="Network"
          actions={
            me?.role !== "viewer" && (
              <Action
                label="Delete network"
                icon={Delete02Icon}
                destructive
                path={path}
                method="DELETE"
                onSuccess={() => nav("/networks")}
              />
            )
          }
        >
          <Section title="Configuration">
            <Properties
              values={{
                ID: n.id,
                Driver: n.driver,
                Scope: n.scope,
                Subnet: n.ipam?.subnet,
                Gateway: n.ipam?.gateway,
                Internal: n.internal ? "Yes" : "No",
                Attachable: n.attachable ? "Yes" : "No",
                Ingress: n.ingress ? "Yes" : "No",
                IPv6: n.enableIPv6 ? "Yes" : "No",
                Created: date(n.created),
              }}
            />
            <PairValues value={n.options} />
          </Section>
          <Section title="Network topology">
            <ErrorMessage error={services.error} />
            {services.isPending ? <Loading variant="chart" label="Network topology loading" /> : services.data && <NetworkMap key={n.id} network={n} services={services.data} />}
          </Section>
          <Section title="Services">
            {services.isPending ? <Loading variant="table" /> : services.data && <ServicesTable rows={services.data} />}
          </Section>
        </Page>
      )}
    </DetailLoader>
  );
}
export function VolumeDetail() {
  const { name = "" } = useParams();
  const path = "/api/volumes/" + segment(name);
  const { me } = useSession();
  const nav = useNavigate();
  return (
    <DetailLoader<Volume> path={path}>
      {(v) => (
        <Page
          title={v.volumeName}
          description="Volume"
          actions={
            me?.role !== "viewer" && (
              <Action
                label="Delete volume"
                icon={Delete02Icon}
                destructive
                path={path}
                method="DELETE"
                confirm="This removes the volume and its data. Ensure you have a backup before continuing."
                onSuccess={() => nav("/volumes")}
              />
            )
          }
        >
          <Section title="Configuration">
            <Properties
              values={{
                Driver: v.driver,
                Scope: v.scope,
                Mountpoint: v.mountpoint,
                Size: bytes(v.size),
                Stack: v.stack,
              }}
            />
            <PairValues value={v.options} />
          </Section>
          <Section title="Services">
            <RelatedServices path={path + "/services"} />
          </Section>
        </Page>
      )}
    </DetailLoader>
  );
}
export function SecretDetail() {
  const { id = "" } = useParams();
  const path = "/api/secrets/" + segment(id);
  const { me } = useSession();
  const nav = useNavigate();
  return (
    <DetailLoader<Secret> path={path}>
      {(s) => (
        <Page
          title={s.secretName}
          description="Secret"
          actions={
            me?.role !== "viewer" && (
              <Action
                label="Delete secret"
                icon={Delete02Icon}
                destructive
                path={path}
                method="DELETE"
                onSuccess={() => nav("/secrets")}
              />
            )
          }
        >
          <Section title="Metadata">
            <Properties
              values={{
                ID: s.id,
                Created: date(s.createdAt),
                Updated: date(s.updatedAt),
                Version: s.version,
              }}
            />
            <p className="text-sm text-muted-foreground">
              Docker does not expose secret contents after creation.
            </p>
          </Section>
          <Section title="Services">
            <RelatedServices path={path + "/services"} />
          </Section>
        </Page>
      )}
    </DetailLoader>
  );
}
export function ConfigDetail() {
  const { id = "" } = useParams();
  const path = "/api/configs/" + segment(id);
  const { me } = useSession();
  const nav = useNavigate();
  return (
    <DetailLoader<Config> path={path}>
      {(c) => (
        <Page
          title={c.configName}
          description="Config"
          actions={
            me?.role !== "viewer" && (
              <Action
                label="Delete config"
                icon={Delete02Icon}
                destructive
                path={path}
                method="DELETE"
                onSuccess={() => nav("/configs")}
              />
            )
          }
        >
          <Section title="Configuration">
            <Properties
              values={{
                ID: c.id,
                Created: date(c.createdAt),
                Updated: date(c.updatedAt),
                Version: c.version,
              }}
            />
            <ConfigValueField value={decodeText(c.data || "")} readOnly />
          </Section>
          <Section title="Services">
            <RelatedServices path={path + "/services"} />
          </Section>
        </Page>
      )}
    </DetailLoader>
  );
}
export function NodeDetail() {
  const { id = "" } = useParams();
  const path = "/api/nodes/" + segment(id);
  const { me, version } = useSession();
  const nav = useNavigate();
  const series = useApi<Timeseries[]>("/api/nodes/ts", !!version?.statistics);
  return (
    <DetailLoader<Node> path={path}>
      {(n) => (
        <Page
          title={n.nodeName}
          description="Node"
          actions={
            <>
              <Live handler="node-info" params={{ id }} />
              <Action
                icon={me?.["node-dashboard"]?.includes(n.id) ? PinOffIcon : PinIcon}
                label={
                  me?.["node-dashboard"]?.includes(n.id)
                    ? "Unpin"
                    : "Pin to dashboard"
                }
                path={path + "/dashboard"}
                method={
                  me?.["node-dashboard"]?.includes(n.id) ? "DELETE" : "POST"
                }
              />
              {me?.role === "admin" && (
                <>
                  <Button asChild variant="outline">
                    <Link to={"/nodes/" + segment(id) + "/edit"}><HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Edit</Link>
                  </Button>
                  <Action
                    label="Remove node"
                    icon={Delete02Icon}
                    destructive
                    method="DELETE"
                    path={path}
                    onSuccess={() => nav("/nodes")}
                  />
                </>
              )}
            </>
          }
        >
          <Tabs defaultValue="overview">
            <TabsList>
              <TabsTrigger value="overview"><HugeiconsIcon icon={DashboardSquare01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Overview</TabsTrigger>
              <TabsTrigger value="tasks"><HugeiconsIcon icon={Task01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Tasks</TabsTrigger>
              <TabsTrigger value="metrics"><HugeiconsIcon icon={ChartLineData01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Metrics</TabsTrigger>
            </TabsList>
            <TabsContent value="overview" className="space-y-6">
              <Section title="Node details">
                <Properties
                  values={{
                    ID: n.id,
                    State: <Status value={n.state} />,
                    Role: n.role + (n.leader ? " · leader" : ""),
                    Availability: n.availability,
                    Address: n.address,
                    Engine: n.engine,
                    OS: n.os,
                    Architecture: n.arch,
                    CPUs: n.resources?.cpu,
                    Memory: bytes(
                      n.resources?.memory == null
                        ? undefined
                        : n.resources.memory * 1024 ** 2,
                    ),
                  }}
                />
                <PairValues value={n.labels} />
              </Section>
              <NodeUsage node={n} />
              <Section title="Plugins">
                <Properties
                  values={{
                    "Network plugins":
                      n.plugins?.networks?.join(", ") || "None reported",
                    "Volume plugins":
                      n.plugins?.volumes?.join(", ") || "None reported",
                  }}
                />
              </Section>
            </TabsContent>
            <TabsContent value="tasks">
              <RelatedTasks path={path + "/tasks"} />
            </TabsContent>
            <TabsContent value="metrics">
              <ErrorMessage error={series.error} />
              <TimeseriesChart
                title="CPU and memory usage"
                pending={series.isPending && !!version?.statistics}
                series={(series.data || []).filter(
                  (s) => s.name === n.nodeName,
                )}
              />
            </TabsContent>
          </Tabs>
        </Page>
      )}
    </DetailLoader>
  );
}
export function TaskDetail() {
  const { id = "" } = useParams();
  const path = "/api/tasks/" + segment(id);
  return (
    <DetailLoader<Task> path={path}>
      {(t) => <TaskContent task={t} />}
    </DetailLoader>
  );
}
function TaskContent({ task: t }: { task: Task }) {
  const { version } = useSession();
  const [search, setSearch] = useSearchParams();
  const series = useApi<Timeseries>(
    "/api/tasks/" + segment(t.taskName) + "/ts",
    !!version?.statistics,
  );
  const logsSupported = supportsLogs(t.logdriver);
  const logOpen = search.get("log") === "1" && logsSupported;
  function showLogs(open: boolean) {
    setSearch((current) => {
      const next = new URLSearchParams(current);
      if (open) next.set("log", "1");
      else next.delete("log");
      return next;
    });
  }
  return (
    <Page
      title={t.taskName || t.id}
      description="Task"
      actions={<Live handler="task-info" params={{ serviceName: t.serviceName }} />}
    >
      <Tabs value={logOpen ? "logs" : "overview"} onValueChange={(value) => showLogs(value === "logs")}>
        <TabsList aria-label="Task sections">
          <TabsTrigger value="overview"><HugeiconsIcon icon={DashboardSquare01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Overview</TabsTrigger>
          <TabsTrigger value="logs" disabled={!logsSupported}><HugeiconsIcon icon={File01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Logs</TabsTrigger>
        </TabsList>
        <TabsContent value="overview" className="space-y-6">
          <Section title="Task details">
            <Properties
              values={{
                ID: t.id,
                State: <Status value={t.state} />,
                Desired: t.desiredState,
                Service: (
                  <Link
                    to={"/services/" + segment(t.serviceName)}
                    className="underline"
                  >
                    {t.serviceName}
                  </Link>
                ),
                Node: (
                  <Link to={"/nodes/" + segment(t.nodeId)} className="underline">
                    {t.nodeName}
                  </Link>
                ),
                Image: t.repository?.image,
                "Image digest": t.repository?.imageDigest,
                Created: date(t.createdAt),
                Updated: date(t.updatedAt),
                Error: t.status?.error || "None",
              }}
            />
          </Section>
          <TaskUsage task={t} />
          <ErrorMessage error={series.error} />
          <TimeseriesChart
            title="Resource usage"
            pending={series.isPending && !!version?.statistics}
            series={series.data ? [series.data] : []}
            memoryUnit="mib"
            cpuUnit="vcpu"
          />
        </TabsContent>
        <TabsContent value="logs">
          {logsSupported && <ServiceLogs key={t.id} id={t.serviceName} initialTask={t.id} />}
        </TabsContent>
      </Tabs>
    </Page>
  );
}
function percentage(value?: number) {
  return value == null ? "—" : value.toFixed(1) + "%";
}
function vcpu(value?: number) {
  return value == null ? "—" : value.toFixed(2);
}
function NodeUsage({ node }: { node: Node }) {
  const stats = node.state === "ready" ? node.stats : null;
  return (
    <Section title="Current usage">
      {stats && (stats.cpu || stats.memory || stats.disk) ? (
        <NodeResourceCharts node={node} />
      ) : (
        <p className="text-sm text-muted-foreground">
          Current resource statistics are not available.
        </p>
      )}
    </Section>
  );
}
function TaskUsage({ task }: { task: Task }) {
  if (task.state !== "running") return null;
  const stats = task.stats;
  return (
    <Section title="Current usage">
      {stats && (stats.cpu != null || stats.memory != null) ? (
        <Properties
          values={{
            "CPU usage": percentage(stats.cpuPercentage),
            CPU: vcpu(stats.cpu) + " / " + vcpu(stats.cpuLimit) + " vCPU",
            "Memory usage": percentage(stats.memoryPercentage),
            "Memory used": bytes(stats.memory),
            "Memory limit": bytes(stats.memoryLimit),
          }}
        />
      ) : (
        <p className="text-sm text-muted-foreground">
          Current resource statistics are not available.
        </p>
      )}
    </Section>
  );
}
