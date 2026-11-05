import type { ReactNode } from "react";
import { Link } from "react-router";
import { useApi } from "@/lib/queries";
import { useLive } from "@/lib/live";
import { Page, Loading, ErrorMessage } from "@/components/common";
import { Button } from "@/components/ui/button";
import { Plus, Radio } from "lucide-react";
import { useSession } from "@/features/session/session";
import {
  ServicesTable,
  NodesTable,
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
  return (
    <span
      role="status"
      className="flex items-center gap-1.5 text-xs text-muted-foreground"
    >
      <Radio
        className={
          "size-3 " +
          (status === "live" ? "text-emerald-500" : "text-amber-500")
        }
      />
      {status === "live"
        ? "Live"
        : status === "reconnecting"
          ? "Reconnecting…"
          : "Connecting…"}
    </span>
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
                <Plus className="size-4" />
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
      {query.isPending ? <Loading /> : query.data ? children(query.data) : null}
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
export const NodesList = () => (
  <List<Node> title="Nodes" path="/api/nodes" handler="node-list">
    {(rows) => <NodesTable rows={rows} />}
  </List>
);
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
