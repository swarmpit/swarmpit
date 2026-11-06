import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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
    if (path === "/api/configs" && options?.method === "POST") {
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
