import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import { encodeText } from "@/lib/format";
import { ContentForm } from "./forms";
import { ConfigDetail } from "./detail";

vi.mock("@/features/session/session", () => ({
  useSession: () => ({ me: { username: "operator", role: "user" } }),
}));
// Exercise the form contract independently of the editor's contenteditable DOM.
vi.mock("@/components/config-value-field", () => ({
  ConfigValueField: ({ value, onChange, readOnly, disabled }: {
    value: string; onChange?: (value: string) => void; readOnly?: boolean; disabled?: boolean;
  }) => <textarea aria-label="Value" value={value} readOnly={readOnly}
    disabled={disabled} onChange={(event) => onChange?.(event.target.value)} />,
}));

let writes: unknown[];
let failure: boolean;
const raw = '\nmessage: "žluťoučký 🐳"\nvalue: "<literal>"\n';
beforeEach(() => {
  writes = [];
  failure = false;
  vi.stubGlobal("fetch", vi.fn(async (path: string, options?: RequestInit) => {
    if ((path === "/api/configs" || path === "/api/secrets") && options?.method === "POST") {
      writes.push(JSON.parse(String(options.body)));
      return failure
        ? new Response(JSON.stringify({ error: "Config already exists" }), { status: 409 })
        : new Response(JSON.stringify({ id: "saved/id" }), { status: 201 });
    }
    if (path === "/api/configs/existing")
      return new Response(JSON.stringify({ id: "existing", configName: "settings.yaml", data: encodeText(raw), version: 2 }), { status: 200 });
    if (path === "/api/configs/existing/services")
      return new Response("[]", { status: 200 });
    throw new Error("Unexpected request: " + path);
  }));
});
afterEach(() => vi.unstubAllGlobals());

function mount(path = "/configs/create", kind: "configs" | "secrets" = "configs") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity }, mutations: { retry: false } },
  });
  return render(<QueryClientProvider client={client}>
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={"/" + kind + "/create"} element={<ContentForm kind={kind} />} />
        <Route path="/configs/:id" element={<ConfigDetail />} />
        <Route path="/configs/saved%2Fid" element={<p>Config saved</p>} />
        <Route path="/secrets/saved%2Fid" element={<p>Secret saved</p>} />
      </Routes>
    </MemoryRouter>
  </QueryClientProvider>);
}
function submit() {
  fireEvent.submit(screen.getByRole("button", { name: "Create config" }).closest("form")!);
}

it("rejects an empty config value and sends edited Unicode text without formatting", async () => {
  mount();
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "settings.yaml" } });
  submit();
  expect(await screen.findByRole("alert")).toHaveTextContent("Config value is required.");
  expect(writes).toEqual([]);
  fireEvent.change(screen.getByRole("textbox", { name: "Value" }), { target: { value: raw } });
  submit();
  expect(await screen.findByText("Config saved")).toBeInTheDocument();
  expect(writes).toEqual([{ configName: "settings.yaml", data: encodeText(raw) }]);
});

it("keeps the draft and shows backend failure after correcting an empty value", async () => {
  mount();
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "settings.yaml" } });
  submit();
  await screen.findByText("Config value is required.");
  failure = true;
  fireEvent.change(screen.getByRole("textbox", { name: "Value" }), { target: { value: raw } });
  submit();
  expect(await screen.findByRole("alert")).toHaveTextContent("Config already exists");
  expect(screen.queryByText("Config value is required.")).not.toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: "Value" })).toHaveValue(raw);
  expect(screen.getByRole("button", { name: "Create config" })).toBeEnabled();
});

it("retains the text draft when changing upload mode and sends uploaded bytes unchanged", async () => {
  mount();
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "opaque.conf" } });
  fireEvent.change(screen.getByRole("textbox", { name: "Value" }), { target: { value: raw } });
  const file = new File(["unused"], "opaque.conf");
  const bytes = new Uint8Array([0, 255, 10, 195, 169]);
  Object.defineProperty(file, "arrayBuffer", { value: async () => bytes.buffer });
  const upload = screen.getByLabelText("Upload file (optional)");
  fireEvent.change(upload, { target: { files: [file] } });
  await screen.findByText("opaque.conf selected");
  expect(screen.queryByRole("textbox", { name: "Value" })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Use text instead" }));
  expect(screen.getByRole("textbox", { name: "Value" })).toHaveValue(raw);
  fireEvent.change(screen.getByRole("textbox", { name: "Value" }), { target: { value: "" } });
  fireEvent.change(upload, { target: { files: [file] } });
  await screen.findByText("opaque.conf selected");
  submit();
  await screen.findByText("Config saved");
  expect(writes).toEqual([{ configName: "opaque.conf", data: btoa(String.fromCharCode(...bytes)) }]);
});

it.each(["configs", "secrets"] as const)("blocks another %s upload and submission until the selected file finishes reading", async (kind) => {
  mount("/" + kind + "/create", kind);
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "opaque.conf" } });
  const upload = screen.getByLabelText("Upload file (optional)");
  const previous = new File(["previous"], "previous.conf");
  Object.defineProperty(previous, "arrayBuffer", { value: async () => new Uint8Array([1]).buffer });
  fireEvent.change(upload, { target: { files: [previous] } });
  await screen.findByText("previous.conf selected");

  let resolve!: (value: ArrayBuffer) => void;
  const reading = new Promise<ArrayBuffer>((done) => { resolve = done; });
  const selected = new File(["unused"], "opaque.conf");
  Object.defineProperty(selected, "arrayBuffer", { value: () => reading });
  fireEvent.change(upload, { target: { files: [selected] } });
  expect(upload).toBeDisabled();
  const useText = screen.getByRole("button", { name: "Use text instead" });
  expect(useText).toBeDisabled();
  fireEvent.click(useText);
  expect(screen.getByText("previous.conf selected")).toBeInTheDocument();
  const pending = screen.getByRole("button", { name: "Working…" });
  expect(pending).toBeDisabled();
  await act(async () => { fireEvent.submit(pending.closest("form")!); });
  expect(writes).toEqual([]);

  await act(async () => { resolve(new Uint8Array([0, 255, 10, 195, 169]).buffer); });
  await screen.findByText("opaque.conf selected");
  expect(upload).toBeEnabled();
  expect(screen.getByRole("button", { name: "Use text instead" })).toBeEnabled();
  const create = screen.getByRole("button", { name: kind === "configs" ? "Create config" : "Create secret" });
  expect(create).toBeEnabled();
  fireEvent.click(create);
  await screen.findByText(kind === "configs" ? "Config saved" : "Secret saved");
  expect(writes).toEqual([{ [kind === "configs" ? "configName" : "secretName"]: "opaque.conf", data: "AP8Kw6k=" }]);
});

it.each(["configs", "secrets"] as const)("allows another %s file selection after a read failure", async (kind) => {
  mount("/" + kind + "/create", kind);
  fireEvent.change(screen.getByLabelText("Name"), { target: { value: "recovered.conf" } });
  const upload = screen.getByLabelText("Upload file (optional)");
  const previous = new File(["previous"], "previous.conf");
  Object.defineProperty(previous, "arrayBuffer", { value: async () => new Uint8Array([1]).buffer });
  fireEvent.change(upload, { target: { files: [previous] } });
  await screen.findByText("previous.conf selected");
  let reject!: (reason: Error) => void;
  const reading = new Promise<ArrayBuffer>((_, fail) => { reject = fail; });
  const broken = new File(["unused"], "broken.conf");
  Object.defineProperty(broken, "arrayBuffer", { value: () => reading });
  fireEvent.change(upload, { target: { files: [broken] } });
  expect(upload).toBeDisabled();
  expect(screen.getByRole("button", { name: "Use text instead" })).toBeDisabled();
  await act(async () => { reject(new Error("Cannot read selected file")); });
  expect(await screen.findByRole("alert")).toHaveTextContent("Cannot read selected file");
  expect(upload).toBeEnabled();
  expect(screen.queryByRole("button", { name: "Use text instead" })).not.toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: kind === "configs" ? "Value" : "Contents" })).toBeEnabled();
  expect(screen.getByRole("button", { name: kind === "configs" ? "Create config" : "Create secret" })).toBeEnabled();
  expect(writes).toEqual([]);

  const recovered = new File(["unused"], "recovered.conf");
  Object.defineProperty(recovered, "arrayBuffer", { value: async () => new Uint8Array([255, 0]).buffer });
  fireEvent.change(upload, { target: { files: [recovered] } });
  await screen.findByText("recovered.conf selected");
  expect(screen.getByRole("button", { name: "Use text instead" })).toBeEnabled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: kind === "configs" ? "Create config" : "Create secret" }));
  await screen.findByText(kind === "configs" ? "Config saved" : "Secret saved");
  expect(writes).toEqual([{ [kind === "configs" ? "configName" : "secretName"]: "recovered.conf", data: "/wA=" }]);
});

it("displays decoded config values read-only without offering an update action", async () => {
  mount("/configs/existing");
  const value = await screen.findByRole("textbox", { name: "Value" });
  expect(value).toHaveValue(raw);
  expect(value).toHaveAttribute("readonly");
  expect(screen.queryByRole("button", { name: /Save|Update/ })).not.toBeInTheDocument();
  await waitFor(() => expect(screen.queryByText("Loading…")).not.toBeInTheDocument());
  expect(writes).toEqual([]);
});

it("leaves secret content entry as a required plain text field", () => {
  mount("/secrets/create", "secrets");
  expect(screen.getByRole("textbox", { name: "Contents" })).toBeRequired();
  expect(screen.queryByRole("textbox", { name: "Value" })).not.toBeInTheDocument();
});
