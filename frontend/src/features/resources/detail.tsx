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
  TextField,
} from "@/components/common";
import { Button } from "@/components/ui/button";
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
export function RelatedServices({ path }: { path: string }) {
  const query = useApi<Service[]>(path);
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading />
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
      {query.isPending ? <Loading /> : <TasksTable rows={query.data || []} />}
    </>
  );
}
export function DetailLoader<T>({
  path,
  children,
}: {
  path: string;
  children: (data: T) => ReactNode;
}) {
  const query = useApi<T>(path);
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? <Loading /> : query.data ? children(query.data) : null}
    </>
  );
}
export function NetworkDetail() {
  const { id = "" } = useParams();
  const path = "/api/networks/" + segment(id);
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
          <Section title="Services">
            <RelatedServices path={path + "/services"} />
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
            <TextField
              label="Contents"
              value={decodeText(c.data || "")}
              readOnly
            />
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
                    <Link to={"/nodes/" + segment(id) + "/edit"}>Edit</Link>
                  </Button>
                  <Action
                    label="Remove node"
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
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="tasks">Tasks</TabsTrigger>
              <TabsTrigger value="metrics">Metrics</TabsTrigger>
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
  const logOpen = search.get("log") === "1";
  const logsSupported = ["json-file", "journald", "local", "loki"].includes(
    t.logdriver || "",
  );
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
      actions={
        <>
          <Live handler="task-info" params={{ serviceName: t.serviceName }} />
          <Button
            type="button"
            variant="outline"
            disabled={!logsSupported}
            onClick={() => showLogs(true)}
          >
            View logs
          </Button>
        </>
      }
    >
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
      {logOpen && (
        <div className="space-y-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => showLogs(false)}
          >
            Close logs
          </Button>
          <ServiceLogs key={t.id} id={t.serviceName} initialTask={t.id} />
        </div>
      )}
      <ErrorMessage error={series.error} />
      <TimeseriesChart
        title="Resource usage"
        series={series.data ? [series.data] : []}
        memoryUnit="mib"
        cpuUnit="vcpu"
      />
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
  const stats = node.state === "down" ? null : node.stats;
  const cpu = stats?.cpu;
  const usedCpu =
    cpu?.cores != null && cpu.usedPercentage != null
      ? (cpu.cores * cpu.usedPercentage) / 100
      : undefined;
  return (
    <Section title="Current usage">
      {stats && (stats.cpu || stats.memory || stats.disk) ? (
        <Properties
          values={{
            "CPU usage": percentage(cpu?.usedPercentage),
            CPU: vcpu(usedCpu) + " / " + vcpu(cpu?.cores) + " vCPU",
            "Memory usage": percentage(stats.memory?.usedPercentage),
            "Memory used": bytes(stats.memory?.used),
            "Memory total": bytes(stats.memory?.total),
            "Disk usage": percentage(stats.disk?.usedPercentage),
            "Disk used": bytes(stats.disk?.used),
            "Disk total": bytes(stats.disk?.total),
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
