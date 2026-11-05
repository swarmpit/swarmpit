import { useMemo } from "react";
import { Button } from "@/components/ui/button";
import { composeChanges, restoreCompose } from "./compose";

export function ComposeComparison({
  baseline,
  current,
  onRestore,
}: {
  baseline: string;
  current: string;
  onRestore?: (compose: string) => void;
}) {
  const changes = useMemo(
    () => composeChanges(baseline, current),
    [baseline, current],
  );
  if (!changes.length)
    return (
      <p className="text-sm text-muted-foreground">
        Compose matches the current engine state.
      </p>
    );
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted-foreground">
        Changes from engine state to this draft. Restore a block to keep its
        engine values without discarding other edits.
      </p>
      {changes.map((change, index) => (
        <div key={index} className="overflow-hidden rounded-lg border">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b bg-muted/30 px-3 py-2">
            <span className="text-xs">Draft line {change.start + 1}</span>
            {onRestore && (
              <Button
                type="button"
                size="sm"
                variant="outline"
                onClick={() => onRestore(restoreCompose(current, change))}
              >
                Restore engine lines at {change.start + 1}
              </Button>
            )}
          </div>
          {change.before.length > 0 && (
            <pre
              className="overflow-x-auto bg-destructive/5 p-3 text-xs leading-5 text-destructive"
              aria-label="Engine lines removed or replaced"
            >
              {change.before.map((line) => "− " + line).join("\n")}
            </pre>
          )}
          {change.after.length > 0 && (
            <pre
              className="overflow-x-auto bg-emerald-500/5 p-3 text-xs leading-5 text-emerald-700 dark:text-emerald-400"
              aria-label="Draft lines added or changed"
            >
              {change.after.map((line) => "+ " + line).join("\n")}
            </pre>
          )}
        </div>
      ))}
    </div>
  );
}
