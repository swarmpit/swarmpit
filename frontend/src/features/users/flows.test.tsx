import { beforeEach, afterEach, expect, it, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Routes, Route } from "react-router";
import { UserForm, UserDetail } from "./users";
import { RegistryForm, RegistriesList } from "../registries/registries";
import { Account } from "../session/account";
const identity = vi.hoisted(() => ({
  username: "admin",
  email: "admin@example.com",
  role: "admin",
}));
vi.mock("../session/session", () => ({ useSession: () => ({ me: identity }) }));
let calls: { path: string; method: string; body: unknown }[];
let user = {
  _id: "u1",
  _rev: "3",
  type: "user",
  username: "alice",
  email: "old@example.com",
  role: "viewer",
  "node-dashboard": ["node1"],
  "api-token": { jti: "j", mask: "mask", expiresAt: "2027-01-01T00:00:00Z" },
};
function mount(page: React.ReactNode, path: string, route: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path={route} element={page} />
          {route !== "/users/:id" && (
            <Route path="/users/u1" element={<p>User saved</p>} />
          )}
          <Route path="/registries/ecr/new" element={<p>Registry saved</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
beforeEach(() => {
  identity.role = "admin";
  calls = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      const method = init?.method || "GET";
      calls.push({
        path,
        method,
        body: init?.body ? JSON.parse(String(init.body)) : undefined,
      });
      let data: unknown = {};
      if (path === "/api/admin/users/u1") data = user;
      else if (path === "/api/me")
        data = {
          ...identity,
          "api-token": {
            jti: "j",
            mask: "old",
            expiresAt: "2027-01-01T00:00:00Z",
          },
        };
      else if (path === "/api-token" && method === "POST")
        data = {
          token: "Bearer newly-generated",
          expiresAt: "2027-02-01T00:00:00Z",
        };
      else if (path === "/api/registry/ecr" && method === "POST")
        data = { id: "new" };
      else if (path.startsWith("/api/registry/")) data = [];
      return new Response(JSON.stringify(data), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());
it("keeps dirty user fields during refresh and submits backend metadata without password", async () => {
  const client = mount(<UserForm />, "/users/u1/edit", "/users/:id/edit");
  const email = await screen.findByLabelText("Email");
  fireEvent.change(email, { target: { value: "edited@example.com" } });
  await act(async () => {
    client.setQueryData(["api", "/api/admin/users/u1"], {
      ...user,
      _rev: "4",
      email: "refreshed@example.com",
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  expect(email).toHaveValue("edited@example.com");
  fireEvent.change(screen.getByLabelText("Role"), {
    target: { value: "user" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("User saved");
  expect(calls.find((c) => c.method === "POST")).toMatchObject({
    path: "/api/admin/users/u1",
    body: {
      _id: "u1",
      _rev: "4",
      type: "user",
      username: "alice",
      email: "edited@example.com",
      role: "user",
      "node-dashboard": ["node1"],
      "api-token": {
        jti: "j",
        mask: "mask",
        expiresAt: "2027-01-01T00:00:00Z",
      },
    },
  });
  expect(calls.find((c) => c.method === "POST")?.body).not.toHaveProperty(
    "password",
  );
});
it("does not fetch admin user details for viewers", () => {
  identity.role = "viewer";
  mount(<UserDetail />, "/users/u1", "/users/:id");
  expect(screen.getByRole("alert")).toHaveTextContent(/administrator/i);
  expect(calls).toEqual([]);
});
it("creates ECR with the accessKey field and handles an id response", async () => {
  mount(<RegistryForm />, "/registries/create", "/registries/create");
  fireEvent.change(screen.getByLabelText("Registry type"), {
    target: { value: "ecr" },
  });
  fireEvent.change(screen.getByLabelText("Access key ID"), {
    target: { value: "key-id" },
  });
  fireEvent.change(screen.getByLabelText("Secret access key"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Link registry" }));
  await screen.findByText("Registry saved");
  expect(calls.find((c) => c.method === "POST")).toMatchObject({
    path: "/api/registry/ecr",
    body: {
      user: "swarmpit-ecr",
      region: "eu-west-1",
      accessKeyId: "key-id",
      accessKey: "secret",
      public: false,
    },
  });
});
it("lists public registry ownership without linking another owners credentials", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async (path: string) =>
        new Response(
          JSON.stringify(
            path === "/api/registry/v2"
              ? [
                  {
                    _id: "shared",
                    type: "v2",
                    owner: "someone",
                    name: "Shared",
                    url: "https://ghcr.io",
                    public: true,
                  },
                ]
              : [],
          ),
          { status: 200 },
        ),
    ),
  );
  mount(<RegistriesList />, "/registries", "/registries");
  await screen.findByText("Shared");
  expect(
    screen.queryByRole("link", { name: "Shared" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText("someone")).toBeInTheDocument();
});
it("validates password confirmation, sends password keys and clears secrets after success", async () => {
  mount(<Account />, "/account", "/account");
  fireEvent.change(screen.getByLabelText("Current password"), {
    target: { value: "current" },
  });
  fireEvent.change(screen.getByLabelText("New password"), {
    target: { value: "new-secret" },
  });
  fireEvent.change(screen.getByLabelText("Confirm password"), {
    target: { value: "different" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Change password" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(/match/i);
  expect(calls.filter((c) => c.path === "/password")).toHaveLength(0);
  fireEvent.change(screen.getByLabelText("Confirm password"), {
    target: { value: "new-secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Change password" }));
  await screen.findByText("Password changed.");
  expect(calls.find((c) => c.path === "/password")).toMatchObject({
    method: "POST",
    body: {
      password: "current",
      "new-password": "new-secret",
      "confirm-password": "new-secret",
    },
  });
  expect(screen.getByLabelText("Current password")).toHaveValue("");
});

it("hides a generated token permanently without leaving a second hide action", async () => {
  mount(<Account />, "/account", "/account");
  await screen.findByDisplayValue("Bearer ...old");
  fireEvent.click(screen.getByRole("button", { name: "Regenerate API token" }));
  fireEvent.click(await screen.findByRole("button", { name: "Replace token" }));
  await screen.findByDisplayValue("Bearer newly-generated");
  fireEvent.click(screen.getByRole("button", { name: "Hide token" }));
  expect(
    screen.queryByDisplayValue("Bearer newly-generated"),
  ).not.toBeInTheDocument();
  expect(screen.getByDisplayValue("Bearer ...rated")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Hide token" }),
  ).not.toBeInTheDocument();
});
it("shows registry backend failures without retrying or discarding edited credentials", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async () =>
        new Response(
          JSON.stringify({ error: "Invalid registry credentials" }),
          { status: 400 },
        ),
    ),
  );
  mount(<RegistryForm />, "/registries/create", "/registries/create");
  fireEvent.change(screen.getByLabelText("Username"), {
    target: { value: "deploy" },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "bad-password" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Link registry" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Invalid registry credentials",
  );
  expect(screen.getByLabelText("Password")).toHaveValue("bad-password");
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(screen.getByRole("button", { name: "Link registry" })).toBeEnabled();
});
it("keeps registry edits on refresh and omits an unchanged secret", async () => {
  const record = {
    _id: "new",
    _rev: "2",
    type: "ecr",
    owner: "admin",
    user: "deploy",
    region: "eu-west-1",
    accessKeyId: "old-key",
    public: false,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, init?: RequestInit) => {
      const method = init?.method || "GET";
      calls.push({
        path,
        method,
        body: init?.body ? JSON.parse(String(init.body)) : undefined,
      });
      return new Response(JSON.stringify(record), { status: 200 });
    }),
  );
  const client = mount(
    <RegistryForm />,
    "/registries/ecr/new/edit",
    "/registries/:registryType/:id/edit",
  );
  const key = await screen.findByLabelText("Access key ID");
  fireEvent.change(key, { target: { value: "changed-key" } });
  await act(async () => {
    client.setQueryData(["api", "/api/registry/ecr/new"], {
      ...record,
      _rev: "3",
      accessKeyId: "refreshed-key",
    });
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
  expect(key).toHaveValue("changed-key");
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("Registry saved");
  expect(calls.find((c) => c.method === "POST")).toMatchObject({
    path: "/api/registry/ecr/new",
    body: {
      _id: "new",
      _rev: "3",
      user: "deploy",
      region: "eu-west-1",
      accessKeyId: "changed-key",
      public: false,
    },
  });
  expect(calls.find((c) => c.method === "POST")?.body).not.toHaveProperty(
    "accessKey",
  );
});

it("presents generated API token once and revokes it only after confirmation", async () => {
  mount(<Account />, "/account", "/account");
  await screen.findByDisplayValue("Bearer ...old");
  fireEvent.click(screen.getByRole("button", { name: "Regenerate API token" }));
  fireEvent.click(await screen.findByRole("button", { name: "Replace token" }));
  await screen.findByDisplayValue("Bearer newly-generated");
  expect(screen.getByText(/2027/)).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Revoke API token" }));
  expect(
    calls.filter((c) => c.path === "/api-token" && c.method === "DELETE"),
  ).toHaveLength(0);
  fireEvent.click(await screen.findByRole("button", { name: "Revoke token" }));
  await waitFor(() =>
    expect(
      screen.queryByDisplayValue("Bearer newly-generated"),
    ).not.toBeInTheDocument(),
  );
  expect(
    calls.find((c) => c.path === "/api-token" && c.method === "DELETE"),
  ).toBeDefined();
});
