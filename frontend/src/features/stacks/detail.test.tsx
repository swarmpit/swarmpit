import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StackDetail } from "./detail";

const session = vi.hoisted(() => ({
  me: { username: "operator", role: "user" },
}));
vi.mock("@/features/session/session", () => ({ useSession: () => session }));
vi.mock("@/features/resources/lists", () => ({
  Live: () => <span>Live</span>,
}));
let responses: Record<string, { status?: number; body: unknown }>;
let requests: { path: string; init?: RequestInit }[];
function renderDetail() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/stacks/example"]}>
        <Routes>
          <Route path="/stacks/:name" element={<StackDetail />} />
          <Route path="/stacks" element={<div>Stack list</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  session.me.role = "user";
  requests = [];
  responses = {
    "/api/stacks": {
      body: [
        {
          stackName: "example",
          state: "deployed",
          services: [{ id: "svc", serviceName: "example_app" }],
          networks: [],
          volumes: [],
          secrets: [],
          configs: [],
        },
      ],
    },
    "/api/stacks/example/file": {
      body: {
        name: "example",
        spec: { compose: "# stored\nservices: {}\n" },
        previousSpec: { compose: "# previous\nservices: {}\n" },
      },
    },
    "/api/stacks/example/compose": {
      body: { name: "example", spec: { compose: "services: {}\n" } },
    },
    "/api/stacks/example/networks": {
      body: [
        {
          id: "network",
          networkName: "example_default",
          driver: "overlay",
          ipam: {},
        },
      ],
    },
    "/api/stacks/example/history": {
      body: [
        {
          at: "2026-11-01",
          by: "operator",
          comment: "First version",
          spec: { compose: "# historic\nservices: {}\n" },
        },
      ],
    },
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
      const path = String(input);
      requests.push({ path, init });
      const response =
        init?.method && init.method !== "GET"
          ? { body: undefined }
          : responses[path] || {
              status: 404,
              body: { error: "Unexpected request: " + path },
            };
      return new Response(
        response.body === undefined ? null : JSON.stringify(response.body),
        {
          status: response.status || 200,
          headers: {
            "Content-Type": "application/json",
            "X-Backend-Server": "swarmpit",
          },
        },
      );
    }),
  );
});
describe("stack details and lifecycle", () => {
  it("derives inactive details from the list and offers activation and saved-file deletion", async () => {
    responses["/api/stacks"] = {
      body: [{ stackName: "example", state: "inactive" }],
    };
    renderDetail();
    expect(
      await screen.findByRole("link", { name: "Activate stack" }),
    ).toHaveAttribute("href", "/stacks/example/activate");
    expect(
      await screen.findByRole("button", { name: "Delete stackfile" }),
    ).toBeEnabled();
    expect(requests.some((r) => r.path === "/api/stacks/example")).toBe(false);
    expect(
      requests.some((r) =>
        /\/(services|tasks|networks|volumes|configs|secrets)$/.test(r.path),
      ),
    ).toBe(false);
  });

  it("confirms redeploy before posting and includes the change comment", async () => {
    renderDetail();
    const redeploy = await screen.findByRole("button", { name: "Redeploy" });
    await waitFor(() => expect(redeploy).toBeEnabled());
    fireEvent.change(screen.getByLabelText("Change comment"), {
      target: { value: "re & deploy" },
    });
    fireEvent.click(redeploy);
    expect(requests.filter((r) => r.init?.method === "POST")).toHaveLength(0);
    fireEvent.click(
      screen.getAllByRole("button", { name: "Redeploy" }).at(-1)!,
    );
    await waitFor(() =>
      expect(requests.find((r) => r.init?.method === "POST")?.path).toBe(
        "/api/stacks/example/redeploy?comment=re+%26+deploy",
      ),
    );
  });

  it("disables saved-file operations after deleting a stackfile even when its old query data is cached", async () => {
    renderDetail();
    const redeploy = await screen.findByRole("button", { name: "Redeploy" });
    await waitFor(() => expect(redeploy).toBeEnabled());
    responses["/api/stacks/example/file"] = {
      status: 400,
      body: { error: "Stackfile not found" },
    };
    fireEvent.click(screen.getByRole("button", { name: "Delete stackfile" }));
    fireEvent.click(
      screen.getAllByRole("button", { name: "Delete stackfile" }).at(-1)!,
    );
    await waitFor(() => expect(redeploy).toBeDisabled());
  });

  it("shows related resources from their own backend endpoint", async () => {
    renderDetail();
    await screen.findByRole("heading", { name: "example" });
    fireEvent.mouseDown(screen.getByRole("tab", { name: "Networks" }), {
      button: 0,
      ctrlKey: false,
    });
    expect(
      await screen.findByRole("link", { name: "example_default" }),
    ).toHaveAttribute("href", "/networks/network");
  });

  it("shows history compose without changing the saved version", async () => {
    renderDetail();
    await screen.findByRole("heading", { name: "example" });
    fireEvent.mouseDown(screen.getByRole("tab", { name: "History" }), {
      button: 0,
      ctrlKey: false,
    });
    const version = await screen.findByRole("button", {
      name: /First version/,
    });
    fireEvent.click(version);
    expect(screen.getByLabelText("Historical compose")).toHaveValue(
      "# historic\nservices: {}\n",
    );
    expect(
      screen.getByRole("link", { name: "Edit this version" }),
    ).toHaveAttribute("href", "/stacks/example/compose?history=0");
    expect(requests.filter((r) => r.init?.method === "POST")).toHaveLength(0);
  });

  it("keeps operations available when optional resource metrics are unavailable", async () => {
    responses["/api/stacks/example/tasks"] = { body: [] };
    responses["/api/stats"] = {
      status: 503,
      body: { error: "Stats unavailable" },
    };
    renderDetail();
    expect(
      await screen.findByText("Resource metrics are currently unavailable."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Redeploy" })).toBeEnabled();
  });

  it("hides lifecycle mutations for viewers", async () => {
    session.me.role = "viewer";
    renderDetail();
    await screen.findByRole("heading", { name: "example" });
    expect(
      screen.queryByRole("button", { name: "Redeploy" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Delete stack" }),
    ).not.toBeInTheDocument();
  });
});
