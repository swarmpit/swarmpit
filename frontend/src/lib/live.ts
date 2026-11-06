import { useEffect, useState } from "react";
import { useQueryClient, type QueryClient } from "@tanstack/react-query";
import { connectEvents, type Subscription } from "./events";
import { apiKey } from "./queries";
import { segment } from "./api";
import type { Me, Stack, Task } from "./types";
export function applySnapshot(
  client: QueryClient,
  subscription: Subscription,
  data: unknown,
) {
  const { handler, params: p } = subscription;
  const root = handler.split("-")[0];
  if (
    ["service-list", "node-list", "task-list", "stack-list"].includes(handler)
  ) {
    if (Array.isArray(data))
      client.setQueryData(apiKey("/api/" + root + "s"), data);
    return;
  }
  if (handler === "task-info") {
    if (Array.isArray(data)) {
      const ids = new Set(
        data
          .filter((item) => item && typeof item.id === "string")
          .map((item) => item.id),
      );
      for (const [key, task] of client.getQueriesData<Task>({
        queryKey: ["api"],
      })) {
        if (
          typeof key[1] === "string" &&
          key[1].startsWith("/api/tasks/") &&
          !key[1].endsWith("/ts") &&
          task?.serviceName === p.serviceName &&
          !ids.has(task.id)
        )
          client.setQueryData(key, { ...task, state: "removed" });
      }
      for (const item of data)
        if (item && typeof item === "object" && typeof item.id === "string")
          client.setQueryData(apiKey("/api/tasks/" + segment(item.id)), item);
    }
    return;
  }
  if (!data || typeof data !== "object" || Array.isArray(data)) return;
  const snapshot = data as Record<string, unknown>;
  const set = (path: string, key: string) => {
    if (snapshot[key] !== undefined)
      client.setQueryData(apiKey(path), snapshot[key]);
  };
  if (handler === "index") {
    for (const key of ["services", "nodes", "stats"]) set("/api/" + key, key);
    set("/api/nodes/ts", "nodes-ts");
    set("/api/services/ts/cpu", "services-ts-cpu");
    set("/api/services/ts/memory", "services-ts-memory");
    client.setQueryData<Me>(apiKey("/api/me"), (old) =>
      old
        ? {
            ...old,
            ...(Array.isArray(snapshot["services-dashboard"])
              ? {
                  "service-dashboard": snapshot[
                    "services-dashboard"
                  ] as string[],
                }
              : {}),
            ...(Array.isArray(snapshot["nodes-dashboard"])
              ? { "node-dashboard": snapshot["nodes-dashboard"] as string[] }
              : {}),
          }
        : old,
    );
  } else if (handler === "service-info" || handler === "node-info") {
    const entity = snapshot[root] as
      { id?: unknown; serviceName?: unknown } | undefined;
    const identifiers = new Set([p.id]);
    if (typeof entity?.id === "string") identifiers.add(entity.id);
    if (root === "service" && typeof entity?.serviceName === "string")
      identifiers.add(entity.serviceName);
    for (const id of identifiers) {
      const path = "/api/" + root + "s/" + segment(id);
      set(path, root);
      set(path + "/tasks", "tasks");
      set(path + "/networks", "networks");
    }
    set("/api/stats", "stats");
  } else if (handler === "stack-info") {
    const stack = data as Stack;
    client.setQueryData<Stack[]>(apiKey("/api/stacks"), (old) =>
      old?.map((s) => (s.stackName === p.name ? { ...s, ...stack } : s)),
    );
    for (const key of [
      "services",
      "tasks",
      "networks",
      "volumes",
      "configs",
      "secrets",
    ])
      set("/api/stacks/" + segment(p.name) + "/" + key, key);
    set("/api/stats", "stats");
  }
}
export function useLive(handler: string, params: Record<string, string> = {}) {
  const client = useQueryClient();
  const [status, setStatus] = useState("connecting");
  const encoded = JSON.stringify({ handler, params });
  useEffect(() => {
    const subscription = JSON.parse(encoded) as Subscription;
    return connectEvents(
      subscription,
      (data) => applySnapshot(client, subscription, data),
      (next) => {
        setStatus(next);
        if (next === "live")
          void client.invalidateQueries({
            queryKey: ["api"],
            refetchType: "active",
          });
      },
    );
  }, [encoded, client]);
  return status;
}
