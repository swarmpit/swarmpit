import { beforeEach, afterEach, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SessionProvider, useSession } from "./session";
import { saveToken } from "@/lib/api";
import { apiKey } from "@/lib/queries";
const me = { username: "admin", role: "admin", email: "" };
beforeEach(() => {
  localStorage.clear();
  saveToken("Bearer session");
  vi.stubGlobal(
    "fetch",
    vi.fn(
      async (path) =>
        new Response(
          JSON.stringify(
            String(path) === "/version"
              ? { initialized: true, version: "test", docker: {} }
              : me,
          ),
        ),
    ),
  );
});
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
function Harness() {
  const s = useSession();
  return (
    <>
      <p>{s.token ? "signed in" : "signed out"}</p>
      <p>{s.notice}</p>
      <button onClick={() => void s.logout().catch(() => {})}>Logout</button>
    </>
  );
}
function mount() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <SessionProvider>
        <Harness />
      </SessionProvider>
    </QueryClientProvider>,
  );
  return client;
}
it("clears protected state and cache even when browser storage removal fails", async () => {
  const client = mount();
  await waitFor(() =>
    expect(client.getQueryData(apiKey("/api/me"))).toEqual(me),
  );
  vi.spyOn(Storage.prototype, "removeItem").mockImplementation(() => {
    throw new Error("blocked");
  });
  fireEvent.click(screen.getByText("Logout"));
  await screen.findByText("signed out");
  expect(client.getQueryData(apiKey("/api/me"))).toBeUndefined();
});
it("signs out locally immediately and forwards the captured token for revocation", async () => {
  const client = mount();
  await waitFor(() =>
    expect(client.getQueryData(apiKey("/api/me"))).toEqual(me),
  );
  let complete!: () => void;
  vi.mocked(fetch).mockImplementation(async (path, init) => {
    if (String(path) === "/logout") {
      expect(new Headers(init?.headers).get("Authorization")).toBe(
        "Bearer session",
      );
      await new Promise<void>((r) => {
        complete = r;
      });
      return new Response(null, { status: 200 });
    }
    return new Response(
      JSON.stringify({ initialized: true, version: "test", docker: {} }),
    );
  });
  fireEvent.click(screen.getByText("Logout"));
  await screen.findByText("signed out");
  expect(client.getQueryData(apiKey("/api/me"))).toBeUndefined();
  complete();
});
