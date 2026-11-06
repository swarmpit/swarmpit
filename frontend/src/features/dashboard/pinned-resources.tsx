import { CubeIcon, GlobeIcon, Layers01Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { ResourceLink, Status } from "@/components/common";
import { Badge } from "@/components/ui/badge";
import { segment } from "@/lib/api";
import type { Node, Service } from "@/lib/types";
import { NodeCards } from "@/features/resources/node-cards";
import { serviceDisplayState } from "@/features/services/status";

export function PinnedNodes({ rows }: { rows: Node[] }) {
  if (!rows.length) return null;
  return (
    <section aria-label="Pinned nodes" className="space-y-3">
      <h2 className="text-sm font-medium text-muted-foreground">Pinned nodes</h2>
      <NodeCards rows={rows} />
    </section>
  );
}

export function PinnedServices({ rows }: { rows: Service[] }) {
  if (!rows.length) return null;
  return (
    <section aria-label="Pinned services" className="space-y-3">
      <h2 className="text-sm font-medium text-muted-foreground">Pinned services</h2>
      <div className="grid gap-3 lg:grid-cols-2">
        {[...rows].sort((a, b) => a.serviceName.localeCompare(b.serviceName)).map((service) => {
          const state = serviceDisplayState(service);
          const repository = service.repository;
          const image = repository?.image || (repository?.name ? repository.name + (repository.tag ? ":" + repository.tag : "") : "—");
          const digest = repository?.imageDigest;
          const tasks = service.status?.tasks;
          return (
            <article key={service.id} aria-label={service.serviceName} className="min-w-0 rounded-xl border bg-card p-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="min-w-0 break-words font-semibold"><ResourceLink to={"/services/" + segment(service.serviceName)}>{service.serviceName}</ResourceLink></h3>
                <Status value={state} />
              </div>
              <p className="mt-2 break-all font-mono text-xs text-muted-foreground">{image}</p>
              {digest && !image.includes(digest) && <p className="mt-1 break-all font-mono text-xs text-muted-foreground">{digest}</p>}
              <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
                <span>{tasks ? tasks.running + " / " + tasks.total + " tasks" : "Tasks unavailable"}</span>
                <Badge variant="secondary">
                  <HugeiconsIcon icon={service.mode === "global" ? GlobeIcon : service.mode === "replicated" ? Layers01Icon : CubeIcon} strokeWidth={2} aria-hidden="true" className="size-3 shrink-0" />
                  {service.mode}
                </Badge>
                <div className="flex flex-wrap gap-x-3 gap-y-1" aria-label="Published ports">
                  {service.ports?.length ? service.ports.map((port, index) => (
                    <span key={index} className="font-mono text-xs">{port.hostPort} [{port.protocol}]</span>
                  )) : <span className="text-xs text-muted-foreground">No published ports</span>}
                </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}
