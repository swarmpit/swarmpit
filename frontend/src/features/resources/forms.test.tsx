import { beforeEach, afterEach, expect, it, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
  act,
  waitFor,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import type { Node } from "@/lib/types";
import { NodeForm } from "./forms";
import { selectOption } from "@/test-utils";
vi.mock("@/features/session/session", () => ({
  useSession: () => ({ me: { username: "admin", role: "admin" } }),
}));
const initial: Node = {
  id: "n1",
  version: 3,
  nodeName: "worker-1",
  role: "worker",
  availability: "active",
  labels: [{ name: "zone", value: "original" }],
  state: "ready",
  address: "10.0.0.1",
  engine: "27.0",
  arch: "amd64",
  os: "linux",
  leader: false,
  resources: { cpu: 2, memory: 2048 },
};
let serverNode: Node;
let getFailure: boolean;
let reads: number;
let writes: Record<string, unknown>[];
beforeEach(() => {
  serverNode = structuredClone(initial);
  getFailure = false;
  reads = 0;
  writes = [];
  vi.spyOn(window, "confirm").mockReturnValue(true);
  vi.stubGlobal(
    "fetch",
    vi.fn(async (path: string, options?: RequestInit) => {
      if (path !== "/api/nodes/n1")
        throw new Error("Unexpected request: " + path);
      if (options?.method === "POST") {
        const body = JSON.parse(String(options.body)) as Record<
          string,
          unknown
        >;
        writes.push(body);
        return body.version === serverNode.version
          ? new Response(null, { status: 200 })
          : new Response(JSON.stringify({ error: "update out of sequence" }), {
              status: 409,
            });
      }
      reads++;
      return getFailure
        ? new Response(JSON.stringify({ error: "Cannot reload node" }), {
            status: 400,
          })
        : new Response(JSON.stringify(serverNode), { status: 200 });
    }),
  );
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
function mount() {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: Infinity },
      mutations: { retry: false },
    },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/nodes/n1/edit"]}>
        <Routes>
          <Route path="/nodes/:id/edit" element={<NodeForm />} />
          <Route path="/nodes/n1" element={<p>Node saved</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return client;
}
async function editDraft() {
  await screen.findByLabelText("Availability");
  await selectOption("Availability", "drain");
  fireEvent.change(screen.getByLabelText("Value"), {
    target: { value: "draft" },
  });
}
async function backgroundRefresh(client: QueryClient) {
  serverNode = {
    ...initial,
    version: 4,
    availability: "pause",
    labels: [{ name: "zone", value: "server" }],
  };
  await act(async () => {
    await client.refetchQueries({ queryKey: ["api", "/api/nodes/n1"] });
  });
}
it("keeps dirty node fields and the original optimistic version through background refresh", async () => {
  const client = mount();
  await editDraft();
  await backgroundRefresh(client);
  expect(screen.getByLabelText("Availability")).toHaveTextContent("drain");
  expect(screen.getByLabelText("Value")).toHaveValue("draft");
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "update out of sequence",
  );
  expect(writes).toEqual([
    {
      nodeName: "worker-1",
      version: 3,
      role: "worker",
      availability: "drain",
      labels: [{ name: "zone", value: "draft" }],
    },
  ]);
  expect(
    screen.getByRole("button", { name: "Reload server values" }),
  ).toBeEnabled();
});
it("recovers from a conflict by fetching fresh fields and version only after confirmed reload", async () => {
  const client = mount();
  await editDraft();
  await backgroundRefresh(client);
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("update out of sequence");
  serverNode = {
    ...serverNode,
    version: 5,
    role: "manager",
    availability: "active",
    labels: [{ name: "zone", value: "latest" }],
  };
  fireEvent.click(screen.getByRole("button", { name: "Reload server values" }));
  await waitFor(() =>
    expect(screen.getByLabelText("Value")).toHaveValue("latest"),
  );
  expect(window.confirm).toHaveBeenCalledWith(
    expect.stringMatching(/discard.*unsaved/i),
  );
  expect(screen.getByLabelText("Availability")).toHaveTextContent("active");
  expect(screen.getByLabelText("Role")).toHaveTextContent("manager");
  expect(screen.queryByText("update out of sequence")).not.toBeInTheDocument();
  expect(reads).toBe(3);
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("Node saved");
  expect(writes[1]).toEqual({
    nodeName: "worker-1",
    version: 5,
    role: "manager",
    availability: "active",
    labels: [{ name: "zone", value: "latest" }],
  });
});
it("cancelling reload keeps the draft and original version without another fetch", async () => {
  const client = mount();
  await editDraft();
  await backgroundRefresh(client);
  vi.mocked(window.confirm).mockReturnValue(false);
  fireEvent.click(screen.getByRole("button", { name: "Reload server values" }));
  expect(reads).toBe(2);
  expect(screen.getByLabelText("Availability")).toHaveTextContent("drain");
  expect(screen.getByLabelText("Value")).toHaveValue("draft");
  fireEvent.click(screen.getByRole("button", { name: "Save changes" }));
  await screen.findByText("update out of sequence");
  expect(writes[0]?.version).toBe(3);
});
it("disables node choices while confirmed server values are reloading", async () => {
  mount();
  await editDraft();
  let finishReload!: (response: Response) => void;
  vi.mocked(fetch).mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      finishReload = resolve;
    }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Reload server values" }));
  const availability = screen.getByLabelText("Availability");
  const role = screen.getByLabelText("Role");
  expect(availability).toBeDisabled();
  expect(role).toBeDisabled();
  fireEvent.keyDown(availability, { key: "ArrowDown" });
  fireEvent.keyDown(role, { key: "ArrowDown" });
  expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  await act(async () => {
    finishReload(
      new Response(JSON.stringify({ ...initial, role: "manager" }), {
        status: 200,
      }),
    );
  });
  await waitFor(() => expect(role).toHaveTextContent("manager"));
  expect(role).toBeEnabled();
  expect(availability).toBeEnabled();
});

it("preserves the draft and displays a failed confirmed reload", async () => {
  mount();
  await editDraft();
  getFailure = true;
  fireEvent.click(screen.getByRole("button", { name: "Reload server values" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Cannot reload node",
  );
  expect(screen.getByLabelText("Availability")).toHaveTextContent("drain");
  expect(screen.getByLabelText("Value")).toHaveValue("draft");
  expect(
    screen.getByRole("button", { name: "Reload server values" }),
  ).toBeEnabled();
  expect(writes).toEqual([]);
});
