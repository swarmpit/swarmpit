import { expect, it } from "vitest";
import { defaultService, servicePayload } from "./payload";
it("keeps Docker entrypoint separate from command and both label scopes", () => {
  const service = defaultService("nginx");
  service.entrypoint = ["/bin/sh", "-c"];
  service.command = ["echo hello world"];
  service.labels = [{ name: "service", value: "one" }];
  service.containerLabels = [{ name: "container", value: "two" }];
  const payload = servicePayload(service, true);
  expect(payload.entrypoint).toEqual(["/bin/sh", "-c"]);
  expect(payload.command).toEqual(["echo hello world"]);
  expect(payload.labels).toEqual(service.labels);
  expect(payload.containerLabels).toEqual(service.containerLabels);
});
it("omits meaningless hosts for tmpfs and anonymous volume mounts without losing volume options", () => {
  const service = defaultService("nginx");
  service.mounts = [
    { type: "tmpfs", host: "/old", containerPath: "/cache", readOnly: false },
    {
      type: "volume",
      host: "",
      containerPath: "/data",
      readOnly: true,
      volumeOptions: {
        driver: { name: "local", options: [{ name: "type", value: "tmpfs" }] },
      },
    },
  ];
  expect(servicePayload(service, true).mounts).toEqual([
    { type: "tmpfs", containerPath: "/cache", readOnly: false },
    {
      type: "volume",
      containerPath: "/data",
      readOnly: true,
      volumeOptions: {
        driver: { name: "local", options: [{ name: "type", value: "tmpfs" }] },
      },
    },
  ]);
  expect(service.mounts[0].host).toBe("/old");
});
it("preserves the edit version and advanced deployment data", () => {
  const s = defaultService("registry:5000/team/image");
  s.serviceName = "test";
  s.version = 7;
  s.deployment.restartPolicy.window = 12;
  s.deployment.placement = [{ rule: "node.role == manager" }];
  s.deployment.maxReplicas = 2;
  const p = servicePayload(s, true);
  expect(p.version).toBe(7);
  expect(p.repository.name).toBe("registry:5000/team/image");
  expect(p.deployment).toEqual(s.deployment);
});
it("normalizes actual Docker volume option maps without losing values or forwarding nulls", () => {
  const s = defaultService("nginx");
  s.mounts = JSON.parse(
    '[{"containerPath":"/data","host":"data","type":"volume","readOnly":false,"id":"data","stack":null,"volumeOptions":{"labels":null,"driver":{"name":"local","options":{"type":"tmpfs","device":"tmpfs"}}}}]',
  );
  expect(servicePayload(s, true).mounts).toEqual([
    {
      containerPath: "/data",
      host: "data",
      type: "volume",
      readOnly: false,
      volumeOptions: {
        driver: {
          name: "local",
          options: [
            { name: "type", value: "tmpfs" },
            { name: "device", value: "tmpfs" },
          ],
        },
      },
    },
  ]);
});
it("rejects invalid resource limits and out-of-range ports before mutation", () => {
  const s = defaultService("nginx");
  s.resources.limit.memory = -1;
  expect(() => servicePayload(s, false)).toThrow(/memory/i);
  s.resources.limit.memory = 0;
  s.ports = [
    { hostPort: 99999, containerPort: 80, mode: "ingress", protocol: "tcp" },
  ];
  expect(() => servicePayload(s, false)).toThrow(/port/i);
});
it("omits null Docker healthcheck timings while preserving NONE", () => {
  const s = defaultService("nginx");
  s.healthcheck = {
    test: ["NONE"],
    interval: null,
    timeout: null,
    startPeriod: null,
    retries: null,
  } as unknown as typeof s.healthcheck;
  expect(servicePayload(s, true).healthcheck).toEqual({ test: ["NONE"] });
});
