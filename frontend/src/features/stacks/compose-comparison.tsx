import { useMemo } from "react";
import { ArrowTurnBackwardIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Button } from "@/components/ui/button";
import { CodeField } from "@/components/code-field";
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
                <HugeiconsIcon icon={ArrowTurnBackwardIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                Restore engine lines at {change.start + 1}
              </Button>
            )}
          </div>
          {change.before.length > 0 && (
            <div className="border-l-2 border-destructive bg-destructive/5 p-3">
              <CodeField
                label="Engine lines removed or replaced"
                value={change.before.join("\n")}
                readOnly
                rows={Math.min(12, Math.max(2, change.before.length))}
                showSyntaxSelector={false}
              />
            </div>
          )}
          {change.after.length > 0 && (
            <div className="border-l-2 border-success bg-success/5 p-3">
              <CodeField
                label="Draft lines added or changed"
                value={change.after.join("\n")}
                readOnly
                rows={Math.min(12, Math.max(2, change.after.length))}
                showSyntaxSelector={false}
              />
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
