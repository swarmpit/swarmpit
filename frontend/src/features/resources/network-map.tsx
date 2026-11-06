import { useId, useState } from "react";
import { Link } from "react-router";
import { ArrowUpRight01Icon, CubeIcon, Cursor01Icon, NetworkIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Badge } from "@/components/ui/badge";
import { segment } from "@/lib/api";
import type { Network, Service } from "@/lib/types";

export function NetworkMap({ network, services }: { network: Network; services: Service[] }) {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const detailsId = useId();
  const selected = services.find((service) => service.id === selectedId);
  const attachment = selected?.networks?.find((item) =>
    item.id === network.id || item.networkName === network.networkName,
  );
  const rows = Math.ceil(services.length / 2);
  const height = Math.max(360, rows * 116 + 72);
  const centerY = height / 2;
  const nodes = services.map((service, index) => ({
    service,
    x: index % 2 === 0 ? 36 : 624,
    y: centerY - ((rows - 1) * 116) / 2 + Math.floor(index / 2) * 116,
  }));

  return (
    <section aria-label="Network topology" className="min-w-0 space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <HugeiconsIcon icon={Cursor01Icon} strokeWidth={2} aria-hidden="true" className="size-4" />
          Select a service to inspect its network attachment.
        </p>
        <Badge variant="secondary">
          <HugeiconsIcon icon={CubeIcon} strokeWidth={2} aria-hidden="true" className="size-3 shrink-0" />
          {services.length} connected {services.length === 1 ? "service" : "services"}
        </Badge>
      </div>
      <div
        role="region"
        aria-label="Scrollable topology map"
        tabIndex={0}
        className="max-h-[520px] max-w-full overflow-auto rounded-xl border bg-muted/30"
      >
        <div className="relative isolate mx-auto w-[880px]" style={{ height }}>
          <svg width="880" height={height} aria-hidden="true" className="pointer-events-none absolute inset-0">
            <circle cx="440" cy={centerY} r="136" fill="none" stroke="var(--primary)" strokeOpacity=".08" strokeWidth="32" />
            <circle cx="440" cy={centerY} r="112" fill="none" stroke="var(--primary)" strokeOpacity=".2" strokeDasharray="4 8" />
            {nodes.map(({ service, x, y }, index) => {
              const active = selected?.id === service.id;
              const left = index % 2 === 0;
              const endX = left ? x + 220 : x;
              const startX = left ? 330 : 550;
              return (
                <g key={service.id}>
                  <path
                    d={`M ${startX} ${centerY} C ${left ? 290 : 590} ${centerY}, ${left ? 290 : 590} ${y}, ${endX} ${y}`}
                    fill="none"
                    stroke={active ? "var(--primary)" : "var(--secondary)"}
                    strokeWidth={active ? 3 : 1.5}
                    opacity={selected && !active ? 0.25 : 0.8}
                  />
                  <circle cx={endX} cy={y} r={active ? 5 : 3} fill={active ? "var(--primary)" : "var(--secondary)"} />
                </g>
              );
            })}
          </svg>
          <div
            className="absolute left-[330px] z-10 flex w-[220px] -translate-y-1/2 flex-col items-center rounded-2xl border-2 border-primary/60 bg-card p-5 text-center shadow-lg shadow-primary/10"
            style={{ top: centerY }}
          >
            <span className="mb-3 flex size-12 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <HugeiconsIcon icon={NetworkIcon} strokeWidth={2} aria-hidden="true" className="size-6" />
            </span>
            <span className="text-[10px] font-semibold tracking-[0.18em] text-muted-foreground uppercase">Network</span>
            <span className="mt-1 max-w-full truncate font-semibold" title={network.networkName}>{network.networkName}</span>
            <span className="mt-2 text-xs text-muted-foreground">{network.driver}{network.scope ? " · " + network.scope : ""}</span>
            {network.ipam?.subnet && <span className="mt-2 font-mono text-xs text-primary">{network.ipam.subnet}</span>}
          </div>
          {nodes.map(({ service, x, y }) => {
            const active = selected?.id === service.id;
            return (
              <button
                key={service.id}
                type="button"
                aria-label={"Inspect " + service.serviceName}
                aria-pressed={active}
                aria-controls={detailsId}
                onClick={() => setSelectedId(active ? null : service.id)}
                className={`absolute z-10 flex h-[88px] w-[220px] -translate-y-1/2 items-center gap-3 rounded-xl border bg-card px-4 text-left shadow-sm transition-colors motion-reduce:transition-none ${active ? "border-primary ring-2 ring-primary/20" : "border-border hover:border-primary/60 hover:bg-accent"}`}
                style={{ left: x, top: y }}
              >
                <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-secondary/15 text-foreground">
                  <HugeiconsIcon icon={CubeIcon} strokeWidth={2} aria-hidden="true" className="size-5" />
                </span>
                <span className="grid min-w-0 gap-1">
                  <span className="truncate text-sm font-semibold" title={service.serviceName}>{service.serviceName}</span>
                  <span className="truncate text-xs text-muted-foreground">{service.status?.tasks
                    ? `${service.status.tasks.running} / ${service.status.tasks.total} tasks running`
                    : service.mode}</span>
                </span>
              </button>
            );
          })}
          {services.length === 0 && (
            <p className="absolute inset-x-4 bottom-6 text-center text-sm text-muted-foreground">
              No services are connected to this network.
            </p>
          )}
        </div>
      </div>
      <div id={detailsId} aria-live="polite" aria-atomic="true">
        {selected ? (
          <section aria-label="Selected service" className="flex flex-wrap items-start justify-between gap-4 rounded-xl border border-primary/25 bg-primary/5 p-4">
            <div className="min-w-0 space-y-3">
              <p className="break-all font-semibold">{selected.serviceName}</p>
              <dl className="flex flex-wrap gap-x-8 gap-y-3 text-sm">
                <div>
                  <dt className="text-xs text-muted-foreground">Service aliases</dt>
                  <dd className="mt-1 break-all">{attachment?.serviceAliases?.length
                    ? attachment.serviceAliases.join(", ")
                    : "No service aliases reported"}</dd>
                </div>
                {selected.status?.tasks && (
                  <div>
                    <dt className="text-xs text-muted-foreground">Running tasks</dt>
                    <dd className="mt-1">{selected.status.tasks.running} / {selected.status.tasks.total}</dd>
                  </div>
                )}
                {network.ipam?.gateway && (
                  <div>
                    <dt className="text-xs text-muted-foreground">Network gateway</dt>
                    <dd className="mt-1 font-mono">{network.ipam.gateway}</dd>
                  </div>
                )}
              </dl>
            </div>
            <Link to={"/services/" + segment(selected.id)} className="inline-flex items-center gap-1 text-sm font-medium text-primary underline underline-offset-4">
              Open service <HugeiconsIcon icon={ArrowUpRight01Icon} strokeWidth={2} aria-hidden="true" className="size-4" />
            </Link>
          </section>
        ) : (
          <p className="text-xs text-muted-foreground">Each connection represents a service attached to {network.networkName}.</p>
        )}
      </div>
    </section>
  );
}
