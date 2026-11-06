import {
  useId,
  useState,
  type ReactNode,
  type InputHTMLAttributes,
} from "react";
import { Link } from "react-router";
import { useCommand } from "@/lib/queries";
import { ApiError } from "@/lib/api";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Textarea } from "./ui/textarea";
import { Badge } from "./ui/badge";
import { Skeleton } from "./ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "./ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "./ui/dialog";
import type { Pair } from "@/lib/types";
import { HugeiconsIcon, type IconSvgElement } from "@hugeicons/react";
import {
  Add01Icon, ArrowReloadHorizontalIcon, ArrowTurnBackwardIcon, Cancel01Icon, CancelCircleIcon,
  CheckmarkCircle02Icon, Clock01Icon, CrownIcon, Delete02Icon, FloppyDiskIcon, HelpCircleIcon,
  Loading03Icon, PackageDeliveredIcon, PauseCircleIcon, PlayCircleIcon,
  PowerServiceIcon, Search01Icon, StopCircleIcon, WifiOff01Icon,
} from "@hugeicons/core-free-icons";

export function ErrorMessage({ error }: { error: unknown }) {
  return error ? (
    <div
      role="alert"
      className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive"
    >
      {error instanceof ApiError && error.status === 403
        ? "You do not have permission to perform this action. "
        : ""}
      {error instanceof Error ? error.message : String(error)}
    </div>
  ) : null;
}
export function Loading({
  variant = "content",
  label = "Loading…",
}: {
  variant?: "content" | "table" | "chart";
  label?: string;
}) {
  return (
    <div role="status" aria-label={label} aria-busy="true" className="w-full">
      <span className="sr-only">{label}</span>
      <div aria-hidden="true">
        {variant === "table" ? (
          <div className="space-y-4">
            <Skeleton className="h-9 w-64 max-w-full" />
            <div className="overflow-hidden rounded-xl border bg-card">
              {Array.from({ length: 6 }, (_, row) => (
                <div key={row} className="grid grid-cols-4 gap-4 border-b p-4 last:border-b-0">
                  {Array.from({ length: 4 }, (_, column) => (
                    <Skeleton key={column} className={column === 0 ? "h-4 w-3/4" : "h-4 w-full"} />
                  ))}
                </div>
              ))}
            </div>
          </div>
        ) : variant === "chart" ? (
          <div className="flex h-64 flex-col gap-3">
            <Skeleton className="min-h-0 w-full flex-1" />
            <div className="flex justify-between">
              <Skeleton className="h-3 w-12" />
              <Skeleton className="h-3 w-12" />
              <Skeleton className="h-3 w-12" />
            </div>
          </div>
        ) : (
          <div className="space-y-4 rounded-xl border bg-card p-5">
            <Skeleton className="h-5 w-48 max-w-full" />
            <div className="grid gap-4 sm:grid-cols-2">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-24 w-full" />
          </div>
        )}
      </div>
    </div>
  );
}
export function Empty({
  children = "No resources found.",
}: {
  children?: ReactNode;
}) {
  return (
    <div className="rounded-xl border border-dashed py-14 text-center text-muted-foreground">
      {children}
    </div>
  );
}
export function Page({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
          {description && (
            <p className="mt-1 text-sm text-muted-foreground">{description}</p>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-2">{actions}</div>
      </div>
      {children}
    </div>
  );
}
export function Section({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="space-y-4 rounded-xl border bg-card p-5">
      <h2 className="font-semibold">{title}</h2>
      {children}
    </section>
  );
}
export function Field({
  label,
  hint,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; hint?: string }) {
  const id = useId();
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        {...props}
        id={id}
        aria-describedby={hint ? id + "-hint" : undefined}
      />
      {hint && (
        <p id={id + "-hint"} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
    </div>
  );
}
export function TextField({
  label,
  value,
  onChange,
  rows = 6,
  required = false,
  hint,
  readOnly = false,
}: {
  label: string;
  value: string;
  onChange?: (value: string) => void;
  rows?: number;
  required?: boolean;
  hint?: string;
  readOnly?: boolean;
}) {
  const id = useId();
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Textarea
        id={id}
        value={value}
        rows={rows}
        required={required}
        readOnly={readOnly}
        onChange={(e) => onChange?.(e.target.value)}
        className="font-mono text-xs leading-6"
      />
      {hint && <p className="text-xs text-muted-foreground">{hint}</p>}
    </div>
  );
}
export function Choice({
  label,
  value,
  onChange,
  options,
  disabled,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: string[];
  disabled?: boolean;
}) {
  const id = useId();
  // Radix reserves empty item values; encode all options so Default still returns "".
  const prefix = "choice:";
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      <Select
        value={prefix + value}
        onValueChange={(next) => onChange(next.slice(prefix.length))}
        disabled={disabled}
      >
        <SelectTrigger id={id} className="w-full min-w-0">
          <SelectValue className="min-w-0 truncate">{value || "Default"}</SelectValue>
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option} value={prefix + option}>
              {option || "Default"}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
export function Toggle({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-3 text-sm">
      <input
        type="checkbox"
        className="size-4 accent-primary"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
      {label}
    </label>
  );
}
export function Pairs({
  label,
  value = [],
  onChange,
}: {
  label: string;
  value?: Pair[];
  onChange: (v: Pair[]) => void;
}) {
  return (
    <Repeat
      title={label}
      value={value}
      add={() => ({ name: "", value: "" })}
      onChange={onChange}
    >
      {(row, set) => (
        <>
          <Field
            label="Name"
            value={row.name}
            required
            onChange={(e) => set({ ...row, name: e.target.value })}
          />
          <Field
            label="Value"
            value={row.value}
            onChange={(e) => set({ ...row, value: e.target.value })}
          />
        </>
      )}
    </Repeat>
  );
}
export function Repeat<T>({
  title,
  value,
  onChange,
  add,
  children,
}: {
  title: string;
  value: T[];
  onChange: (v: T[]) => void;
  add: () => T;
  children: (row: T, set: (v: T) => void) => ReactNode;
}) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium">{title}</h3>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...value, add()])}
        >
          <HugeiconsIcon icon={Add01Icon} strokeWidth={2} aria-hidden="true" className="size-3" />
          Add
        </Button>
      </div>
      {value.map((row, i) => (
        <div key={i} className="flex items-end gap-3 rounded-lg border p-3">
          <div className="grid flex-1 grid-cols-1 items-end gap-3 sm:grid-cols-2">
            {children(row, (v) =>
              onChange(value.map((old, j) => (i === j ? v : old))),
            )}
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={"Remove " + title + " row " + (i + 1)}
            onClick={() => onChange(value.filter((_, j) => i !== j))}
          >
            <HugeiconsIcon icon={Delete02Icon} strokeWidth={2} aria-hidden="true" className="size-4" />
          </Button>
        </div>
      ))}
    </div>
  );
}
const statusIcons = new Map<string, typeof HelpCircleIcon>([
  ["running", PlayCircleIcon],
  ["ready", CheckmarkCircle02Icon],
  ["active", PlayCircleIcon],
  ["leader", CrownIcon],
  ["completed", CheckmarkCircle02Icon],
  ["failed", CancelCircleIcon],
  ["rejected", CancelCircleIcon],
  ["error", CancelCircleIcon],
  ["shutdown", PowerServiceIcon],
  ["down", WifiOff01Icon],
  ["updating", ArrowReloadHorizontalIcon],
  ["rollback", ArrowTurnBackwardIcon],
  ["pending", Clock01Icon],
  ["new", Clock01Icon],
  ["assigned", Clock01Icon],
  ["accepted", CheckmarkCircle02Icon],
  ["starting", Loading03Icon],
  ["preparing", Loading03Icon],
  ["drain", ArrowTurnBackwardIcon],
  ["pause", PauseCircleIcon],
  ["paused", PauseCircleIcon],
  ["deployed", PackageDeliveredIcon],
  ["inactive", StopCircleIcon],
]);

export function Status({ value }: { value?: string }) {
  const good = ["running", "ready", "active", "leader", "completed"].includes(
    value || "",
  );
  const bad = ["failed", "rejected", "shutdown", "down", "error"].includes(
    value || "",
  );
  return (
    <Badge
      variant="outline"
      className={
        good
          ? "border-success/25 bg-success/10 text-success-foreground"
          : bad
            ? "border-destructive/25 text-destructive"
            : "text-muted-foreground"
      }
    >
      <HugeiconsIcon
        icon={statusIcons.get(value || "") ?? HelpCircleIcon}
        strokeWidth={2}
        aria-hidden="true"
        className="size-3 shrink-0"
      />
      {value || "unknown"}
    </Badge>
  );
}
const alphabeticalOrder = new Intl.Collator(undefined, { sensitivity: "base", numeric: true });

export function ResourceTable<T>({
  rows,
  columns,
  search,
  sortBy,
  empty,
}: {
  rows: readonly T[];
  columns: { label: string; render: (row: T) => ReactNode }[];
  search?: (row: T) => string;
  sortBy: (row: T) => string | number;
  empty?: string;
}) {
  const [filter, setFilter] = useState("");
  const filtered = search
    ? rows.filter((row) =>
        search(row).toLowerCase().includes(filter.toLowerCase()),
      )
    : rows;
  const sorted = [...filtered].sort((a, b) => alphabeticalOrder.compare(String(sortBy(a)), String(sortBy(b))));
  return (
    <div className="space-y-4">
      {search && (
        <div className="relative max-w-sm">
          <HugeiconsIcon icon={Search01Icon} strokeWidth={2} aria-hidden="true" className="absolute left-3 top-3 size-4 text-muted-foreground" />
          <Input
            aria-label="Filter resources"
            placeholder="Filter resources…"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="pl-9"
          />
        </div>
      )}
      {sorted.length ? (
        <div className="overflow-hidden rounded-xl border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                {columns.map((col) => (
                  <TableHead key={col.label}>{col.label}</TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {sorted.map((row, i) => (
                <TableRow key={i}>
                  {columns.map((col) => (
                    <TableCell key={col.label}>{col.render(row)}</TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      ) : (
        <Empty>{empty || "No matching resources."}</Empty>
      )}
    </div>
  );
}
export function ResourceLink({
  to,
  children,
}: {
  to: string;
  children: ReactNode;
}) {
  return (
    <Link
      to={to}
      className="font-medium text-foreground underline-offset-4 hover:underline"
    >
      {children}
    </Link>
  );
}
export function Properties({ values }: { values: Record<string, ReactNode> }) {
  return (
    <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
      {Object.entries(values).map(([name, value]) => (
        <div key={name}>
          <dt className="mb-1 text-xs text-muted-foreground">{name}</dt>
          <dd className="break-words text-sm">{value ?? "—"}</dd>
        </div>
      ))}
    </dl>
  );
}
export function PairValues({ value }: { value?: Pair[] }) {
  return value?.length ? (
    <div className="space-y-2">
      {value.map((p, i) => (
        <div key={i} className="flex flex-wrap gap-2 font-mono text-xs">
          <span className="text-muted-foreground">{p.name}</span>
          <span>{p.value}</span>
        </div>
      ))}
    </div>
  ) : (
    <p className="text-sm text-muted-foreground">None configured</p>
  );
}
export function Action({
  label,
  icon,
  path,
  method = "POST",
  body,
  destructive,
  confirm,
  onSuccess,
  disabled,
}: {
  label: string;
  icon: IconSvgElement;
  path: string;
  method?: string;
  body?: unknown;
  destructive?: boolean;
  confirm?: string;
  onSuccess?: () => void;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const command = useCommand(() => {
    setOpen(false);
    onSuccess?.();
  });
  function run() {
    command.mutate({ path, method, body });
  }
  return (
    <div className="space-y-2">
      <Button
        type="button"
        variant={destructive ? "destructive" : "outline"}
        disabled={disabled || command.isPending}
        onClick={() => (destructive || confirm ? setOpen(true) : run())}
      >
        <HugeiconsIcon icon={command.isPending ? Loading03Icon : icon} strokeWidth={2} aria-hidden="true" className={command.isPending ? "size-4 shrink-0 animate-spin motion-reduce:animate-none" : "size-4 shrink-0"} />
        {label}
      </Button>
      {!open && <ErrorMessage error={command.error} />}
      <Dialog
        open={open}
        onOpenChange={(v) => {
          if (!command.isPending) setOpen(v);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{label}?</DialogTitle>
            <DialogDescription>
              {confirm ||
                "This changes or removes the resource. Confirm that you want to continue."}
            </DialogDescription>
          </DialogHeader>
          <ErrorMessage error={command.error} />
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setOpen(false)}
              disabled={command.isPending}
            >
              <HugeiconsIcon icon={Cancel01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Cancel
            </Button>
            <Button
              type="button"
              variant={destructive ? "destructive" : "default"}
              onClick={run}
              disabled={command.isPending}
            >
              <HugeiconsIcon icon={command.isPending ? Loading03Icon : icon} strokeWidth={2} aria-hidden="true" className={command.isPending ? "size-4 shrink-0 animate-spin motion-reduce:animate-none" : "size-4 shrink-0"} />
              {command.isPending ? "Working…" : label}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
export function Submit({
  pending,
  icon = FloppyDiskIcon,
  children = "Save changes",
}: {
  pending: boolean;
  icon?: IconSvgElement;
  children?: ReactNode;
}) {
  return (
    <Button type="submit" disabled={pending}>
      <HugeiconsIcon icon={pending ? Loading03Icon : icon} strokeWidth={2} aria-hidden="true" className={pending ? "size-4 shrink-0 animate-spin motion-reduce:animate-none" : "size-4 shrink-0"} />
      {pending ? "Working…" : children}
    </Button>
  );
}
