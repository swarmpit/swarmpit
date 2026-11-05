import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StackForm } from "./stack-form";

const session = vi.hoisted(() => ({
  me: { username: "operator", role: "user" },
}));
vi.mock("@/features/session/session", () => ({ useSession: () => session }));
const stored =
  "# keep this comment\nservices:\n  app:\n    image: app:latest\n";
const engine = "services:\n  app:\n    image: app@sha256:123\n";
const previous = "# previous\nservices: {}\n";
let responses: Record<string, { status?: number; body: unknown }>;
let requests: { path: string; init?: RequestInit }[];

function renderForm(route = "/stacks/example/compose") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[route]}>
        <Routes>
          <Route path="/stacks/create" element={<StackForm />} />
          <Route path="/stacks/:name/:view" element={<StackForm />} />
          <Route path="/stacks/:name" element={<div>Stack detail</div>} />
          <Route path="/stacks" element={<div>Stack list</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}

beforeEach(() => {
  session.me.role = "user";
  requests = [];
  responses = {
    "/api/stacks/example/file": {
      body: {
        name: "example",
        spec: { compose: stored },
        previousSpec: { compose: previous },
      },
    },
    "/api/stacks/example/compose": {
      body: { name: "example", spec: { compose: engine } },
    },
    "/api/stacks/example/history": {
      body: [
        {
          at: "2026-11-01T10:00:00Z",
          by: "alice",
          comment: "first deployment",
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

describe("stack compose form", () => {
  it("uses stored compose verbatim and preserves edits across a query refresh", async () => {
    const client = renderForm();
    const editor = await screen.findByLabelText("Compose YAML");
    expect(editor).toHaveValue(stored);
    expect(requests.some((r) => r.path === "/api/stacks/example/compose")).toBe(
      false,
    );
    fireEvent.change(editor, { target: { value: stored + "# unsaved\n" } });
    await act(async () => {
      client.setQueryData(["api", "/api/stacks/example/file"], {
        name: "example",
        spec: { compose: "services: {}" },
      });
    });
    expect(editor).toHaveValue(stored + "# unsaved\n");
  });

  it("keeps the draft visible when a background refresh fails", async () => {
    const client = renderForm();
    await screen.findByLabelText("Compose YAML");
    fireEvent.change(screen.getByLabelText("Compose YAML"), {
      target: { value: "# unsaved\nservices: {}" },
    });
    responses["/api/stacks/example/file"] = {
      status: 503,
      body: { error: "Temporarily unavailable" },
    };
    await act(async () => {
      await client.refetchQueries({
        queryKey: ["api", "/api/stacks/example/file"],
      });
    });
    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Temporarily unavailable",
      ),
    );
    expect(screen.getByLabelText("Compose YAML")).toHaveValue(
      "# unsaved\nservices: {}",
    );
  });

  it("confirms replacement of dirty compose before selecting another source", async () => {
    renderForm();
    await screen.findByLabelText("Compose YAML");
    fireEvent.change(screen.getByLabelText("Compose YAML"), {
      target: { value: "# unsaved" },
    });
    fireEvent.change(screen.getByLabelText("Compose source"), {
      target: { value: "previous" },
    });
    expect(screen.getByLabelText("Compose YAML")).toHaveValue("# unsaved");
    fireEvent.click(screen.getByRole("button", { name: "Keep editing" }));
    expect(screen.getByLabelText("Compose YAML")).toHaveValue("# unsaved");
    fireEvent.change(screen.getByLabelText("Compose source"), {
      target: { value: "previous" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Replace draft" }));
    expect(screen.getByLabelText("Compose YAML")).toHaveValue(previous);
  });

  it("reconstructs only when the stored stackfile is absent", async () => {
    responses["/api/stacks/example/file"] = {
      status: 400,
      body: { error: "Stackfile not found" },
    };
    renderForm();
    expect(await screen.findByLabelText("Compose YAML")).toHaveValue(engine);
  });

  it("does not replace a denied stackfile read with reconstructed compose", async () => {
    responses["/api/stacks/example/file"] = {
      status: 403,
      body: { error: "Forbidden" },
    };
    renderForm();
    expect(await screen.findByRole("alert")).toHaveTextContent("Forbidden");
    expect(screen.queryByLabelText("Compose YAML")).not.toBeInTheDocument();
    expect(requests.some((r) => r.path === "/api/stacks/example/compose")).toBe(
      false,
    );
  });

  it("opens the previous version and can select history without deploying it", async () => {
    renderForm("/stacks/example/previous");
    const editor = await screen.findByLabelText("Compose YAML");
    expect(editor).toHaveValue(previous);
    await screen.findByRole("option", { name: /first deployment/ });
    fireEvent.change(screen.getByLabelText("Compose source"), {
      target: { value: "history-0" },
    });
    expect(editor).toHaveValue("# historic\nservices: {}\n");
    expect(
      requests.every((r) => !r.init?.method || r.init.method === "GET"),
    ).toBe(true);
  });

  it("submits full compose, skip-image-resolution and encoded change comment", async () => {
    renderForm();
    await screen.findByLabelText("Compose YAML");
    fireEvent.change(screen.getByLabelText("Change comment"), {
      target: { value: "keep & deploy" },
    });
    fireEvent.click(screen.getByLabelText("Skip image resolution"));
    fireEvent.click(screen.getByRole("button", { name: "Deploy" }));
    await screen.findByText("Stack detail");
    const request = requests.find((r) => r.init?.method === "POST");
    expect(request?.path).toBe("/api/stacks/example?comment=keep+%26+deploy");
    expect(JSON.parse(String(request?.init?.body))).toEqual({
      name: "example",
      spec: { compose: stored },
      skipImageResolve: true,
    });
  });

  it("creates a saved inactive stack from an uploaded text file without deploying", async () => {
    renderForm("/stacks/create");
    fireEvent.change(screen.getByLabelText("Stack name"), {
      target: { value: "saved" },
    });
    const file = new File([stored], "compose.yml", { type: "text/yaml" });
    Object.defineProperty(file, "text", { value: async () => stored });
    fireEvent.change(screen.getByLabelText("Upload compose file"), {
      target: { files: [file] },
    });
    await waitFor(() =>
      expect(screen.getByLabelText("Compose YAML")).toHaveValue(stored),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save stackfile" }));
    await screen.findByText("Stack list");
    expect(requests.filter((r) => r.init?.method === "POST")).toMatchObject([
      { path: "/api/stacks/saved/file" },
    ]);
  });

  it("activates inactive stacks through POST stack update with saved compose", async () => {
    renderForm("/stacks/example/activate");
    expect(await screen.findByLabelText("Compose YAML")).toHaveValue(stored);
    fireEvent.click(screen.getByRole("button", { name: "Activate stack" }));
    await screen.findByText("Stack detail");
    expect(requests.find((r) => r.init?.method === "POST")?.path).toBe(
      "/api/stacks/example",
    );
  });

  it("does not expose deployment controls to viewers", async () => {
    session.me.role = "viewer";
    renderForm();
    await screen.findByLabelText("Compose YAML");
    expect(
      screen.queryByRole("button", { name: "Deploy" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Compose YAML")).toHaveAttribute("readonly");
  });
});
