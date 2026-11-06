import { useState, type ReactNode } from "react";
import { Link } from "react-router";
import { useApi } from "@/lib/queries";
import { useLive } from "@/lib/live";
import { Page, Loading, ErrorMessage, Choice, Empty } from "@/components/common";
import { Input } from "@/components/ui/input";
import { NodeCards, NodeCardsLoading } from "./node-cards";
import { Button } from "@/components/ui/button";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, LiveStreaming02Icon, Loading03Icon, Search01Icon, WifiOff01Icon } from "@hugeicons/core-free-icons";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { useSession } from "@/features/session/session";
import {
  ServicesTable,
  TasksTable,
  StacksTable,
  NetworksTable,
  VolumesTable,
  SecretsTable,
  ConfigsTable,
  UsersTable,
} from "./tables";
import type {
  Service,
  Node,
  Task,
  Stack,
  Network,
  Volume,
  Secret,
  Config,
  User,
} from "@/lib/types";
export function Live({
  handler,
  params,
}: {
  handler: string;
  params?: Record<string, string>;
}) {
  const status = useLive(handler, params);
  const connected = status === "live";
  const label = connected ? "Live" : status === "reconnecting" ? "Reconnecting…" : "Connecting…";
  return (
    <Badge
      variant="outline"
      role="status"
      aria-label={connected ? "Live updates connected" : "Live updates " + status}
      aria-live="polite"
      aria-atomic="true"
      title={connected ? "Changes appear automatically" : "Waiting for the live connection"}
      className={cn(
        "h-7 gap-1.5 rounded-md px-2 text-xs font-medium",
        connected
          ? "border-success/20 bg-success/5 text-success-foreground"
          : "border-warning/20 bg-warning/5 text-warning-foreground",
      )}
    >
      <span aria-hidden="true" className="flex size-3.5 shrink-0 items-center justify-center">
        {connected ? (
          <HugeiconsIcon icon={LiveStreaming02Icon} strokeWidth={2} aria-hidden="true" className="size-3.5" />
        ) : status === "reconnecting" ? (
          <HugeiconsIcon icon={WifiOff01Icon} strokeWidth={2} aria-hidden="true" className="size-3.5" />
        ) : (
          <HugeiconsIcon icon={Loading03Icon} strokeWidth={2} aria-hidden="true" className="size-3.5 animate-spin motion-reduce:animate-none" />
        )}
      </span>
      <span>{label}</span>
    </Badge>
  );
}
function List<T>({
  title,
  path,
  create,
  handler,
  children,
}: {
  title: string;
  path: string;
  create?: string;
  handler?: string;
  children: (rows: T[]) => ReactNode;
}) {
  const query = useApi<T[]>(path);
  const { me } = useSession();
  const canCreate =
    me?.role !== "viewer" && (title !== "Users" || me?.role === "admin");
  return (
    <Page
      title={title}
      description={
        query.data
          ? query.data.length + " " + title.toLowerCase() + " in your swarm"
          : "Manage your swarm resources"
      }
      actions={
        <>
          {handler && <Live handler={handler} />}
          {create && canCreate && (
            <Button asChild>
              <Link to={create}>
                <HugeiconsIcon icon={Add01Icon} strokeWidth={2} aria-hidden="true" className="size-4" />
                Create{" "}
                {title === "Services"
                  ? "service"
                  : title.slice(0, -1).toLowerCase()}
              </Link>
            </Button>
          )}
        </>
      }
    >
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading variant="table" />
      ) : query.data ? (
        children(query.data)
      ) : null}
    </Page>
  );
}
export const ServicesList = () => (
  <List<Service>
    title="Services"
    path="/api/services"
    create="/services/create/wizard/image"
    handler="service-list"
  >
    {(rows) => <ServicesTable rows={rows} />}
  </List>
);
export function NodesList() {
  const query = useApi<Node[]>("/api/nodes");
  const [search, setSearch] = useState("");
  const [role, setRole] = useState("");
  const filtered = (query.data || []).filter((node) =>
    (!role || node.role === role) &&
    [node.nodeName, node.role, node.address, node.state, node.availability, node.os, node.engine]
      .join(" ").toLowerCase().includes(search.toLowerCase()),
  );
  return (
    <Page title="Nodes"
      description={query.data ? query.data.length + " nodes in your swarm" : "Manage your swarm resources"}
      actions={<Live handler="node-list" />}>
      <ErrorMessage error={query.error} />
      <div className="flex flex-wrap items-end gap-3">
        <div className="relative w-full max-w-sm">
          <HugeiconsIcon icon={Search01Icon} strokeWidth={2} aria-hidden="true" className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input aria-label="Filter nodes" placeholder="Filter nodes…" value={search}
            onChange={(event) => setSearch(event.target.value)} className="pl-9" />
        </div>
        <div className="w-40">
          <Choice label="Role" value={role} onChange={setRole} options={["", "manager", "worker"]} />
        </div>
      </div>
      {query.isPending ? <NodeCardsLoading /> : query.data ? (
        filtered.length ? (
          <section aria-label="Node resources">
            <h2 className="sr-only">Node resources</h2>
            <NodeCards rows={filtered} />
          </section>
        ) : <Empty>{query.data.length ? "No matching nodes." : "No nodes found."}</Empty>
      ) : null}
    </Page>
  );
}
export const TasksList = () => (
  <List<Task> title="Tasks" path="/api/tasks" handler="task-list">
    {(rows) => <TasksTable rows={rows} />}
  </List>
);
export const StacksList = () => (
  <List<Stack>
    title="Stacks"
    path="/api/stacks"
    create="/stacks/create"
    handler="stack-list"
  >
    {(rows) => <StacksTable rows={rows} />}
  </List>
);
export const NetworksList = () => (
  <List<Network>
    title="Networks"
    path="/api/networks"
    create="/networks/create"
  >
    {(rows) => <NetworksTable rows={rows} />}
  </List>
);
export const VolumesList = () => (
  <List<Volume> title="Volumes" path="/api/volumes" create="/volumes/create">
    {(rows) => <VolumesTable rows={rows} />}
  </List>
);
export const SecretsList = () => (
  <List<Secret> title="Secrets" path="/api/secrets" create="/secrets/create">
    {(rows) => <SecretsTable rows={rows} />}
  </List>
);
export const ConfigsList = () => (
  <List<Config> title="Configs" path="/api/configs" create="/configs/create">
    {(rows) => <ConfigsTable rows={rows} />}
  </List>
);
export const UsersList = () => (
  <List<User> title="Users" path="/api/admin/users" create="/users/create">
    {(rows) => <UsersTable rows={rows} />}
  </List>
);
