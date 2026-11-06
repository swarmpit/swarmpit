import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router";
import { App } from "./app";
import { SessionProvider } from "@/features/session/session";
import { TooltipProvider } from "@/components/ui/tooltip";
import { saveToken } from "@/lib/api";

vi.mock("@/lib/events", () => ({ connectEvents: () => () => {} }));

let respond: Map<string, (value: unknown) => void>;
beforeEach(() => {
  localStorage.clear();
  saveToken("Bearer loading-review");
  document.cookie = "sidebar_state=; max-age=0; path=/";
  Object.defineProperty(window, "innerWidth", { value: 1024, configurable: true });
  vi.stubGlobal("matchMedia", () => ({ matches: false, addEventListener: vi.fn(), removeEventListener: vi.fn() }));
  respond = new Map();
  vi.stubGlobal("fetch", vi.fn((path: string) => {
    if (path === "/version" || path === "/api/me") return new Promise<Response>((resolve) => {
      respond.set(path, (value) => resolve(new Response(JSON.stringify(value), { status: 200 })));
    });
    if (path === "/api/services") return Promise.resolve(new Response("[]", { status: 200 }));
    throw new Error("Protected request mounted too early: " + path);
  }));
});
afterEach(() => {
  vi.unstubAllGlobals();
  document.documentElement.classList.remove("dark");
});
function mount(path = "/services") {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity } } });
  return render(<QueryClientProvider client={client}>
    <MemoryRouter initialEntries={[path]}>
      <SessionProvider><TooltipProvider><App /></TooltipProvider></SessionProvider>
    </MemoryRouter>
  </QueryClientProvider>);
}
async function answer(path: string, value: unknown) {
  await act(async () => respond.get(path)!(value));
}

it("keeps the actual shell and sidebar preference through session loading without mounting protected routes", async () => {
  const view = mount();
  const main = view.container.querySelector('[data-slot="sidebar-inset"]');
  const sidebar = view.container.querySelector('[data-slot="sidebar"]');
  const pane = document.getElementById("main-content");
  expect(pane).toBeInTheDocument();
  expect(main).toContainElement(pane);
  expect(main).toBeInTheDocument();
  expect(sidebar).toBeInTheDocument();
  expect(screen.getByRole("status", { name: "Workspace loading" })).toHaveAttribute("aria-busy", "true");
  expect(screen.getAllByRole("status")).toHaveLength(1);
  expect(within(screen.getByRole("navigation", { name: "Main navigation" })).queryByRole("link")).not.toBeInTheDocument();
  expect(screen.queryByRole("button", { name: /User menu/ })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: /^Swagger API/ })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Classic UI" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Create service" })).not.toBeInTheDocument();
  expect(vi.mocked(fetch).mock.calls.map(([path]) => path).sort()).toEqual(["/api/me", "/version"]);
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");

  await answer("/version", { initialized: true, version: "1.11", statistics: false, docker: {} });
  expect(screen.getByRole("status", { name: "Workspace loading" })).toBeInTheDocument();
  expect(screen.queryByRole("link", { name: /^Swagger API/ })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Classic UI" })).not.toBeInTheDocument();
  expect(fetch).not.toHaveBeenCalledWith("/api/services", expect.anything());
  await answer("/api/me", { username: "review", role: "user" });

  expect(await screen.findByRole("heading", { name: "Services" })).toBeInTheDocument();
  await waitFor(() => expect(screen.queryByRole("status", { name: "Workspace loading" })).not.toBeInTheDocument());
  expect(view.container.querySelector('[data-slot="sidebar-inset"]')).toBe(main);
  expect(view.container.querySelector('[data-slot="sidebar"]')).toBe(sidebar);
  expect(document.getElementById("main-content")).toBe(pane);
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
  expect(screen.getByRole("button", { name: "User menu for review" })).toBeInTheDocument();
  const resources = within(screen.getByRole("navigation", { name: "Resources" }));
  expect(resources.getByRole("link", { name: "Swagger API (opens in a new tab)" })).toHaveAttribute("href", "/api-docs");
  expect(resources.getByRole("link", { name: "Classic UI" })).toHaveAttribute("href", "/?ui=classic");
  expect(within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Services" })).toBeInTheDocument();
  expect(vi.mocked(fetch).mock.calls.some(([path]) => path === "/api/services")).toBe(true);
});

it("uses overview summary and chart shapes inside the shared frame while identity is loading", () => {
  document.cookie = "sidebar_state=false; path=/";
  const view = mount("/");
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
  const status = screen.getByRole("status", { name: "Workspace loading" });
  const summary = status.querySelector('[data-slot="loading-overview-summary"]');
  expect(summary?.children).toHaveLength(4);
  expect(within(status).queryByRole("textbox")).not.toBeInTheDocument();
  for (const skeleton of view.container.querySelectorAll('[data-slot="skeleton"]')) {
    expect(skeleton).toHaveAttribute("aria-hidden", "true");
  }
  expect(vi.mocked(fetch).mock.calls.map(([path]) => path).sort()).toEqual(["/api/me", "/version"]);
});

it("shows a sign-in form skeleton rather than an authenticated shell when signed out", async () => {
  saveToken(null);
  mount();
  const loading = await screen.findByRole("status", { name: "Sign-in form loading" });
  expect(loading.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(5);
  expect(screen.queryByRole("status", { name: "Workspace loading" })).not.toBeInTheDocument();
  expect(screen.queryByRole("navigation", { name: "Main navigation" })).not.toBeInTheDocument();
  expect(screen.queryByRole("textbox", { name: "Username" })).not.toBeInTheDocument();
  await answer("/version", { initialized: true, version: "1.11", statistics: false, docker: {} });
  expect(await screen.findByRole("textbox", { name: "Username" })).toBeRequired();
  expect(screen.getByLabelText("Password")).toBeRequired();
  expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  expect(vi.mocked(fetch).mock.calls.map(([path]) => path)).toEqual(["/version"]);
});
