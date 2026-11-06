import { Skeleton } from "@/components/ui/skeleton";
import { NodeCardsLoading } from "@/features/resources/node-cards";

export function WorkspaceLoading({ pathname, statistics }: { pathname: string; statistics?: boolean }) {
  const segments = pathname.split("/").filter(Boolean);
  const overview = pathname === "/";
  const form = segments[1] === "create" || segments[2] === "edit" ||
    (segments[0] === "stacks" && ["compose", "previous", "last", "activate"].includes(segments[2] || "")) ||
    pathname === "/account-settings";
  const list = !overview && segments.length === 1 && !form;
  return (
    <div role="status" aria-label="Workspace loading" aria-busy="true" className="space-y-6">
      <span className="sr-only">Loading workspace…</span>
      <div aria-hidden="true" className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-2">
            <Skeleton className="h-8 w-40" />
            <Skeleton className="h-5 w-64 max-w-full" />
          </div>
          <Skeleton className={overview ? "h-7 w-16" : "h-9 w-28"} />
        </div>
        {overview ? (
          <>
            <div data-slot="loading-overview-summary" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <div className="min-w-0 rounded-xl border bg-card p-4">
                <Skeleton className="h-5 w-12" />
                <Skeleton className="my-2 h-9 w-28" />
                <div className="flex min-h-7 flex-wrap gap-1.5">
                  <Skeleton className="h-7 w-28" />
                  <Skeleton className="h-7 w-28" />
                </div>
                <Skeleton className="mt-2 h-7 w-28" />
              </div>
              {[0, 1, 2].map((card) => (
                <div key={card} className="min-w-0 rounded-xl border bg-card p-4">
                  <Skeleton className="h-5 w-16" />
                  <div className="mt-3 flex min-w-0 items-center gap-3">
                    <Skeleton className="size-16 shrink-0 rounded-full" />
                    <div className="min-w-0 flex-1 space-y-2">
                      <Skeleton className="h-4 w-3/4" />
                      <Skeleton className="h-3 w-full" />
                      <Skeleton className="h-3 w-12" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
            {statistics !== false && (
              <div className="grid gap-4 lg:grid-cols-2">
                {[0, 1].map((chart) => (
                  <div key={chart} className="space-y-4 rounded-xl border bg-card p-6">
                    <Skeleton className="h-6 w-48 max-w-full" />
                    <div className="flex gap-2"><Skeleton className="h-9 w-32" /><Skeleton className="h-9 w-16" /></div>
                    <Skeleton className="h-64 w-full" />
                    <Skeleton className="mx-auto h-4 w-32" />
                  </div>
                ))}
              </div>
            )}
          </>
        ) : pathname === "/nodes" ? (
          <div className="space-y-6">
            <div className="flex flex-wrap items-end gap-3">
              <Skeleton className="h-9 w-80 max-w-full" />
              <div className="w-40 space-y-2"><Skeleton className="h-3.5 w-12" /><Skeleton className="h-9 w-full" /></div>
            </div>
            <NodeCardsLoading />
          </div>
        ) : list ? (
          <div className="space-y-4">
            <Skeleton className="h-9 w-64 max-w-full" />
            <div className="overflow-hidden rounded-xl border bg-card">
              {Array.from({ length: 7 }, (_, row) => (
                <div key={row} className="grid grid-cols-4 gap-4 border-b px-4 py-4 last:border-b-0">
                  {Array.from({ length: 4 }, (_, column) => <Skeleton key={column} className="h-4 w-full" />)}
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className={form ? "max-w-3xl space-y-5" : "space-y-6"}>
            <div className="space-y-4 rounded-xl border bg-card p-5">
              <Skeleton className="h-5 w-32" />
              <div className="grid gap-4 sm:grid-cols-2">
                {Array.from({ length: form ? 4 : 6 }, (_, field) => (
                  <div key={field} className="space-y-2">
                    <Skeleton className="h-3.5 w-20" />
                    <Skeleton className={form ? "h-9 w-full" : "h-5 w-3/4"} />
                  </div>
                ))}
              </div>
              {form && <Skeleton className="h-40 w-full" />}
            </div>
            {form ? <Skeleton className="h-9 w-28" /> : <Skeleton className="h-48 w-full" />}
          </div>
        )}
      </div>
    </div>
  );
}
