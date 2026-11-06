import type { Service } from "@/lib/types";

export function serviceDisplayState(service: Pick<Service, "state" | "status">) {
  const update = service.status?.update;
  return update === "updating" ? "updating" : update === "rollback_started" ? "rollback" : service.state;
}
