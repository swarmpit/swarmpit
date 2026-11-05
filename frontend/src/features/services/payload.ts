import type { Service } from "@/lib/types";

export function defaultService(repository: string): Service {
  return {
    id: "",
    version: 0,
    serviceName: "",
    repository: { name: repository, tag: "" },
    mode: "replicated",
    replicas: 1,
    ports: [],
    mounts: [],
    networks: [],
    secrets: [],
    configs: [],
    hosts: [],
    variables: [],
    labels: [],
    containerLabels: [],
    sysctls: [],
    command: [],
    entrypoint: [],
    logdriver: { name: "json-file", opts: [] },
    resources: {
      reservation: { cpu: 0, memory: 0 },
      limit: { cpu: 0, memory: 0 },
    },
    deployment: {
      autoredeploy: false,
      restartPolicy: { condition: "any", delay: 5, attempts: 0 },
      update: {
        parallelism: 1,
        delay: 0,
        order: "stop-first",
        failureAction: "pause",
      },
      rollback: {
        parallelism: 1,
        delay: 0,
        order: "stop-first",
        failureAction: "pause",
      },
      placement: [],
    },
  };
}

function nonnegative(value: number, name: string, integer = false) {
  if (
    !Number.isFinite(value) ||
    value < 0 ||
    (integer && !Number.isInteger(value))
  )
    throw new Error(
      name + " must be a non-negative " + (integer ? "integer" : "number"),
    );
}

export function editableService(service: Service): Service {
  const copy = structuredClone(service);
  if (copy.healthcheck)
    copy.healthcheck = Object.fromEntries(
      Object.entries(copy.healthcheck).filter(([, value]) => value != null),
    );
  copy.mounts = copy.mounts.map((m) => {
    const driver = m.volumeOptions?.driver;
    const options: unknown = driver?.options;
    return {
      containerPath: m.containerPath,
      ...(m.host != null ? { host: m.host } : {}),
      type: m.type,
      readOnly: m.readOnly,
      ...(m.volumeOptions
        ? {
            volumeOptions: {
              ...(m.volumeOptions.labels != null
                ? { labels: m.volumeOptions.labels }
                : {}),
              ...(driver?.name
                ? {
                    driver: {
                      name: driver.name,
                      ...(options != null
                        ? {
                            options: Array.isArray(options)
                              ? options
                              : Object.entries(
                                  options as Record<string, string>,
                                ).map(([name, value]) => ({ name, value })),
                          }
                        : {}),
                    },
                  }
                : {}),
            },
          }
        : {}),
    };
  });
  return copy;
}

export function servicePayload(service: Service, editing: boolean) {
  const s = editableService(service);
  if (!s.repository.name.trim()) throw new Error("Repository is required");
  for (const kind of ["reservation", "limit"] as const) {
    nonnegative(s.resources[kind].cpu, kind + " CPU");
    nonnegative(s.resources[kind].memory, kind + " memory");
  }
  if (s.mode === "replicated") nonnegative(s.replicas ?? 0, "Replicas", true);
  for (const p of s.ports) {
    if (
      !Number.isInteger(p.containerPort) ||
      p.containerPort < 1 ||
      p.containerPort > 65535 ||
      !Number.isInteger(p.hostPort) ||
      p.hostPort < 0 ||
      p.hostPort > 65535
    )
      throw new Error(
        "Ports must be integers from 1 to 65535 (published port may be 0)",
      );
  }
  for (const m of s.mounts) {
    if (!m.containerPath.trim())
      throw new Error("Mount container path is required");
    if (m.type === "bind" && !m.host?.trim())
      throw new Error("Bind mount host path is required");
    if (m.type === "tmpfs" || !m.host?.trim()) delete m.host;
  }
  for (const name of ["update", "rollback"] as const) {
    nonnegative(s.deployment[name].parallelism, name + " parallelism", true);
    nonnegative(s.deployment[name].delay, name + " delay");
  }
  const policy = s.deployment.restartPolicy;
  nonnegative(policy.delay, "Restart delay");
  nonnegative(policy.attempts, "Restart attempts", true);
  if (policy.window != null) nonnegative(policy.window, "Restart window");
  if (s.deployment.maxReplicas != null)
    nonnegative(s.deployment.maxReplicas, "Maximum replicas per node", true);
  for (const name of ["interval", "timeout", "startPeriod", "retries"] as const)
    if (s.healthcheck?.[name] != null)
      nonnegative(
        s.healthcheck[name],
        "Healthcheck " + name,
        name === "retries",
      );
  const {
    forceUpdate: _force,
    rollbackAllowed: _allowed,
    ...deployment
  } = s.deployment;
  return {
    repository: { name: s.repository.name, tag: s.repository.tag },
    serviceName: s.serviceName,
    mode: s.mode,
    ...(s.mode === "replicated" ? { replicas: s.replicas } : {}),
    ports: s.ports,
    mounts: s.mounts.map((m) =>
      editing
        ? m
        : {
            containerPath: m.containerPath,
            ...(m.host ? { host: m.host } : {}),
            type: m.type,
            readOnly: m.readOnly,
          },
    ),
    networks: s.networks.map((n) => ({
      networkName: n.networkName,
      ...(editing && n.serviceAliases
        ? { serviceAliases: n.serviceAliases }
        : {}),
    })),
    secrets: s.secrets.map(({ secretName, secretTarget }) => ({
      secretName,
      secretTarget,
    })),
    configs: s.configs.map(({ configName, configTarget }) => ({
      configName,
      configTarget,
    })),
    hosts: s.hosts,
    variables: s.variables,
    labels: s.labels,
    command: s.command ?? [],
    entrypoint: s.entrypoint ?? [],
    ...(s.hostname != null ? { hostname: s.hostname } : {}),
    ...(s.isolation != null ? { isolation: s.isolation } : {}),
    sysctls: s.sysctls ?? [],
    logdriver: s.logdriver,
    resources: s.resources,
    deployment,
    ...(s.healthcheck ? { healthcheck: s.healthcheck } : {}),
    ...(editing
      ? {
          version: s.version,
          containerLabels: s.containerLabels ?? [],
          ...(s.stack != null ? { stack: s.stack } : {}),
          ...(s.agent != null ? { agent: s.agent } : {}),
          ...(s.immutable != null ? { immutable: s.immutable } : {}),
          ...(s.links != null ? { links: s.links } : {}),
          ...(s.user != null ? { user: s.user } : {}),
          ...(s.dir != null ? { dir: s.dir } : {}),
          ...(s.tty != null ? { tty: s.tty } : {}),
        }
      : {}),
  };
}
