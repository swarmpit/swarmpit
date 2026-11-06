import { ResourceTable, ResourceLink, Status } from "@/components/common";
import { bytes, date } from "@/lib/format";
import { segment } from "@/lib/api";
import { serviceDisplayState } from "@/features/services/status";
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
const link = (kind: string, id: string, name: string) => (
  <ResourceLink to={"/" + kind + "/" + segment(id)}>{name}</ResourceLink>
);
export function ServicesTable({ rows }: { rows: Service[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(s) => s.serviceName}
      search={(s) =>
        [
          s.serviceName,
          s.repository?.image,
          s.repository?.name,
          s.stack,
          serviceDisplayState(s),
          ...(s.ports || []).map((port) => port.hostPort + " [" + port.protocol + "]"),
        ].join(" ")
      }
      columns={[
        {
          label: "Service",
          render: (s) => link("services", s.id, s.serviceName),
        },
        {
          label: "Image",
          render: (s) => (
            <span className="font-mono text-xs">
              {s.repository?.image ||
                s.repository?.name + ":" + s.repository?.tag}
            </span>
          ),
        },
        { label: "State", render: (s) => <Status value={serviceDisplayState(s)} /> },
        {
          label: "Tasks",
          render: (s) =>
            s.status?.tasks
              ? s.status.tasks.running + " / " + s.status.tasks.total
              : "—",
        },
        {
          label: "Ports",
          render: (s) => s.ports?.length ? (
            <div className="space-y-1 font-mono text-xs">
              {s.ports.map((port, index) => <div key={index}>{port.hostPort} [{port.protocol}]</div>)}
            </div>
          ) : "—",
        },
        { label: "Mode", render: (s) => s.mode },
        {
          label: "Stack",
          render: (s) => (s.stack ? link("stacks", s.stack, s.stack) : "—"),
        },
      ]}
    />
  );
}
export function NodesTable({ rows }: { rows: Node[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(n) => n.nodeName}
      search={(n) => [n.nodeName, n.role, n.address, n.state].join(" ")}
      columns={[
        { label: "Node", render: (n) => link("nodes", n.id, n.nodeName) },
        { label: "Status", render: (n) => <Status value={n.state} /> },
        {
          label: "Role",
          render: (n) => n.role + (n.leader ? " · leader" : ""),
        },
        {
          label: "Availability",
          render: (n) => <Status value={n.availability} />,
        },
        { label: "Address", render: (n) => n.address },
        { label: "Engine", render: (n) => n.engine },
      ]}
    />
  );
}
export function TasksTable({ rows }: { rows: Task[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(t) => t.taskName || t.id.slice(0, 12)}
      search={(t) => [t.taskName, t.serviceName, t.nodeName, t.state].join(" ")}
      columns={[
        {
          label: "Task",
          render: (t) => link("tasks", t.id, t.taskName || t.id.slice(0, 12)),
        },
        {
          label: "Service",
          render: (t) => link("services", t.serviceName, t.serviceName),
        },
        {
          label: "Node",
          render: (t) => (t.nodeId ? link("nodes", t.nodeId, t.nodeName) : "—"),
        },
        {
          label: "CPU Usage",
          render: (t) => t.stats ? (
            <div className="space-y-1 tabular-nums">
              <div>{t.stats.cpuPercentage == null ? "—" : t.stats.cpuPercentage.toFixed(2) + "%"}</div>
              <div className="text-xs text-muted-foreground">{t.stats.cpu == null ? "—" : t.stats.cpu.toFixed(2) + " vCPU"}</div>
            </div>
          ) : "—",
        },
        {
          label: "Memory Usage",
          render: (t) => t.stats ? (
            <div className="space-y-1 tabular-nums">
              <div>{t.stats.memoryPercentage == null ? "—" : t.stats.memoryPercentage.toFixed(2) + "%"}</div>
              <div className="text-xs text-muted-foreground">{bytes(t.stats.memory)}</div>
            </div>
          ) : "—",
        },
        { label: "Last update", render: (t) => date(t.updatedAt) },
        { label: "Desired state", render: (t) => t.desiredState },
        {
          label: "Error",
          render: (t) => (
            <span className="text-destructive">{t.status?.error}</span>
          ),
        },
        { label: "State", render: (t) => <Status value={t.state} /> },
      ]}
    />
  );
}
export function StacksTable({ rows }: { rows: Stack[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(s) => s.stackName}
      search={(s) => s.stackName}
      columns={[
        {
          label: "Stack",
          render: (s) => link("stacks", s.stackName, s.stackName),
        },
        {
          label: "State",
          render: (s) => (
            <Status
              value={s.state || (s.services?.length ? "deployed" : "inactive")}
            />
          ),
        },
        { label: "Services", render: (s) => s.services?.length || 0 },
        { label: "Networks", render: (s) => s.networks?.length || 0 },
        { label: "Updated", render: (s) => date(s.updatedAt) },
      ]}
    />
  );
}
export function NetworksTable({ rows }: { rows: Network[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(n) => n.networkName}
      search={(n) => [n.networkName, n.driver, n.scope].join(" ")}
      columns={[
        {
          label: "Network",
          render: (n) => link("networks", n.id, n.networkName),
        },
        { label: "Driver", render: (n) => n.driver },
        { label: "Scope", render: (n) => n.scope },
        { label: "Subnet", render: (n) => n.ipam?.subnet || "—" },
        { label: "Attachable", render: (n) => (n.attachable ? "Yes" : "No") },
      ]}
    />
  );
}
export function VolumesTable({ rows }: { rows: Volume[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(v) => v.volumeName}
      search={(v) => [v.volumeName, v.driver].join(" ")}
      columns={[
        {
          label: "Volume",
          render: (v) => link("volumes", v.volumeName, v.volumeName),
        },
        { label: "Driver", render: (v) => v.driver },
        { label: "Scope", render: (v) => v.scope },
        { label: "Size", render: (v) => bytes(v.size) },
        {
          label: "Mountpoint",
          render: (v) => (
            <span className="font-mono text-xs">{v.mountpoint}</span>
          ),
        },
      ]}
    />
  );
}
export function SecretsTable({ rows }: { rows: Secret[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(s) => s.secretName}
      search={(s) => s.secretName}
      columns={[
        { label: "Secret", render: (s) => link("secrets", s.id, s.secretName) },
        { label: "Created", render: (s) => date(s.createdAt) },
        { label: "Updated", render: (s) => date(s.updatedAt) },
      ]}
    />
  );
}
export function ConfigsTable({ rows }: { rows: Config[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(s) => s.configName}
      search={(s) => s.configName}
      columns={[
        { label: "Config", render: (s) => link("configs", s.id, s.configName) },
        { label: "Created", render: (s) => date(s.createdAt) },
        { label: "Updated", render: (s) => date(s.updatedAt) },
      ]}
    />
  );
}
export function UsersTable({ rows }: { rows: User[] }) {
  return (
    <ResourceTable
      rows={rows}
      sortBy={(u) => u.username}
      search={(u) => [u.username, u.email, u.role].join(" ")}
      columns={[
        { label: "User", render: (u) => link("users", u._id, u.username) },
        { label: "Role", render: (u) => u.role },
        { label: "Email", render: (u) => u.email || "—" },
      ]}
    />
  );
}
