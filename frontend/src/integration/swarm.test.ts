// @vitest-environment node
import { randomBytes, randomUUID } from "node:crypto";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { describe, expect, it } from "vitest";
import { defaultService, servicePayload } from "@/features/services/payload";
import { encodeSubscription } from "@/lib/events";
import type {
  Config,
  Compose,
  History,
  LogLine,
  Me,
  Network,
  Node,
  Secret,
  Service,
  Stack,
  Task,
  User,
  Version,
  Volume,
} from "@/lib/types";

// This suite initializes a NEW app/database and mutates only an explicitly disposable Swarm.
const enabled =
  !!process.env.SWARMPIT_TEST_URL &&
  process.env.SWARMPIT_DISPOSABLE_TEST === "1";
const repository = process.env.SWARMPIT_TEST_REPOSITORY || "busybox";
const tag = process.env.SWARMPIT_TEST_TAG || "1.36.1";
const dockerExec = promisify(execFile);

interface Reply<T> {
  status: number;
  data: T;
}
interface Cleanup {
  path: string;
  token: string;
}
type Credentials = { username: string; password: string };
const segment = encodeURIComponent;

describe.skipIf(!enabled)("disposable real Swarmpit backend", () => {
  it("roundtrips sessions, permissions, streams and Swarm resources", async () => {
    const base = new URL(process.env.SWARMPIT_TEST_URL!);
    if (
      !["http:", "https:"].includes(base.protocol) ||
      !["127.0.0.1", "localhost", "[::1]"].includes(base.hostname) ||
      base.username ||
      base.password ||
      base.pathname !== "/" ||
      base.search ||
      base.hash
    ) {
      throw new Error(
        "SWARMPIT_TEST_URL must be a loopback HTTP(S) origin without credentials or a path",
      );
    }
    const dind = process.env.SWARMPIT_TEST_DIND_CONTAINER;
    if (!dind || !/^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/.test(dind))
      throw new Error(
        "Set SWARMPIT_TEST_DIND_CONTAINER to the supplied disposable Docker-in-Docker container name for read-only volume-reference polling",
      );
    const prefix =
      "react-it-" + Date.now().toString(36) + "-" + randomUUID().slice(0, 8);
    const credentials = (suffix: string): Credentials => ({
      username: prefix + "-" + suffix,
      password: randomBytes(24).toString("base64url"),
    });
    const admin = credentials("admin");
    const operator = credentials("user");
    const viewer = credentials("viewer");
    const cleanup: Cleanup[] = [];
    const tokens: string[] = [];
    let adminToken = "";
    let primaryFailure: unknown;
    let phase = "preflight";
    const completed: string[] = [];

    async function request<T = unknown>(
      path: string,
      options: {
        method?: string;
        body?: unknown;
        token?: string;
        status?: number | number[];
        headers?: HeadersInit;
      } = {},
    ): Promise<Reply<T>> {
      const headers = new Headers(options.headers);
      headers.set("Accept", "application/json");
      if (options.token) headers.set("Authorization", options.token);
      if (options.body !== undefined)
        headers.set("Content-Type", "application/json");
      const response = await fetch(new URL(path, base), {
        method: options.method || "GET",
        headers,
        body:
          options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: AbortSignal.timeout(40000),
        redirect: "error",
      });
      const text = await response.text();
      let data: unknown;
      try {
        data = text ? JSON.parse(text) : undefined;
      } catch {
        data = text;
      }
      const allowed =
        options.status === undefined
          ? [200, 201, 202, 204]
          : Array.isArray(options.status)
            ? options.status
            : [options.status];
      if (!allowed.includes(response.status))
        throw new Error(
          phase +
            ": " +
            (options.method || "GET") +
            " " +
            path.split("?")[0] +
            " returned " +
            response.status +
            "; expected " +
            allowed.join("/"),
        );
      return { status: response.status, data: data as T };
    }
    const read = async <T>(path: string, token = adminToken) =>
      (await request<T>(path, { token })).data;
    const post = async <T>(path: string, body?: unknown, token = adminToken) =>
      (await request<T>(path, { method: "POST", body, token })).data;
    function remember(path: string, token = adminToken) {
      cleanup.unshift({ path, token });
    }
    function forget(path: string) {
      const index = cleanup.findIndex((item) => item.path === path);
      if (index >= 0) cleanup.splice(index, 1);
    }
    async function remove(path: string) {
      forget(path);
      await request(path, { method: "DELETE", token: adminToken });
    }
    async function poll<T>(
      label: string,
      get: () => Promise<T>,
      matches: (value: T) => boolean,
      timeout = 45000,
    ): Promise<T> {
      const deadline = Date.now() + timeout;
      do {
        const value = await get();
        if (matches(value)) return value;
        await new Promise((resolve) => setTimeout(resolve, 500));
      } while (Date.now() < deadline);
      throw new Error(phase + ": timed out waiting for " + label);
    }
    async function login(user: Credentials) {
      const authorization =
        "Basic " +
        Buffer.from(user.username + ":" + user.password).toString("base64");
      const result = await request<{ token: string }>("/login", {
        method: "POST",
        headers: { Authorization: authorization },
      });
      const token = result.data.token;
      if (typeof token !== "string" || !token.startsWith("Bearer "))
        throw new Error(
          "Login did not return a complete Bearer authorization value",
        );
      tokens.push(token);
      return token;
    }
    async function stream(slt: string, expectedServiceId?: string) {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 10000);
      try {
        const query = new URLSearchParams({
          slt,
          subscription: encodeSubscription({
            handler: "service-list",
            params: {},
          }),
        });
        const response = await fetch(new URL("/events?" + query, base), {
          signal: controller.signal,
          redirect: "error",
        });
        expect(response.status).toBe(200);
        expect(
          response.headers.get("content-type")?.includes("text/event-stream"),
        ).toBe(true);
        const reader = response.body?.getReader();
        if (!reader) throw new Error("SSE response has no readable body");
        try {
          const first = await reader.read();
          expect(first.done).toBe(false);
          const decoder = new TextDecoder();
          let buffer = decoder.decode(first.value, { stream: true });
          expect(buffer.includes(":ok")).toBe(true);
          // Use the existing authenticated agent ingress to request a real Docker snapshot.
          await post("/events", {
            type: "event",
            message: {
              Type: "service",
              Action: "update",
              Actor: {
                ID: expectedServiceId || prefix,
                Attributes: { name: prefix },
              },
            },
          });
          let snapshot: Service[] | undefined;
          while (!snapshot) {
            let boundary: number;
            while ((boundary = buffer.indexOf("\n\n")) >= 0) {
              const frame = buffer.slice(0, boundary);
              buffer = buffer.slice(boundary + 2);
              const data = frame
                .split("\n")
                .filter((line) => line.startsWith("data:"))
                .map((line) => line.slice(5).trimStart())
                .join("\n");
              if (data) {
                const parsed: unknown = JSON.parse(data);
                if (
                  Array.isArray(parsed) &&
                  (!expectedServiceId ||
                    parsed.some((s) => s.id === expectedServiceId))
                )
                  snapshot = parsed;
              }
            }
            if (snapshot) break;
            const next = await reader.read();
            if (next.done)
              throw new Error("SSE connection closed before its JSON snapshot");
            buffer += decoder.decode(next.value, { stream: true });
          }
          expect(Array.isArray(snapshot)).toBe(true);
          if (expectedServiceId)
            expect(snapshot.some((s) => s.id === expectedServiceId)).toBe(true);
        } finally {
          await reader.cancel();
        }
      } finally {
        clearTimeout(timeout);
        controller.abort();
      }
    }
    function mark(name: string) {
      completed.push(name);
    }

    try {
      // No mutating request is made until the fresh-instance guard succeeds.
      const initial = await read<Version>("/version", "");
      if (initial.initialized !== false)
        throw new Error(
          "Refusing integration run: Swarmpit instance is already initialized; create a new disposable database/app",
        );
      await request("/api/me", { status: 401 });
      await post("/initialize", admin, "");
      adminToken = await login(admin);
      // The upstream version endpoint memoizes admin existence for one second.
      await poll(
        "initialized version status",
        () => read<Version>("/version"),
        (version) => version.initialized === true,
        5000,
      );
      expect((await read<Me>("/api/me")).username).toBe(admin.username);
      expect((await read<Me>("/api/me")).role).toBe("admin");
      await request("/api/me", { token: "Bearer " + adminToken, status: 401 });
      await request("/initialize", {
        method: "POST",
        body: credentials("second-admin"),
        status: 403,
      });
      // A fresh app is insufficient if it points at an existing Docker cluster.
      if ((await read<Service[]>("/api/services")).length !== 0)
        throw new Error(
          "Refusing mutations: disposable Swarm must start with no services",
        );
      mark(
        "version, first-admin setup, login, exact Bearer reuse, unauthenticated rejection",
      );

      phase = "accounts and permissions";
      for (const [account, role] of [
        [operator, "user"],
        [viewer, "viewer"],
      ] as const) {
        const created = await post<{ id: string }>("/api/admin/users", {
          ...account,
          email: account.username + "@example.invalid",
          role,
        });
        if (!created.id) throw new Error("Create user returned no ID");
        remember("/api/admin/users/" + segment(created.id));
        expect(
          (await read<User>("/api/admin/users/" + segment(created.id))).role,
        ).toBe(role);
      }
      const operatorToken = await login(operator);
      const viewerToken = await login(viewer);
      expect((await read<Me>("/api/me", operatorToken)).role).toBe("user");
      expect((await read<Me>("/api/me", viewerToken)).role).toBe("viewer");
      const nodes = await read<Node[]>("/api/nodes");
      if (nodes.length !== 1)
        throw new Error(
          "Disposable integration Swarm must have exactly one node",
        );
      await request("/api/nodes/" + segment(nodes[0].id), {
        method: "POST",
        token: operatorToken,
        body: {
          nodeName: nodes[0].nodeName,
          version: nodes[0].version,
          role: nodes[0].role,
          availability: nodes[0].availability,
          labels: nodes[0].labels,
        },
        status: 403,
      });
      await request("/api/admin/users", { token: operatorToken, status: 403 });
      await request("/api/admin/users", { token: viewerToken, status: 403 });
      await request("/api/admin/users", {
        token: viewerToken,
        method: "POST",
        body: { ...credentials("forbidden"), role: "admin" },
        status: 403,
      });
      await request("/api/services", {
        token: viewerToken,
        method: "POST",
        body: servicePayload(defaultService(repository), false),
        status: 403,
      });
      expect(
        Array.isArray(await read<Service[]>("/api/services", viewerToken)),
      ).toBe(true);
      mark(
        "admin user creation/read, operator/viewer login, server-enforced role denial",
      );

      phase = "event streams";
      const first = (await read<{ slt: string }>("/slt")).slt;
      if (!first) throw new Error("SLT endpoint returned no token");
      await stream(first);
      const subscription = encodeSubscription({
        handler: "service-list",
        params: {},
      });
      await request(
        "/events?" + new URLSearchParams({ slt: first, subscription }),
        { status: 401 },
      );
      const fresh = (await read<{ slt: string }>("/slt")).slt;
      expect(!!fresh && fresh !== first).toBe(true);
      await stream(fresh);
      mark(
        "SSE content type/body, single-use SLT rejection, fresh-token reconnect",
      );

      phase = "network, volume, config and secret create/read";
      const networkName = prefix + "-net";
      const network = await post<{ id: string }>(
        "/api/networks",
        {
          networkName,
          driver: "overlay",
          internal: false,
          attachable: true,
          ingress: false,
          enableIPv6: false,
          ipam: { subnet: "", gateway: "" },
          options: [],
        },
        operatorToken,
      );
      if (!network.id) throw new Error("Create network returned no ID");
      const networkPath = "/api/networks/" + segment(network.id);
      remember(networkPath);
      expect((await read<Network>(networkPath)).networkName).toBe(networkName);
      expect(
        (await read<Network[]>("/api/networks")).some(
          (n) => n.id === network.id,
        ),
      ).toBe(true);

      const volumeName = prefix + "-vol";
      const volume = await post<Volume>("/api/volumes", {
        volumeName,
        driver: "local",
        options: [],
      });
      expect(volume.volumeName).toBe(volumeName);
      const volumePath = "/api/volumes/" + segment(volumeName);
      remember(volumePath);
      expect((await read<Volume>(volumePath)).driver).toBe("local");
      expect(
        (await read<Volume[]>("/api/volumes")).some(
          (v) => v.volumeName === volumeName,
        ),
      ).toBe(true);

      const configName = prefix + "-cfg";
      const configText = "# Unicode survives Base64\nmessage=ahoj světe\n";
      const config = await post<{ id: string }>("/api/configs", {
        configName,
        data: Buffer.from(configText).toString("base64"),
      });
      if (!config.id) throw new Error("Create config returned no ID");
      const configPath = "/api/configs/" + segment(config.id);
      remember(configPath);
      const configRead = await read<Config>(configPath);
      expect(configRead.configName).toBe(configName);
      expect(
        Buffer.from(configRead.data || "", "base64").toString("utf8"),
      ).toBe(configText);
      expect(
        (await read<Config[]>("/api/configs")).some((c) => c.id === config.id),
      ).toBe(true);

      const secretName = prefix + "-secret";
      const secret = await post<{ id: string }>("/api/secrets", {
        secretName,
        data: Buffer.from("disposable test secret").toString("base64"),
      });
      if (!secret.id) throw new Error("Create secret returned no ID");
      const secretPath = "/api/secrets/" + segment(secret.id);
      remember(secretPath);
      expect((await read<Secret>(secretPath)).secretName).toBe(secretName);
      expect("data" in (await read<Secret>(secretPath))).toBe(false);
      expect(
        (await read<Secret[]>("/api/secrets")).some((s) => s.id === secret.id),
      ).toBe(true);
      await request(secretPath, { token: viewerToken, status: 403 });
      await request(networkPath, {
        token: viewerToken,
        method: "DELETE",
        status: 403,
      });
      mark(
        "network/volume/config/secret create, list, detail, UTF-8 encoding and secret-content isolation",
      );

      phase = "service create and full edit";
      const service = defaultService(repository);
      service.repository.tag = tag;
      service.serviceName = prefix + "-service";
      service.entrypoint = ["/bin/sh", "-c"];
      service.command = ["echo swarmpit-integration-ready; sleep 3600"];
      service.variables = [
        { name: "INTEGRATION_MESSAGE", value: "hello with spaces" },
      ];
      service.labels = [{ name: "integration-owner", value: prefix }];
      service.containerLabels = [{ name: "remove-on-edit", value: "old" }];
      service.sysctls = [
        { name: "net.ipv4.tcp_syncookies", value: "1" },
        { name: "net.ipv4.tcp_fin_timeout", value: "60" },
      ];
      service.networks = [{ networkName }];
      service.mounts = [
        {
          type: "volume",
          host: volumeName,
          containerPath: "/integration-volume",
          readOnly: false,
        },
        { type: "tmpfs", containerPath: "/integration-cache", readOnly: false },
      ];
      service.configs = [{ configName, configTarget: "/integration.conf" }];
      service.secrets = [{ secretName, secretTarget: "integration-secret" }];
      const created = await post<{ id: string }>(
        "/api/services",
        servicePayload(service, false),
        operatorToken,
      );
      if (!created.id) throw new Error("Create service returned no ID");
      const servicePath = "/api/services/" + segment(created.id);
      remember(servicePath);
      const running = await poll(
        "running service task",
        () => read<Service>(servicePath),
        (s) => (s.status?.tasks?.running || 0) > 0,
        60000,
      );
      expect(running.serviceName).toBe(service.serviceName);
      expect(running.command).toEqual(service.command);
      expect(running.entrypoint).toEqual(service.entrypoint);
      expect(running.variables).toContainEqual(service.variables[0]);
      expect(
        running.mounts.some(
          (m) =>
            m.type === "tmpfs" &&
            m.containerPath === "/integration-cache" &&
            !m.host,
        ),
      ).toBe(true);
      expect(
        (await read<Service[]>("/api/services")).some(
          (s) => s.id === created.id,
        ),
      ).toBe(true);
      expect(
        (await read<Compose>(servicePath + "/compose")).spec.compose.includes(
          service.serviceName,
        ),
      ).toBe(true);
      expect(
        (await read<Network[]>(servicePath + "/networks")).some(
          (n) => n.networkName === networkName,
        ),
      ).toBe(true);
      expect(
        (await read<Task[]>(servicePath + "/tasks")).some(
          (t) => t.state === "running",
        ),
      ).toBe(true);
      await poll(
        "native or agent service logs",
        () => read<LogLine[]>(servicePath + "/logs?since=15m"),
        (logs) =>
          logs.some((l) => l.line.includes("swarmpit-integration-ready")),
        30000,
      );
      await request(servicePath, {
        method: "DELETE",
        token: viewerToken,
        status: 403,
      });

      running.variables.push({ name: "AFTER_EDIT", value: "preserved" });
      running.labels.push({ name: "updated-service", value: "yes" });
      running.containerLabels = [{ name: "container-only", value: "separate" }];
      running.sysctls = [{ name: "net.ipv4.tcp_syncookies", value: "0" }];
      running.networks = [
        { networkName, serviceAliases: ["integration-alias"] },
      ];
      running.resources.reservation = { cpu: 0.01, memory: 4 };
      running.deployment.maxReplicas = 1;
      await post(
        servicePath + "?comment=integration-edit",
        servicePayload(running, true),
      );
      const edited = await poll(
        "service edit version",
        () => read<Service>(servicePath),
        (s) => s.version > running.version,
      );
      expect(edited.variables).toContainEqual({
        name: "AFTER_EDIT",
        value: "preserved",
      });
      expect(edited.containerLabels).toEqual([{
        name: "container-only",
        value: "separate",
      }]);
      expect(edited.sysctls).toEqual([{ name: "net.ipv4.tcp_syncookies", value: "0" }]);
      expect(edited.labels).toContainEqual({
        name: "updated-service",
        value: "yes",
      });
      expect(
        edited.networks.some((n) =>
          n.serviceAliases?.includes("integration-alias"),
        ),
      ).toBe(true);
      expect(edited.deployment.maxReplicas).toBe(1);
      expect(edited.command).toEqual(service.command);
      expect(edited.entrypoint).toEqual(service.entrypoint);
      await post(servicePath, servicePayload({
        ...edited, containerLabels: [], sysctls: [],
      }, true));
      const cleared = await poll(
        "container label and kernel parameter removal",
        () => read<Service>(servicePath),
        (s) => s.version > edited.version,
      );
      expect(cleared.containerLabels).toEqual([]);
      expect(cleared.sysctls).toEqual([]);
      await stream((await read<{ slt: string }>("/slt")).slt, created.id);
      mark(
        "running service create/list/detail/tasks/networks/logs/compose, live snapshot and advanced edit roundtrip",
      );

      phase = "service actions";
      await request(servicePath + "/dashboard", {
        method: "POST",
        token: viewerToken,
      });
      expect(
        (await read<Me>("/api/me", viewerToken))["service-dashboard"]?.includes(
          created.id,
        ),
      ).toBe(true);
      await request(servicePath + "/dashboard", {
        method: "DELETE",
        token: viewerToken,
      });
      expect(
        (await read<Me>("/api/me", viewerToken))["service-dashboard"]?.includes(
          created.id,
        ),
      ).toBe(false);
      await poll(
        "update completion",
        () => read<Service>(servicePath),
        (s) =>
          s.status?.update !== "updating" &&
          (s.status?.tasks?.running || 0) > 0,
      );
      const beforeRedeploy = await read<Service>(servicePath);
      await post(
        servicePath +
          "/redeploy?tag=" +
          segment(tag) +
          "&comment=integration-redeploy",
      );
      const redeployed = await poll(
        "redeploy force update",
        () => read<Service>(servicePath),
        (s) =>
          s.version > beforeRedeploy.version &&
          (s.deployment.forceUpdate || 0) >
            (beforeRedeploy.deployment.forceUpdate || 0),
      );
      await poll(
        "redeploy completion",
        () => read<Service>(servicePath),
        (s) =>
          s.status?.update !== "updating" &&
          (s.status?.tasks?.running || 0) > 0,
      );
      if (!redeployed.deployment.rollbackAllowed)
        throw new Error("Service update did not provide rollback capability");
      const beforeRollback = await read<Service>(servicePath);
      await post(servicePath + "/rollback?comment=integration-rollback");
      await poll(
        "rollback service version",
        () => read<Service>(servicePath),
        (s) => s.version > beforeRollback.version,
      );
      await post(servicePath + "/stop");
      await poll(
        "stopped service",
        () => read<Service>(servicePath),
        (s) => s.replicas === 0 && (s.status?.tasks?.running || 0) === 0,
      );
      await poll(
        "all stopped task processes",
        () => read<Task[]>(servicePath + "/tasks"),
        (tasks) =>
          tasks.every(
            (t) =>
              ![
                "new",
                "allocated",
                "pending",
                "assigned",
                "accepted",
                "preparing",
                "ready",
                "starting",
                "running",
              ].includes(t.state),
          ),
      );
      await remove(servicePath);
      await poll(
        "service deletion",
        () => read<Service[]>("/api/services"),
        (rows) => !rows.some((s) => s.id === created.id),
      );
      mark("viewer pin/unpin, service redeploy/tag, rollback, stop and delete");

      phase = "stack compose lifecycle";
      const stackName = prefix + "-stack";
      const stackPath = "/api/stacks/" + segment(stackName);
      const compose =
        '# exact stored source\nversion: "3.8"\nservices:\n  app:\n    image: ' +
        repository +
        ":" +
        tag +
        '\n    command: ["/bin/sh", "-c", "sleep 3600"]\n    deploy:\n      replicas: 0\n';
      const stack = {
        name: stackName,
        spec: { compose },
        skipImageResolve: true,
      };
      // Register first: CLI deploy may create a partial stack before reporting a failure.
      remember(stackPath);
      await post("/api/stacks?comment=integration-stack-create", stack);
      await poll(
        "stack deployment",
        () => read<Service[]>(stackPath + "/services"),
        (rows) => rows.length === 1,
      );
      expect((await read<Compose>(stackPath + "/file")).spec.compose).toBe(
        compose,
      );
      expect(
        (await read<History[]>(stackPath + "/history")).some(
          (h) => h.comment === "integration-stack-create",
        ),
      ).toBe(true);
      const nextCompose =
        compose.replace(
          "    deploy:\n",
          '    environment:\n      INTEGRATION_STACK_REVISION: "2"\n    deploy:\n',
        ) + "# edited source remains intact\n";
      await post(stackPath + "?comment=integration-stack-edit", {
        ...stack,
        spec: { compose: nextCompose },
      });
      expect((await read<Compose>(stackPath + "/file")).spec.compose).toBe(
        nextCompose,
      );
      expect(
        (await read<History[]>(stackPath + "/history")).some(
          (h) => h.comment === "integration-stack-edit",
        ),
      ).toBe(true);
      await post(stackPath + "/deactivate");
      await poll(
        "inactive stack services",
        () => read<Service[]>(stackPath + "/services"),
        (rows) => rows.length === 0,
      );
      expect((await read<Compose>(stackPath + "/file")).spec.compose).toBe(
        nextCompose,
      );
      expect(
        (await read<Array<Stack & { state?: string }>>("/api/stacks")).some(
          (s) => s.stackName === stackName && s.state === "inactive",
        ),
      ).toBe(true);
      await post(stackPath + "?comment=integration-stack-reactivate", {
        ...stack,
        spec: { compose: nextCompose },
      });
      await poll(
        "reactivated stack",
        () => read<Service[]>(stackPath + "/services"),
        (rows) => rows.length === 1,
      );
      await remove(stackPath);
      await poll(
        "stack deletion",
        () => read<Stack[]>("/api/stacks"),
        (rows) => !rows.some((s) => s.stackName === stackName),
      );
      mark(
        "stack deploy/edit, exact stored compose/history, deactivate/reactivate and delete",
      );

      phase = "resource deletion";
      await remove(configPath);
      await remove(secretPath);
      await remove(networkPath);
      // The API omits container references; read the supplied nested daemon until task GC releases this volume.
      await poll(
        "Docker task containers releasing the created volume",
        async () => {
          const result = await dockerExec(
            "docker",
            [
              "exec",
              dind,
              "docker",
              "ps",
              "--all",
              "--filter",
              "volume=" + volumeName,
              "--format",
              "{{.ID}}",
            ],
            { timeout: 5000, maxBuffer: 4096 },
          );
          return result.stdout.trim();
        },
        (references) => references === "",
        60000,
      );
      await remove(volumePath);
      expect(
        (await read<Config[]>("/api/configs")).some((c) => c.id === config.id),
      ).toBe(false);
      expect(
        (await read<Secret[]>("/api/secrets")).some((s) => s.id === secret.id),
      ).toBe(false);
      expect(
        (await read<Network[]>("/api/networks")).some(
          (n) => n.id === network.id,
        ),
      ).toBe(false);
      expect(
        (await read<Volume[]>("/api/volumes")).some(
          (v) => v.volumeName === volumeName,
        ),
      ).toBe(false);
      mark("created resource deletion verified by actual lists");

      phase = "logout revocation";
      await post("/logout", undefined, operatorToken);
      await request("/api/me", { token: operatorToken, status: 401 });
      await post("/logout", undefined, viewerToken);
      await request("/api/me", { token: viewerToken, status: 401 });
      mark("operator/viewer logout JWT revocation");
    } catch (error) {
      primaryFailure = error;
    } finally {
      const cleanupFailures: string[] = [];
      for (const item of cleanup) {
        try {
          await request(item.path, { method: "DELETE", token: item.token });
        } catch {
          cleanupFailures.push(item.path);
        }
      }
      if (adminToken) {
        try {
          phase = "administrator logout revocation";
          await post("/logout", undefined, adminToken);
          await request("/api/me", { token: adminToken, status: 401 });
          mark("administrator logout JWT revocation");
        } catch {
          cleanupFailures.push("administrator logout/revocation");
        }
      }
      // Keep credentials/tokens out of error output, including when assertions fail.
      tokens.fill("");
      admin.password = "";
      operator.password = "";
      viewer.password = "";
      if (cleanupFailures.length) {
        const detail =
          "Cleanup incomplete for created test resources: " +
          cleanupFailures.join(", ");
        primaryFailure = new AggregateError(
          primaryFailure
            ? [primaryFailure, new Error(detail)]
            : [new Error(detail)],
          "Disposable integration run requires runtime disposal",
        );
      }
    }
    if (primaryFailure) throw primaryFailure;
    expect(completed.length).toBe(10);
  }, 360000);
});
