import { ApiError } from "@/lib/api";
import type { History } from "@/lib/types";

export function missingStackfile(error: unknown) {
  return (
    error instanceof ApiError &&
    error.status === 400 &&
    error.message === "Stackfile not found"
  );
}

const triggers: Record<string, string> = {
  "stack-create": "Created",
  "stack-update": "Updated",
  "stack-redeploy": "Redeployed",
  "stack-rollback": "Rolled back",
  "service-update": "Service updated",
  "service-redeploy": "Service redeployed",
  "service-rollback": "Service rolled back",
  "service-scale": "Service scaled",
  "auto-redeploy": "Auto-redeploy",
};
export function historyLabel(entry: History) {
  const action =
    triggers[entry.trigger?.kind || ""] || entry.trigger?.kind || "Change";
  return [
    entry.comment || action,
    entry.trigger?.service,
    entry.at,
    entry.by || "system",
  ]
    .filter(Boolean)
    .join(" · ");
}

export interface ComposeChange {
  start: number;
  end: number;
  before: string[];
  after: string[];
}
export function composeChanges(
  baseline: string,
  current: string,
): ComposeChange[] {
  const before = baseline.split("\n");
  const after = current.split("\n");
  let prefix = 0;
  while (
    prefix < before.length &&
    prefix < after.length &&
    before[prefix] === after[prefix]
  )
    prefix++;
  if (prefix === before.length && prefix === after.length) return [];
  let suffix = 0;
  while (
    suffix < before.length - prefix &&
    suffix < after.length - prefix &&
    before[before.length - 1 - suffix] === after[after.length - 1 - suffix]
  )
    suffix++;
  const a = before.slice(prefix, before.length - suffix);
  const b = after.slice(prefix, after.length - suffix);
  // Bound comparison memory for unusually large compose files; the whole changed block remains restorable.
  if (a.length * b.length > 4_000_000)
    return [{ start: prefix, end: prefix + b.length, before: a, after: b }];
  const rows = Array.from(
    { length: a.length + 1 },
    () => new Uint32Array(b.length + 1),
  );
  for (let i = 1; i <= a.length; i++) {
    for (let j = 1; j <= b.length; j++) {
      rows[i]![j] =
        a[i - 1] === b[j - 1]
          ? rows[i - 1]![j - 1]! + 1
          : Math.max(rows[i - 1]![j]!, rows[i]![j - 1]!);
    }
  }
  const operations: { kind: "keep" | "delete" | "add"; text: string }[] = [];
  let i = a.length;
  let j = b.length;
  while (i || j) {
    if (i && j && a[i - 1] === b[j - 1]) {
      operations.push({ kind: "keep", text: a[--i]! });
      j--;
    } else if (i && (!j || rows[i - 1]![j]! >= rows[i]![j - 1]!))
      operations.push({ kind: "delete", text: a[--i]! });
    else operations.push({ kind: "add", text: b[--j]! });
  }
  const changes: ComposeChange[] = [];
  let line = prefix;
  let chunk: ComposeChange | undefined;
  for (const op of operations.reverse()) {
    if (op.kind === "keep") {
      if (chunk) {
        changes.push(chunk);
        chunk = undefined;
      }
      line++;
    } else {
      chunk ||= { start: line, end: line, before: [], after: [] };
      if (op.kind === "delete") chunk.before.push(op.text);
      else {
        chunk.after.push(op.text);
        chunk.end = ++line;
      }
    }
  }
  if (chunk) changes.push(chunk);
  return changes;
}
export function restoreCompose(current: string, change: ComposeChange) {
  const lines = current.split("\n");
  lines.splice(change.start, change.end - change.start, ...change.before);
  return lines.join("\n");
}
