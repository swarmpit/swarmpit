import { useState, type ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { segment, url } from "@/lib/api";
import { useApi } from "@/lib/queries";
import { bytes, date } from "@/lib/format";
import type {
  Compose,
  Config,
  History,
  Network,
  Secret,
  Stack,
  Task,
  Volume,
} from "@/lib/types";
import {
  Action,
  Empty,
  ErrorMessage,
  Field,
  Loading,
  Page,
  Properties,
  Section,
  Status,
  TextField,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useSession } from "@/features/session/session";
import {
  DetailLoader,
  RelatedServices,
  RelatedTasks,
} from "@/features/resources/detail";
import {
  ConfigsTable,
  NetworksTable,
  SecretsTable,
  VolumesTable,
} from "@/features/resources/tables";
import { Live } from "@/features/resources/lists";
import { historyLabel, missingStackfile } from "./compose";

export function StackDetail() {
  const { name = "" } = useParams();
  return <StackPage key={name} name={name} />;
}

function StackPage({ name }: { name: string }) {
  const stacks = useApi<Stack[]>("/api/stacks");
  const path = "/api/stacks/" + segment(name);
  const file = useApi<Compose>(path + "/file");
  const { me } = useSession();
  const [comment, setComment] = useState("");
  const navigate = useNavigate();
  const stack = stacks.data?.find((s) => s.stackName === name);
  if (stacks.isPending) return <Loading />;
  if (!stack)
    return (
      <Page title={name}>
        <ErrorMessage error={stacks.error || new Error("Stack not found.")} />
      </Page>
    );
  const inactive =
    stack.state === "inactive" || (!stack.state && !stack.services?.length);
  const editable = !!me && me.role !== "viewer";
  const root = "/stacks/" + segment(name);
  const stored = missingStackfile(file.error) ? undefined : file.data;
  const fileError = missingStackfile(file.error) ? null : file.error;
  return (
    <Page
      title={name}
      description="Stack"
      actions={
        <>
          {!inactive && <Live handler="stack-info" params={{ name }} />}
          <Button asChild variant="outline">
            <Link to={root + (inactive ? "/activate" : "/compose")}>
              {editable
                ? inactive
                  ? "Activate stack"
                  : "Edit compose"
                : "View compose"}
            </Link>
          </Button>
        </>
      }
    >
      <ErrorMessage error={stacks.error} />
      <ErrorMessage error={fileError} />
      <Tabs defaultValue="overview">
        <TabsList className="h-auto max-w-full flex-wrap justify-start">
          {[
            "Overview",
            "Services",
            "Tasks",
            "Networks",
            "Volumes",
            "Configs",
            "Secrets",
            "Compose",
            "History",
          ].map((tab) => (
            <TabsTrigger key={tab} value={tab.toLowerCase()}>
              {tab}
            </TabsTrigger>
          ))}
        </TabsList>
        <TabsContent value="overview" className="space-y-6">
          <Section title="Stack details">
            <Properties
              values={{
                State: <Status value={inactive ? "inactive" : "active"} />,
                "Stored compose": file.isPending
                  ? "Loading…"
                  : stored
                    ? "Available"
                    : fileError
                      ? "Unavailable"
                      : "Not saved",
                Services: stack.services?.length || 0,
                Networks: stack.networks?.length || 0,
                Volumes: stack.volumes?.length || 0,
                Configs: stack.configs?.length || 0,
                Secrets: stack.secrets?.length || 0,
                Created: date(stack.createdAt),
                Updated: date(stack.updatedAt),
              }}
            />
            {inactive && (
              <p className="text-sm text-muted-foreground">
                This stack is saved but is not deployed. Activate it to deploy
                the stored compose.
              </p>
            )}
          </Section>
          {!inactive && <StackMetrics stack={stack} path={path} />}
          {editable && (
            <Section title="Stack operations">
              {!inactive && (
                <Field
                  label="Change comment"
                  value={comment}
                  maxLength={120}
                  onChange={(e) => setComment(e.target.value)}
                  hint="Optional note for redeploy or rollback history."
                />
              )}
              <div className="flex flex-wrap items-start gap-3">
                {inactive ? (
                  <Action
                    label="Delete stackfile"
                    path={path + "/file"}
                    method="DELETE"
                    destructive
                    disabled={!stored}
                    confirm="This removes the saved compose and all history. The inactive stack will no longer be available to activate."
                    onSuccess={() => navigate("/stacks")}
                  />
                ) : (
                  <>
                    <Action
                      label="Redeploy"
                      path={url(path + "/redeploy", {
                        comment: comment || undefined,
                      })}
                      disabled={!stored?.spec}
                      confirm="Redeploy this stack using the stored compose file?"
                    />
                    <Action
                      label="Rollback"
                      path={url(path + "/rollback", {
                        comment: comment || undefined,
                      })}
                      disabled={!stored?.previousSpec}
                      confirm="Deploy the previous compose version and swap it with the last deployed version?"
                    />
                    <Action
                      label="Deactivate"
                      path={path + "/deactivate"}
                      destructive
                      confirm="This removes the running stack. Its compose is saved so it can be activated again."
                      onSuccess={() => navigate("/stacks")}
                    />
                    <Action
                      label="Delete stack"
                      path={path}
                      method="DELETE"
                      destructive
                      confirm="This removes the running stack, saved compose and history. Docker retains volumes and external resources."
                      onSuccess={() => navigate("/stacks")}
                    />
                    {stored && (
                      <Action
                        label="Delete stackfile"
                        path={path + "/file"}
                        method="DELETE"
                        destructive
                        confirm="This deletes saved compose and history while leaving the stack running. Future editing will reconstruct compose from engine state."
                      />
                    )}
                  </>
                )}
              </div>
            </Section>
          )}
        </TabsContent>
        <TabsContent value="services">
          {inactive ? (
            <Empty>This stack is inactive.</Empty>
          ) : (
            <RelatedServices path={path + "/services"} />
          )}
        </TabsContent>
        <TabsContent value="tasks">
          {inactive ? (
            <Empty>This stack is inactive.</Empty>
          ) : (
            <RelatedTasks path={path + "/tasks"} />
          )}
        </TabsContent>
        <TabsContent value="networks">
          <Related inactive={inactive}>
            <DetailLoader<Network[]> path={path + "/networks"}>
              {(rows) => <NetworksTable rows={rows} />}
            </DetailLoader>
          </Related>
        </TabsContent>
        <TabsContent value="volumes">
          <Related inactive={inactive}>
            <DetailLoader<Volume[]> path={path + "/volumes"}>
              {(rows) => <VolumesTable rows={rows} />}
            </DetailLoader>
          </Related>
        </TabsContent>
        <TabsContent value="configs">
          <Related inactive={inactive}>
            <DetailLoader<Config[]> path={path + "/configs"}>
              {(rows) => <ConfigsTable rows={rows} />}
            </DetailLoader>
          </Related>
        </TabsContent>
        <TabsContent value="secrets">
          <Related inactive={inactive}>
            <DetailLoader<Secret[]> path={path + "/secrets"}>
              {(rows) => <SecretsTable rows={rows} />}
            </DetailLoader>
          </Related>
        </TabsContent>
        <TabsContent value="compose">
          {file.isPending ? (
            <Loading />
          ) : (
            <ComposeView
              name={name}
              stored={stored}
              fileError={fileError}
              inactive={inactive}
            />
          )}
        </TabsContent>
        <TabsContent value="history">
          <StackHistory name={name} editable={editable} />
        </TabsContent>
      </Tabs>
    </Page>
  );
}

function StackMetrics({ stack, path }: { stack: Stack; path: string }) {
  const tasks = useApi<
    (Task & { stats?: { cpu?: number; memory?: number } })[]
  >(path + "/tasks");
  const stats = useApi<{
    cpu?: { cores?: number };
    memory?: { total?: number };
  }>("/api/stats");
  const measured =
    tasks.data?.filter(
      (task) =>
        task.stats &&
        (task.stats.cpu !== undefined || task.stats.memory !== undefined),
    ) || [];
  const cpu = measured.reduce((sum, task) => sum + (task.stats?.cpu || 0), 0);
  const memory = measured.reduce(
    (sum, task) => sum + (task.stats?.memory || 0),
    0,
  );
  const running = (stack.services || []).reduce(
    (sum, service) => sum + (service.status?.tasks?.running || 0),
    0,
  );
  const desired = (stack.services || []).reduce(
    (sum, service) => sum + (service.status?.tasks?.total || 0),
    0,
  );
  return (
    <Section title="Resource usage">
      <Properties
        values={{
          "Running tasks": running + " / " + desired,
          "CPU usage": measured.length
            ? cpu.toFixed(2) +
              " vCPU" +
              (stats.data?.cpu?.cores === undefined
                ? ""
                : " / " + stats.data.cpu.cores + " cluster vCPU")
            : "Unavailable",
          "Memory usage": measured.length
            ? bytes(memory) +
              (stats.data?.memory?.total === undefined
                ? ""
                : " / " + bytes(stats.data.memory.total) + " cluster memory")
            : "Unavailable",
        }}
      />
      {(tasks.error ||
        stats.error ||
        (!tasks.isPending && !measured.length)) && (
        <p className="text-sm text-muted-foreground">
          Resource metrics are currently unavailable.
        </p>
      )}
    </Section>
  );
}

function Related({
  inactive,
  children,
}: {
  inactive: boolean;
  children: ReactNode;
}) {
  return inactive ? (
    <Empty>This stack is inactive.</Empty>
  ) : (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Includes external resources used by services in this stack. Volumes and
        external resources may remain after stack deletion.
      </p>
      {children}
    </div>
  );
}
function ComposeView({
  name,
  stored,
  fileError,
  inactive,
}: {
  name: string;
  stored?: Compose;
  fileError: unknown;
  inactive: boolean;
}) {
  const path = "/api/stacks/" + segment(name);
  const engine = useApi<Compose>(
    path + "/compose",
    !stored && !fileError && !inactive,
  );
  const { me } = useSession();
  const compose = stored?.spec.compose ?? engine.data?.spec.compose;
  if (fileError) return <ErrorMessage error={fileError} />;
  if (!stored && !inactive && engine.isPending) return <Loading />;
  return (
    <Section title={stored ? "Stored compose" : "Reconstructed compose"}>
      <ErrorMessage error={engine.error} />
      {compose === undefined ? (
        <Empty>No stored compose is available.</Empty>
      ) : (
        <>
          {!stored && (
            <p className="text-sm text-muted-foreground">
              The original stackfile is unavailable. Engine reconstruction
              cannot recover YAML comments or original syntax.
            </p>
          )}
          <TextField label="Compose YAML" value={compose} readOnly rows={24} />
          <div className="flex flex-wrap gap-3">
            <Button asChild variant="outline">
              <a
                href={
                  "data:text/yaml;charset=utf-8," + encodeURIComponent(compose)
                }
                download={name + ".yml"}
              >
                Download compose
              </a>
            </Button>
            {!stored && me && me.role !== "viewer" && (
              <Action
                label="Save stackfile"
                path={path + "/file"}
                body={{ name, spec: { compose } }}
              />
            )}
          </div>
        </>
      )}
    </Section>
  );
}
function StackHistory({ name, editable }: { name: string; editable: boolean }) {
  const history = useApi<History[]>(
    "/api/stacks/" + segment(name) + "/history",
  );
  const [selected, setSelected] = useState<number | null>(null);
  const entry = selected === null ? undefined : history.data?.[selected];
  return (
    <Section title="Compose history">
      <ErrorMessage error={history.error} />
      {history.isPending ? (
        <Loading />
      ) : !history.data?.length ? (
        <Empty>No compose changes have been recorded.</Empty>
      ) : (
        <div className="space-y-3">
          {history.data
            .map((item, index) => ({ item, index }))
            .reverse()
            .map(({ item, index }) => (
              <Button
                key={index}
                variant={selected === index ? "secondary" : "outline"}
                className="h-auto w-full justify-start whitespace-normal py-3 text-left"
                onClick={() => setSelected(index)}
              >
                {historyLabel(item)}
              </Button>
            ))}
        </div>
      )}
      {entry && (
        <div className="space-y-4">
          <Properties
            values={{
              Time: date(entry.at),
              By: entry.by || "system",
              Comment: entry.comment || "—",
              Trigger: entry.trigger?.kind,
              Service: entry.trigger?.service,
              Digest: entry.trigger?.digest,
            }}
          />
          <TextField
            label="Historical compose"
            value={entry.spec.compose}
            readOnly
            rows={20}
          />
          <Button asChild variant="outline">
            <Link
              to={"/stacks/" + segment(name) + "/compose?history=" + selected}
            >
              {editable ? "Edit this version" : "View this version"}
            </Link>
          </Button>
        </div>
      )}
    </Section>
  );
}
