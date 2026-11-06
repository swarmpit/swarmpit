import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { act, render, screen, fireEvent, within, waitFor } from "@testing-library/react";
import { HashRouter, Routes, Route } from "react-router";
import { Shell } from "./shell";
const session = vi.hoisted(() => ({ role: "admin", version: "1.11-SNAPSHOT" as string | undefined, logout: vi.fn() }));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "a", role: session.role },
    version: { version: session.version, instanceName: "Review swarm", docker: { engine: "29.0.1" } },
    logout: session.logout,
  }),
}));
beforeEach(() => {
  localStorage.removeItem("swarmpit.theme");
  document.documentElement.classList.remove("dark");
  session.role = "admin";
  session.version = "1.11-SNAPSHOT";
  session.logout.mockReset().mockResolvedValue(undefined);
  document.cookie = "sidebar_state=; max-age=0; path=/";
  window.history.replaceState(null, "", "/react/#/services");
  Object.defineProperty(window, "innerWidth", { value: 1024, configurable: true });
  vi.stubGlobal("ResizeObserver", class {
    observe() {}
    unobserve() {}
    disconnect() {}
  });
  vi.stubGlobal("matchMedia", () => ({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
});
afterEach(() => document.head.querySelector("meta[data-shell-theme-test]")?.remove());
function showShell() {
  return render(
    <HashRouter>
      <Routes>
        <Route element={<Shell />}>
          <Route index element={<p>Overview content</p>} />
          <Route path="/services" element={<p>Services content</p>} />
          <Route path="/services/:id/edit" element={<p>Edit service content</p>} />
          <Route path="/nodes" element={<p>Nodes content</p>} />
          <Route path="/account-settings" element={<p>Account content</p>} />
          <Route path="*" element={<p>Unknown route content</p>} />
        </Route>
      </Routes>
    </HashRouter>,
  );
}
it.each(["1.11-SNAPSHOT", "v1.11-SNAPSHOT"])("links the sidebar brand to the Swarmpit website with app version %s", async (version) => {
  session.version = version;
  showShell();
  const home = screen.getByRole("link", { name: "Swarmpit v1.11-SNAPSHOT website" });
  expect(within(home).getByText("Swarmpit")).toBeInTheDocument();
  expect(screen.getAllByText("v1.11-SNAPSHOT")).toHaveLength(1);
  expect(home).toHaveAttribute("href", "https://swarmpit.io/");
  expect(home).toHaveAttribute("title", "Docker Swarm 29.0.1");
  expect(document.title).toBe("Review swarm · Swarm management");
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  fireEvent.focus(home);
  expect(await screen.findByRole("tooltip")).toHaveTextContent("Swarmpit v1.11-SNAPSHOT");
});
it.each(["", undefined])("keeps the brand without inventing an unavailable version (%s)", (version) => {
  session.version = version;
  showShell();
  const home = screen.getByRole("link", { name: "Swarmpit website" });
  expect(home).toHaveTextContent("Swarmpit");
  expect(home).not.toHaveTextContent("undefined");
  expect(home).not.toHaveTextContent("vundefined");
});
it("skip navigation focuses content without replacing the hash route", () => {
  showShell();
  fireEvent.click(screen.getByText("Skip to content"));
  expect(document.getElementById("main-content")).toHaveFocus();
  expect(window.location.hash).toBe("#/services");
});
it("keeps the header outside the content pane and preserves its scroll until the pathname changes", async () => {
  const view = showShell();
  const pane = document.getElementById("main-content")!;
  const header = view.container.querySelector("header")!;
  const inset = view.container.querySelector('[data-slot="sidebar-inset"]')!;
  expect(inset).toContainElement(header);
  expect(inset).toContainElement(pane);
  expect(pane).not.toContainElement(header);

  pane.scrollTop = 420;
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  expect(document.getElementById("main-content")).toBe(pane);
  expect(pane.scrollTop).toBe(420);
  fireEvent.click(within(screen.getByRole("navigation", { name: "Breadcrumb" })).getByRole("link", { name: "Services" }));
  await waitFor(() => expect(window.location.hash).toBe("#/services"));
  expect(pane.scrollTop).toBe(420);

  fireEvent.click(within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Nodes" }));
  expect(await screen.findByText("Nodes content")).toBeInTheDocument();
  expect(document.getElementById("main-content")).toBe(pane);
  expect(pane.scrollTop).toBe(0);
  expect(window.location.hash).toBe("#/nodes");
});
it("collapses desktop navigation to an icon rail without losing the current route or links", () => {
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
  expect(within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Services" })).toHaveAttribute("aria-current", "page");
  expect(screen.getByText("Services content")).toBeInTheDocument();
  expect(window.location.hash).toBe("#/services");
  fireEvent.click(screen.getByRole("button", { name: "Expand sidebar" }));
  expect(screen.getByRole("button", { name: "Collapse sidebar" })).toHaveAttribute("aria-expanded", "true");
});
it("restores the saved collapsed desktop preference after remount", () => {
  const view = showShell();
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  view.unmount();
  showShell();
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
});
it("supports the sidebar keyboard toggle without changing the hash route", () => {
  showShell();
  fireEvent.keyDown(window, { key: "b", ctrlKey: true });
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
  expect(window.location.hash).toBe("#/services");
});
it("keeps viewer restrictions when navigation is collapsed", () => {
  session.role = "viewer";
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  expect(within(screen.getByRole("navigation", { name: "Main navigation" })).getByRole("link", { name: "Services" })).toBeInTheDocument();
  for (const name of ["Users", "Secrets", "Registries"]) expect(screen.queryByRole("link", { name })).not.toBeInTheDocument();
});
it("closes the mobile drawer on navigation and restores focus to its trigger", async () => {
  Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Open navigation" }));
  const drawer = screen.getByRole("dialog");
  fireEvent.click(within(drawer).getByRole("link", { name: "Nodes" }));
  await screen.findByText("Nodes content");
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(screen.getByRole("button", { name: "Open navigation" })).toHaveFocus();
});
it.each(["admin", "user", "viewer"])("keeps native resource links above the user panel for %s", async (role) => {
  session.role = role;
  showShell();
  const resources = screen.getByRole("navigation", { name: "Resources" });
  const swagger = within(resources).getByRole("link", { name: "Swagger API (opens in a new tab)" });
  const classic = within(resources).getByRole("link", { name: "Classic UI" });
  expect(resources.closest('[data-slot="sidebar-content"]')).toBeInTheDocument();
  expect(resources.closest('[data-slot="sidebar-footer"]')).toBeNull();
  expect(resources.closest('[data-slot="sidebar-group"]')).toHaveClass("mt-auto");
  expect(swagger).toHaveAttribute("href", "/api-docs");
  expect(swagger).toHaveAttribute("target", "_blank");
  expect(swagger).toHaveAttribute("rel", "noopener noreferrer");
  expect(classic).toHaveAttribute("href", "/?ui=classic");
  expect(classic).not.toHaveAttribute("target");
  for (const link of [swagger, classic]) {
    expect(link.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
  }
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  for (const [link, label] of [[swagger, "Swagger API (opens in a new tab)"], [classic, "Classic UI"]] as const) {
    fireEvent.focus(link);
    await waitFor(() => expect(screen.getByRole("tooltip")).toHaveTextContent(label));
    fireEvent.blur(link);
  }
  expect(window.location.hash).toBe("#/services");
});
it("keeps sidebar account navigation available when collapsed", async () => {
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  const trigger = screen.getByRole("button", { name: "User menu for a" });
  fireEvent.keyDown(trigger, { key: "Enter" });
  const menu = within(await screen.findByRole("menu", { name: "User menu for a" }));
  expect(menu.getByText("a")).toBeInTheDocument();
  expect(menu.getByText("admin")).toBeInTheDocument();
  fireEvent.click(menu.getByRole("menuitem", { name: "Account settings" }));
  expect(await screen.findByText("Account content")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Expand sidebar" })).toHaveAttribute("aria-expanded", "false");
});
it("keeps resource links outside the user dropdown", async () => {
  showShell();
  expect(screen.getAllByRole("link", { name: "Classic UI" })).toHaveLength(1);
  expect(screen.getAllByRole("link", { name: "Swagger API (opens in a new tab)" })).toHaveLength(1);
  fireEvent.keyDown(screen.getByRole("button", { name: "User menu for a" }), { key: "Enter" });
  const menu = within(await screen.findByRole("menu", { name: "User menu for a" }));
  expect(menu.queryByRole("menuitem", { name: "Classic UI" })).not.toBeInTheDocument();
  expect(menu.queryByRole("menuitem", { name: /^Swagger API/ })).not.toBeInTheDocument();
});
it("closes the mobile drawer when Swagger opens without changing the hash route", async () => {
  Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Open navigation" }));
  const resources = within(screen.getByRole("dialog")).getByRole("navigation", { name: "Resources" });
  expect(within(resources).getByRole("link", { name: "Classic UI" })).toHaveAttribute("href", "/?ui=classic");
  fireEvent.click(within(resources).getByRole("link", { name: "Swagger API (opens in a new tab)" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(window.location.hash).toBe("#/services");
  expect(screen.getByRole("button", { name: "Open navigation" })).toHaveFocus();
});
it("prevents duplicate sign out from the sidebar while logout is pending", async () => {
  let finish!: () => void;
  session.logout.mockImplementation(() => new Promise<void>((resolve) => { finish = resolve; }));
  showShell();
  fireEvent.click(screen.getByRole("button", { name: "Collapse sidebar" }));
  const trigger = screen.getByRole("button", { name: "User menu for a" });
  fireEvent.keyDown(trigger, { key: "Enter" });
  const logout = await screen.findByRole("menuitem", { name: "Sign out" });
  fireEvent.click(logout);
  expect(trigger).toBeDisabled();
  fireEvent.keyDown(trigger, { key: "Enter" });
  expect(session.logout).toHaveBeenCalledTimes(1);
  await act(async () => finish());
  expect(trigger).toBeEnabled();
});
it("selects a persistent theme from the user menu submenu", async () => {
  const meta = document.createElement("meta");
  meta.name = "theme-color";
  meta.setAttribute("data-shell-theme-test", "");
  document.head.append(meta);
  showShell();
  fireEvent.keyDown(screen.getByRole("button", { name: "User menu for a" }), { key: "Enter" });
  const theme = await screen.findByRole("menuitem", { name: /Theme/ });
  expect(theme).toHaveAccessibleName("Theme");
  fireEvent.focus(theme);
  fireEvent.keyDown(theme, { key: "ArrowRight" });
  fireEvent.click(await screen.findByRole("menuitemradio", { name: "Dark" }));
  await waitFor(() => expect(localStorage.getItem("swarmpit.theme")).toBe("dark"));
  expect(document.documentElement).toHaveClass("dark");
  expect(meta).toHaveAttribute("content", "#000");
  expect(screen.queryByRole("button", { name: /Change theme/ })).not.toBeInTheDocument();
  fireEvent.keyDown(screen.getByRole("button", { name: "User menu for a" }), { key: "Enter" });
  const reopenedTheme = await screen.findByRole("menuitem", { name: "Theme" });
  fireEvent.focus(reopenedTheme);
  fireEvent.keyDown(reopenedTheme, { key: "ArrowRight" });
  expect(await screen.findByRole("menuitemradio", { name: "Dark" })).toHaveAttribute("aria-checked", "true");
  fireEvent.click(screen.getByRole("menuitemradio", { name: "Light" }));
  await waitFor(() => expect(localStorage.getItem("swarmpit.theme")).toBe("light"));
  expect(document.documentElement).not.toHaveClass("dark");
  expect(meta).toHaveAttribute("content", "#fafafa");
});
it("navigates from a nested service through its section breadcrumb and a single Overview home item", async () => {
  window.history.replaceState(null, "", "/react/#/services/service-1/edit");
  showShell();
  const breadcrumb = within(screen.getByRole("navigation", { name: "Breadcrumb" }));
  expect(breadcrumb.getByRole("list").tagName).toBe("OL");
  expect(breadcrumb.getAllByRole("listitem")).toHaveLength(2);
  expect(breadcrumb.getByRole("presentation", { hidden: true })).toHaveAttribute("aria-hidden", "true");
  const services = breadcrumb.getByRole("link", { name: "Services" });
  expect(services).toHaveAttribute("href", "#/services");
  expect(services).not.toHaveAttribute("aria-current");
  fireEvent.click(services);
  expect(await screen.findByText("Services content")).toBeInTheDocument();
  expect(breadcrumb.getByRole("link", { name: "Services" })).toHaveAttribute("aria-current", "page");
  const overview = breadcrumb.getByRole("link", { name: "Overview" });
  expect(overview).toHaveAttribute("href", "#/");
  fireEvent.click(overview);
  expect(await screen.findByText("Overview content")).toBeInTheDocument();
  expect(breadcrumb.getAllByRole("listitem")).toHaveLength(1);
  expect(breadcrumb.getAllByRole("link")).toHaveLength(1);
  expect(breadcrumb.getByRole("link", { name: "Overview" })).toHaveAttribute("aria-current", "page");
  expect(breadcrumb.queryByRole("presentation", { hidden: true })).not.toBeInTheDocument();
  expect(breadcrumb.queryByText("Workspace")).not.toBeInTheDocument();
});
it("links the current account breadcrumb to account settings", () => {
  window.history.replaceState(null, "", "/react/#/account-settings");
  showShell();
  const breadcrumb = within(screen.getByRole("navigation", { name: "Breadcrumb" }));
  expect(breadcrumb.getByRole("link", { name: "Overview" })).toHaveAttribute("href", "#/");
  const account = breadcrumb.getByRole("link", { name: "Account settings" });
  expect(account).toHaveAttribute("href", "#/account-settings");
  expect(account).toHaveAttribute("aria-current", "page");
});
it.each(["/services-old", "/account-settings-old", "/error"])("keeps only the home breadcrumb for unknown route %s", (path) => {
  window.history.replaceState(null, "", "/react/#" + path);
  showShell();
  const breadcrumb = within(screen.getByRole("navigation", { name: "Breadcrumb" }));
  expect(breadcrumb.getAllByRole("listitem")).toHaveLength(1);
  expect(breadcrumb.getAllByRole("link")).toHaveLength(1);
  expect(breadcrumb.getByRole("link", { name: "Overview" })).toHaveAttribute("href", "#/");
  expect(breadcrumb.getByRole("link", { name: "Overview" })).not.toHaveAttribute("aria-current");
});
it("closes the mobile drawer with Escape", async () => {
  Object.defineProperty(window, "innerWidth", { value: 390, configurable: true });
  showShell();
  await act(async () => fireEvent.click(screen.getByRole("button", { name: "Open navigation" })));
  const drawer = screen.getByRole("dialog");
  expect(screen.queryByRole("tooltip", { hidden: true })).not.toBeInTheDocument();
  fireEvent.keyDown(drawer, { key: "Escape" });
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
});
