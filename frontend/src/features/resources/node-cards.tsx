import { ComputerIcon, HierarchySquare01Icon, ServerStack01Icon, TerminalIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { ResourceLink, Status } from "@/components/common";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { segment } from "@/lib/api";
import { bytes } from "@/lib/format";
import type { Node } from "@/lib/types";
import { ResourceUsageChart } from "@/features/dashboard/chart";

const cores = (value: number) => value.toLocaleString(undefined, { maximumFractionDigits: 2 }) + " vCPU";
const storage = (value: number) => bytes(value, false);

export function NodeResourceCharts({ node }: { node: Node }) {
  const stats = node.state === "ready" ? node.stats : null;
  const cpu = stats?.cpu;
  const cpuUsed = cpu?.cores != null && cpu.usedPercentage != null
    ? cpu.cores * cpu.usedPercentage / 100 : undefined;
  const resources = [
    { label: "CPU", used: cpuUsed, total: cpu?.cores, usage: cpu?.usedPercentage, format: cores },
    { label: "Memory", used: stats?.memory?.used, total: stats?.memory?.total, usage: stats?.memory?.usedPercentage, format: bytes },
    { label: "Storage", used: stats?.disk?.used, total: stats?.disk?.total, usage: stats?.disk?.usedPercentage, format: storage },
  ];
  return (
    stats ? (
      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        {resources.map(({ label, ...resource }) => (
          <div key={label} className="min-w-0">
            <h4 className="text-xs font-medium text-muted-foreground">{label}</h4>
            <ResourceUsageChart title={node.nodeName + " " + label} {...resource} color="var(--primary)" />
          </div>
        ))}
      </div>
    ) : <p className="mt-4 text-sm text-muted-foreground">Statistics unavailable.</p>
  );
}

export function NodeCardsLoading() {
  return (
    <div role="status" aria-label="Nodes loading" aria-busy="true">
      <span className="sr-only">Loading nodes…</span>
      <div aria-hidden="true" className="grid gap-4 2xl:grid-cols-2">
        {[0, 1].map((card) => (
          <div key={card} className="min-w-0 rounded-xl border bg-card p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-2">
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-3 w-24" />
              </div>
              <Skeleton className="h-4 w-12" />
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <Skeleton className="h-6 w-16" />
              <Skeleton className="h-6 w-20" />
              <Skeleton className="h-6 w-16" />
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-3">
              {[0, 1, 2].map((resource) => (
                <div key={resource} className="min-w-0 space-y-3">
                  <Skeleton className="h-4 w-16" />
                  <div className="flex min-h-20 items-center gap-3">
                    <Skeleton className="size-16 shrink-0 rounded-full" />
                    <div className="min-w-0 flex-1 space-y-2">
                      <Skeleton className="h-4 w-3/4" />
                      <Skeleton className="h-3 w-full" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

export function NodeCards({ rows }: { rows: Node[] }) {
  return (
      <div className="grid gap-4 2xl:grid-cols-2">
        {[...rows].sort((a, b) => a.nodeName.localeCompare(b.nodeName)).map((node) => {
          const osIcon = node.os === "linux" ? TerminalIcon : node.os === "windows" ? ComputerIcon : ServerStack01Icon;
          return (
            <article key={node.id} aria-label={node.nodeName} className="min-w-0 rounded-xl border bg-card p-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="min-w-0">
                  <h3 className="break-words font-semibold"><ResourceLink to={"/nodes/" + segment(node.id)}>{node.nodeName}</ResourceLink></h3>
                  <p className="mt-1 break-all text-xs text-muted-foreground">{node.address}</p>
                </div>
                <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                  <HugeiconsIcon icon={osIcon} strokeWidth={2} aria-hidden="true" className="size-4" />{node.os || "Unknown OS"}
                </span>
              </div>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <Status value={node.state} />
                <Badge variant="secondary">
                  <HugeiconsIcon icon={node.role === "manager" ? HierarchySquare01Icon : node.role === "worker" ? ServerStack01Icon : ComputerIcon} strokeWidth={2} aria-hidden="true" className="size-3 shrink-0" />
                  {node.role}
                </Badge>
                {node.leader && <Status value="leader" />}
                <Status value={node.availability} />
                <span className="text-xs text-muted-foreground">Docker {node.engine}</span>
              </div>
              <NodeResourceCharts node={node} />
            </article>
          );
        })}
      </div>
  );
}
