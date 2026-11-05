import { useId, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { useApi } from "@/lib/queries";
import { segment, url } from "@/lib/api";
import type { Service, Compose, LogLine, Task, Timeseries } from "@/lib/types";
import { date } from "@/lib/format";
import {
  Page,
  Section,
  Properties,
  PairValues,
  Action,
  ErrorMessage,
  Loading,
  Status,
  TextField,
  Field,
  Choice,
  Toggle,
  ResourceTable,
  Empty,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { DetailLoader, RelatedTasks } from "@/features/resources/detail";
import { Live } from "@/features/resources/lists";
import { TimeseriesChart } from "@/features/dashboard/chart";
import { useSession } from "@/features/session/session";

export function ServiceDetail() {
  const { id = "" } = useParams();
  return (
    <DetailLoader<Service> path={"/api/services/" + segment(id)}>
      {(s) => <ServiceContent key={s.id} service={s} routeId={id} />}
    </DetailLoader>
  );
}
function ServiceContent({
  service: s,
  routeId,
}: {
  service: Service;
  routeId: string;
}) {
  const { me, version } = useSession();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const tab =
    params.get("log") === "1" ? "logs" : params.get("tab") || "overview";
  const setTab = (value: string) => {
    const next = new URLSearchParams(params);
    next.delete("log");
    next.set("tab", value);
    setParams(next, { replace: true });
  };
  const [tag, setTag] = useState("");
  const [comment, setComment] = useState("");
  const path = "/api/services/" + segment(s.id);
  const editable = !!me && me.role !== "viewer";
  const tags = useApi<string[]>(
    url("/api/repository/tags", { repository: s.repository.name }),
    editable,
  );
  const tagList = useId();
  return (
    <Page
      title={s.serviceName}
      description={
        "Service · " + s.repository.name + ":" + (s.repository.tag || "latest")
      }
      actions={
        <>
          <Live handler="service-info" params={{ id: routeId }} />
          <Action
            label={
              me?.["service-dashboard"]?.includes(s.id)
                ? "Unpin"
                : "Pin to dashboard"
            }
            path={path + "/dashboard"}
            method={
              me?.["service-dashboard"]?.includes(s.id) ? "DELETE" : "POST"
            }
          />
          {editable && (
            <>
              {s.immutable ? (
                <Button variant="outline" disabled>
                  Edit
                </Button>
              ) : (
                <Button variant="outline" asChild>
                  <Link to={"/services/" + segment(s.id) + "/edit"}>Edit</Link>
                </Button>
              )}
              <Button variant="outline" asChild>
                <Link to={url("/stacks/create", { from: s.id })}>
                  Create stack from service
                </Link>
              </Button>
              <Action
                label="Stop"
                path={path + "/stop"}
                destructive
                disabled={s.mode === "global" || s.state === "not running"}
              />
              <Action
                label="Delete"
                path={path}
                method="DELETE"
                destructive
                onSuccess={() => navigate("/services")}
              />
            </>
          )}
        </>
      }
    >
      {s.immutable && (
        <p className="rounded-lg border p-4 text-sm text-muted-foreground">
          This service is immutable. Edit its owning stack to change its
          configuration.
        </p>
      )}
      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="flex h-auto flex-wrap">
          <TabsTrigger value="overview">Overview</TabsTrigger>
          <TabsTrigger value="configuration">Configuration</TabsTrigger>
          <TabsTrigger value="tasks">Tasks</TabsTrigger>
          <TabsTrigger value="logs">Logs</TabsTrigger>
          <TabsTrigger value="compose">Compose</TabsTrigger>
          <TabsTrigger value="metrics">Metrics</TabsTrigger>
        </TabsList>
        <TabsContent value="overview" className="space-y-6">
          <Section title="Service">
            <Properties
              values={{
                ID: s.id,
                Version: s.version,
                State: <Status value={s.state} />,
                Mode: s.mode,
                Replicas: s.mode === "global" ? "Global" : s.replicas,
                Tasks: s.status?.tasks
                  ? s.status.tasks.running + " / " + s.status.tasks.total
                  : "—",
                Image: s.repository.image,
                Digest: s.repository.imageDigest,
                Stack: s.stack ? (
                  <Link
                    className="underline"
                    to={"/stacks/" + segment(s.stack)}
                  >
                    {s.stack}
                  </Link>
                ) : (
                  "—"
                ),
                Created: date(s.createdAt),
                Updated: date(s.updatedAt),
                "Update status": s.status?.update,
                "Status message": s.status?.message,
              }}
            />
          </Section>
          {editable && (
            <Section title="Deployment actions">
              <div className="grid items-end gap-4 sm:grid-cols-2">
                <Field
                  label="New tag (empty to keep current tag)"
                  value={tag}
                  list={tagList}
                  onChange={(e) => setTag(e.target.value)}
                />
                <datalist id={tagList}>
                  {tags.data?.map((t) => (
                    <option key={t} value={t} />
                  ))}
                </datalist>
                <Field
                  label="History comment"
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
              </div>
              <ErrorMessage error={tags.error} />
              <div className="flex flex-wrap gap-3">
                <Action
                  label="Redeploy"
                  path={url(path + "/redeploy", {
                    tag: tag || undefined,
                    comment: comment || undefined,
                  })}
                  confirm="Replace running tasks with the selected image version?"
                />
                <Action
                  label="Rollback"
                  path={url(path + "/rollback", {
                    comment: comment || undefined,
                  })}
                  disabled={!s.deployment.rollbackAllowed}
                  confirm="Restore the previous service specification?"
                />
              </div>
            </Section>
          )}
        </TabsContent>
        <TabsContent value="configuration" className="space-y-6">
          <Section title="Container settings">
            <Properties
              values={{
                Hostname: s.hostname,
                Isolation: s.isolation,
                User: s.user,
                "Working directory": s.dir,
                TTY: s.tty ? "Yes" : "No",
                Entrypoint: s.entrypoint?.join(" · "),
                "Command arguments": s.command?.join(" · "),
              }}
            />
          </Section>
          <Section title="Published ports">
            <ResourceTable
              rows={s.ports}
              columns={[
                { label: "Published", render: (p) => p.hostPort },
                { label: "Container", render: (p) => p.containerPort },
                { label: "Protocol", render: (p) => p.protocol },
                { label: "Mode", render: (p) => p.mode },
              ]}
            />
          </Section>
          <Section title="Networks">
            <ResourceTable
              rows={s.networks}
              columns={[
                {
                  label: "Network",
                  render: (n) => (
                    <Link
                      className="underline"
                      to={"/networks/" + segment(n.id || n.networkName)}
                    >
                      {n.networkName}
                    </Link>
                  ),
                },
                {
                  label: "Aliases",
                  render: (n) => n.serviceAliases?.join(", ") || "—",
                },
              ]}
            />
          </Section>
          <Section title="Mounts">
            <ResourceTable
              rows={s.mounts}
              columns={[
                { label: "Type", render: (m) => m.type },
                { label: "Source", render: (m) => m.host || "Anonymous" },
                { label: "Target", render: (m) => m.containerPath },
                {
                  label: "Access",
                  render: (m) => (m.readOnly ? "Read only" : "Read / write"),
                },
                {
                  label: "Volume driver",
                  render: (m) => m.volumeOptions?.driver?.name || "—",
                },
              ]}
            />
          </Section>
          <Section title="Secrets">
            <ResourceTable
              rows={s.secrets}
              columns={[
                {
                  label: "Secret",
                  render: (m) =>
                    m.id ? (
                      <Link
                        className="underline"
                        to={"/secrets/" + segment(m.id)}
                      >
                        {m.secretName}
                      </Link>
                    ) : (
                      m.secretName
                    ),
                },
                { label: "Target", render: (m) => m.secretTarget },
                {
                  label: "UID / GID / Mode",
                  render: (m) =>
                    [m.uid, m.gid, m.mode?.toString(8)].join(" / "),
                },
              ]}
            />
          </Section>
          <Section title="Configs">
            <ResourceTable
              rows={s.configs}
              columns={[
                {
                  label: "Config",
                  render: (m) =>
                    m.id ? (
                      <Link
                        className="underline"
                        to={"/configs/" + segment(m.id)}
                      >
                        {m.configName}
                      </Link>
                    ) : (
                      m.configName
                    ),
                },
                { label: "Target", render: (m) => m.configTarget },
                {
                  label: "UID / GID / Mode",
                  render: (m) =>
                    [m.uid, m.gid, m.mode?.toString(8)].join(" / "),
                },
              ]}
            />
          </Section>
          {(
            [
              ["Environment variables", s.variables],
              ["Service labels", s.labels],
              ["Container labels", s.containerLabels],
              ["Extra hosts", s.hosts],
              ["Kernel parameters", s.sysctls],
              ["Links", s.links],
            ] as const
          ).map(([title, value]) => (
            <Section key={title} title={title}>
              <PairValues value={value} />
            </Section>
          ))}
          <Section title="Resources">
            <Properties
              values={{
                "CPU reservation": s.resources.reservation.cpu + " cores",
                "CPU limit": s.resources.limit.cpu + " cores",
                "Memory reservation": s.resources.reservation.memory + " MiB",
                "Memory limit": s.resources.limit.memory + " MiB",
              }}
            />
          </Section>
          <Section title="Deployment">
            <Properties
              values={{
                "Restart condition": s.deployment.restartPolicy.condition,
                "Restart delay": s.deployment.restartPolicy.delay + " seconds",
                "Restart window":
                  (s.deployment.restartPolicy.window || 0) + " seconds",
                "Restart attempts":
                  s.deployment.restartPolicy.attempts || "Unlimited",
                "Maximum replicas per node":
                  s.deployment.maxReplicas || "Unlimited",
                "Automatic redeploy": s.deployment.autoredeploy
                  ? "Enabled"
                  : "Disabled",
                Placement:
                  s.deployment.placement.map((p) => p.rule).join(" · ") ||
                  "None",
              }}
            />
            {(["update", "rollback"] as const).map((kind) => (
              <div className="space-y-3" key={kind}>
                <h3 className="capitalize text-sm font-medium">{kind}</h3>
                <Properties
                  values={{
                    Parallelism: s.deployment[kind].parallelism,
                    Delay: s.deployment[kind].delay + " seconds",
                    Order: s.deployment[kind].order,
                    "Failure action": s.deployment[kind].failureAction,
                  }}
                />
              </div>
            ))}
          </Section>
          <Section title="Healthcheck">
            <Properties
              values={{
                Test: s.healthcheck?.test?.join(" · ") || "Image default",
                Interval: s.healthcheck?.interval,
                Timeout: s.healthcheck?.timeout,
                "Start period": s.healthcheck?.startPeriod,
                Retries: s.healthcheck?.retries,
              }}
            />
          </Section>
          <Section title="Logging">
            <p className="text-sm">{s.logdriver.name}</p>
            <PairValues value={s.logdriver.opts} />
          </Section>
        </TabsContent>
        <TabsContent value="tasks">
          <RelatedTasks path={path + "/tasks"} />
        </TabsContent>
        <TabsContent value="logs">
          <ServiceLogs id={s.id} />
        </TabsContent>
        <TabsContent value="compose">
          <ServiceCompose id={s.id} />
        </TabsContent>
        <TabsContent value="metrics">
          {version?.statistics ? (
            <ServiceMetrics id={s.id} />
          ) : (
            <Empty>
              Historical statistics are disabled. Configure InfluxDB to view
              resource charts.
            </Empty>
          )}
        </TabsContent>
      </Tabs>
    </Page>
  );
}
function ServiceCompose({ id }: { id: string }) {
  const query = useApi<Compose>("/api/services/" + segment(id) + "/compose");
  return (
    <Section title="Compose">
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading />
      ) : (
        <TextField
          label="Generated compose"
          value={query.data?.spec.compose || ""}
          readOnly
          rows={25}
        />
      )}
    </Section>
  );
}
export function ServiceLogs({
  id,
  initialTask = "",
}: {
  id: string;
  initialTask?: string;
}) {
  const [history, setHistory] = useState("15m");
  const [follow, setFollow] = useState(true);
  const [filter, setFilter] = useState("");
  const [task, setTask] = useState(initialTask);
  const query = useApi<LogLine[]>(
    url("/api/services/" + segment(id) + "/logs", { since: history }),
    true,
    follow ? 3000 : undefined,
  );
  const lines = (Array.isArray(query.data) ? query.data : []).filter(
    (l) =>
      (!task || l.task === task) &&
      (!filter || l.line.toLowerCase().includes(filter.toLowerCase())),
  );
  return (
    <Section title="Service logs">
      <div className="grid items-end gap-4 sm:grid-cols-3">
        <Choice
          label="History"
          value={history}
          options={["1m", "15m", "1h", "24h"]}
          onChange={setHistory}
        />
        <Field
          label="Filter logs"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <Choice
          label="Task"
          value={task}
          options={[
            "",
            ...(initialTask ? [initialTask] : []),
            ...new Set(query.data?.map((l) => l.task || "") || []).values(),
          ].filter((v, i, a) => a.indexOf(v) === i)}
          onChange={setTask}
        />
      </div>
      <div className="flex items-center gap-4">
        <Toggle label="Follow logs" checked={follow} onChange={setFollow} />
        <Button variant="outline" onClick={() => void query.refetch()}>
          Refresh
        </Button>
        <Button
          variant="outline"
          onClick={() => {
            const link = document.createElement("a");
            const blob = URL.createObjectURL(
              new Blob(
                [
                  lines
                    .map((l) => (l.timestamp ? l.timestamp + " " : "") + l.line)
                    .join("\n"),
                ],
                { type: "text/plain" },
              ),
            );
            link.href = blob;
            link.download = "service-" + id + ".log";
            link.click();
            URL.revokeObjectURL(blob);
          }}
        >
          Download logs
        </Button>
      </div>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading />
      ) : (
        <pre
          tabIndex={0}
          className="max-h-[36rem] overflow-auto rounded-lg bg-muted p-4 font-mono text-xs leading-6"
        >
          {lines.map((line, i) => (
            <div key={i}>
              <span className="text-muted-foreground">
                {line.timestamp} {line.task && "[" + line.task + "] "}
              </span>
              {line.line}
            </div>
          ))}
          {!lines.length && "No matching log lines."}
        </pre>
      )}
    </Section>
  );
}
function ServiceMetrics({ id }: { id: string }) {
  const tasks = useApi<Task[]>("/api/services/" + segment(id) + "/tasks");
  return (
    <div className="space-y-6">
      <ErrorMessage error={tasks.error} />
      {tasks.isPending ? (
        <Loading />
      ) : (tasks.data || []).length ? (
        (tasks.data || []).map((t) => <TaskMetrics key={t.id} task={t} />)
      ) : (
        <Empty>No tasks have resource statistics.</Empty>
      )}
    </div>
  );
}
function TaskMetrics({ task }: { task: Task }) {
  const query = useApi<Timeseries>(
    "/api/tasks/" + segment(task.taskName) + "/ts",
    true,
    60000,
  );
  return (
    <div className="space-y-4">
      <h2 className="font-medium">{task.taskName}</h2>
      <ErrorMessage error={query.error} />
      <TimeseriesChart
        title="CPU usage"
        series={query.data ? [query.data] : []}
        metric="cpu"
        cpuUnit="vcpu"
      />
      <TimeseriesChart
        title="Memory usage"
        series={query.data ? [query.data] : []}
        metric="memory"
        memoryUnit="mib"
        cpuUnit="vcpu"
      />
    </div>
  );
}
