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
import { selectOption } from "@/test-utils";
import { StackForm } from "./stack-form";

const session = vi.hoisted(() => ({
  me: { username: "operator", role: "user" },
}));
vi.mock("@/features/session/session", () => ({ useSession: () => session }));
vi.mock("@/components/code-field", () => ({
  CodeField: ({ label, value, onChange, readOnly, disabled, required, rows, showSyntaxSelector }: {
    label?: string; value: string; onChange?: (value: string) => void;
    readOnly?: boolean; disabled?: boolean; required?: boolean; rows?: number; showSyntaxSelector?: boolean;
  }) => <textarea aria-label={label} value={value} onChange={(event) => onChange?.(event.target.value)}
    readOnly={readOnly} disabled={disabled} required={required} rows={rows}
    data-syntax-selector={String(showSyntaxSelector)} />,
}));

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
  it("keeps the compose page header and editor layout while initial compose loads", async () => {
    let finish!: () => void;
    const ready = new Promise<void>((resolve) => { finish = resolve; });
    const originalFetch = fetch;
    vi.stubGlobal("fetch", vi.fn(async (...args: Parameters<typeof fetch>) => {
      if (String(args[0]) === "/api/stacks/example/file") await ready;
      return originalFetch(...args);
    }));
    renderForm();
    const heading = screen.getByRole("heading", { name: "Edit stack" });
    const cancel = screen.getByRole("link", { name: "Cancel" });
    expect(screen.getByRole("status", { name: "Loading stack compose" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Compose configuration" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Deploy" })).not.toBeInTheDocument();
    await act(async () => { finish(); });
    expect(await screen.findByLabelText("Compose YAML")).toHaveValue(stored);
    expect(screen.getByRole("heading", { name: "Edit stack" })).toBe(heading);
    expect(screen.getByRole("link", { name: "Cancel" })).toBe(cancel);
    expect(screen.queryByRole("status", { name: "Loading stack compose" })).not.toBeInTheDocument();
  });

  it("uses stored compose verbatim and preserves edits across a query refresh", async () => {
    const client = renderForm();
    const editor = await screen.findByLabelText("Compose YAML");
    expect(editor).toHaveValue(stored);
    expect(editor).toHaveAttribute("data-syntax-selector", "false");
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
    await selectOption("Compose source", "Previously deployed");
    expect(screen.getByLabelText("Compose YAML")).toHaveValue("# unsaved");
    fireEvent.click(screen.getByRole("button", { name: "Keep editing" }));
    expect(screen.getByLabelText("Compose source")).toHaveTextContent(
      "Stored / last deployed",
    );
    expect(screen.getByLabelText("Compose YAML")).toHaveValue("# unsaved");
    await selectOption("Compose source", "Previously deployed");
    fireEvent.click(screen.getByRole("button", { name: "Replace draft" }));
    expect(screen.getByLabelText("Compose YAML")).toHaveValue(previous);
    expect(screen.getByLabelText("Compose source")).toHaveTextContent(
      "Previously deployed",
    );
  });

  it("reconstructs only when the stored stackfile is absent", async () => {
    responses["/api/stacks/example/file"] = {
      status: 400,
      body: { error: "Stackfile not found" },
    };
    renderForm();
    expect(await screen.findByLabelText("Compose YAML")).toHaveValue(engine);
  });

  it("disables unavailable stored sources while retaining the engine selection", async () => {
    responses["/api/stacks/example/file"] = {
      status: 400,
      body: { error: "Stackfile not found" },
    };
    renderForm();
    expect(await screen.findByLabelText("Compose YAML")).toHaveValue(engine);
    const source = screen.getByRole("combobox", { name: "Compose source" });
    expect(source).toHaveTextContent("Current engine state (reconstructed)");
    fireEvent.keyDown(source, { key: "ArrowDown" });
    expect(
      await screen.findByRole("option", { name: "Stored / last deployed" }),
    ).toHaveAttribute("aria-disabled", "true");
    expect(
      screen.getByRole("option", { name: "Previously deployed" }),
    ).toHaveAttribute("aria-disabled", "true");
  });

  it("fetches engine compose only after selecting that source", async () => {
    renderForm();
    await screen.findByLabelText("Compose YAML");
    expect(requests.some((r) => r.path === "/api/stacks/example/compose")).toBe(
      false,
    );
    await selectOption("Compose source", "Current engine state (reconstructed)");
    await waitFor(() =>
      expect(screen.getByLabelText("Compose YAML")).toHaveValue(engine),
    );
    expect(screen.getByLabelText("Compose source")).toHaveTextContent(
      "Current engine state (reconstructed)",
    );
    await selectOption("Compose source", "Stored / last deployed");
    expect(screen.getByLabelText("Compose YAML")).toHaveValue(stored);
    expect(
      requests.filter((r) => r.path === "/api/stacks/example/compose"),
    ).toHaveLength(1);
  });

  it("retains the uploaded source until another compose source is chosen", async () => {
    renderForm();
    await screen.findByLabelText("Compose YAML");
    const uploaded = "# uploaded\nservices: {}\n";
    const file = new File([uploaded], "compose.yml", { type: "text/yaml" });
    Object.defineProperty(file, "text", { value: async () => uploaded });
    fireEvent.change(screen.getByLabelText("Upload compose file"), {
      target: { files: [file] },
    });
    await waitFor(() =>
      expect(screen.getByLabelText("Compose YAML")).toHaveValue(uploaded),
    );
    expect(screen.getByLabelText("Compose source")).toHaveTextContent(
      "Uploaded file",
    );
    await selectOption("Compose source", "Stored / last deployed");
    expect(screen.getByLabelText("Compose YAML")).toHaveValue(stored);
    expect(screen.getByLabelText("Compose source")).toHaveTextContent(
      "Stored / last deployed",
    );
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
    await selectOption("Compose source", /first deployment/);
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

  it("passes pending state into the code editor and keeps the raw draft after a failed deployment", async () => {
    let finish!: () => void;
    const ready = new Promise<void>((resolve) => { finish = resolve; });
    const originalFetch = fetch;
    vi.stubGlobal("fetch", vi.fn(async (...args: Parameters<typeof fetch>) => {
      if (args[1]?.method === "POST") {
        requests.push({ path: String(args[0]), init: args[1] });
        await ready;
        return new Response(JSON.stringify({ error: "Deployment rejected" }), {
          status: 500, headers: { "Content-Type": "application/json" },
        });
      }
      return originalFetch(...args);
    }));
    renderForm();
    const editor = await screen.findByLabelText("Compose YAML");
    const draft = stored + '# preserve ${IMAGE:-nginx:latest} and Привет\n';
    fireEvent.change(editor, { target: { value: draft } });
    fireEvent.click(screen.getByRole("button", { name: "Deploy" }));
    await waitFor(() => expect(editor).toHaveAttribute("disabled"));
    expect(editor).toHaveValue(draft);
    await act(async () => { finish(); });
    expect(await screen.findByRole("alert")).toHaveTextContent("Deployment rejected");
    await waitFor(() => expect(editor).not.toHaveAttribute("disabled"));
    expect(editor).toHaveValue(draft);
    expect(JSON.parse(String(requests.find((request) => request.init?.method === "POST")?.init?.body)).spec.compose).toBe(draft);
  });

  it("shows raw YAML comparison blocks and restores only the selected engine lines", async () => {
    renderForm();
    const editor = await screen.findByLabelText("Compose YAML");
    fireEvent.click(screen.getByLabelText("Show current engine compose"));
    expect(await screen.findByLabelText("Current engine compose")).toHaveValue(engine);
    const removed = screen.getAllByLabelText("Engine lines removed or replaced");
    const added = screen.getAllByLabelText("Draft lines added or changed");
    expect(removed[0]).toHaveValue("    image: app@sha256:123");
    expect(removed[0]).toHaveAttribute("readonly");
    expect(added[0]).toHaveValue("# keep this comment");
    expect(added[0]).toHaveAttribute("data-syntax-selector", "false");
    fireEvent.click(screen.getByRole("button", { name: "Restore engine lines at 1" }));
    expect(editor).toHaveValue(stored.replace("# keep this comment\n", ""));
    expect(requests.some((request) => request.init?.method === "POST")).toBe(false);
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
