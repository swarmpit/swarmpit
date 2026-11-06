import { afterEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { ServiceEditor } from "./service-form";
import { defaultService } from "./payload";

const { mutate } = vi.hoisted(() => ({ mutate: vi.fn() }));
vi.mock("@/lib/queries", () => ({
  useApi: () => ({ data: [], error: null, isPending: false }),
  useCommand: () => ({ mutate, isPending: false, error: null }),
}));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "admin", role: "admin" },
    version: { docker: { api: 1.45 } },
  }),
}));
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

it("retains dirty fields and original edit version across server refetches", () => {
  const service = defaultService("nginx");
  service.id = "service-id";
  service.serviceName = "nginx";
  service.version = 8;
  service.replicas = 2;
  const view = render(
    <MemoryRouter>
      <ServiceEditor initial={service} editing />
    </MemoryRouter>,
  );
  fireEvent.change(screen.getByLabelText("Replicas"), {
    target: { value: "7" },
  });
  view.rerender(
    <MemoryRouter>
      <ServiceEditor
        initial={{ ...service, version: 9, replicas: 3 }}
        editing
      />
    </MemoryRouter>,
  );
  expect(screen.getByLabelText("Replicas")).toHaveValue(7);
  fireEvent.click(screen.getByRole("button", { name: "Deploy changes" }));
  expect(mutate).toHaveBeenCalledTimes(1);
  expect(mutate.mock.calls[0][0]).toMatchObject({
    path: "/api/services/service-id",
    body: { version: 8, replicas: 7 },
  });
});

it("submits native advanced fields through the update POST with encoded history comment", () => {
  const service = defaultService("registry:5000/team/image");
  service.id = "id";
  service.serviceName = "web";
  service.entrypoint = ["/bin/sh", "-c"];
  service.command = ["echo hello world"];
  service.networks = [
    { networkName: "internal", serviceAliases: ["api", "web"] },
  ];
  service.mounts = [
    {
      type: "volume",
      host: "data",
      containerPath: "/var/data",
      readOnly: false,
      volumeOptions: {
        driver: { name: "local", options: [{ name: "type", value: "tmpfs" }] },
        labels: { owner: "team" },
      },
    },
  ];
  service.containerLabels = [{ name: "container-only", value: "1" }];
  render(
    <MemoryRouter>
      <ServiceEditor initial={service} editing />
    </MemoryRouter>,
  );
  fireEvent.change(screen.getByLabelText("Comment"), {
    target: { value: "release & rollback" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Deploy changes" }));
  const request = mutate.mock.calls[0][0];
  expect(request.path).toBe("/api/services/id?comment=release+%26+rollback");
  expect(request.body.entrypoint).toEqual(service.entrypoint);
  expect(request.body.command).toEqual(service.command);
  expect(request.body.networks).toEqual(service.networks);
  expect(request.body.mounts).toEqual(service.mounts);
  expect(request.body.containerLabels).toEqual(service.containerLabels);
});

it("keeps random-name service creation available", () => {
  render(
    <MemoryRouter>
      <ServiceEditor initial={defaultService("nginx")} editing={false} />
    </MemoryRouter>,
  );
  fireEvent.click(screen.getByRole("button", { name: "Create service" }));
  expect(mutate.mock.calls[0][0]).toMatchObject({
    path: "/api/services",
    body: { serviceName: "", repository: { name: "nginx", tag: "" } },
  });
});
