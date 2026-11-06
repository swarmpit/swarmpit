import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router";
import type { Service } from "@/lib/types";
import { defaultService } from "./payload";
import { ServiceDetail } from "./detail";

vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "reader", role: "viewer" },
    version: { statistics: false },
  }),
}));
vi.mock("@/features/resources/lists", () => ({ Live: () => null }));

let service: Service;
beforeEach(() => {
  service = { ...defaultService("nginx"), id: "id", serviceName: "web" };
  vi.stubGlobal("fetch", vi.fn(async (path: string) => {
    if (path === "/api/services/id")
      return new Response(JSON.stringify(service), { status: 200 });
    if (path === "/api/services/id/logs?since=15m")
      return new Response(JSON.stringify([{ task: "task-a", line: "Service output" }]), { status: 200 });
    throw new Error("Unexpected request: " + path);
  }));
});
afterEach(() => vi.unstubAllGlobals());

function mount(search: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/services/id" + search]}>
        <Routes><Route path="/services/:id" element={<ServiceDetail />} /></Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

it("renders named native service links and their original URLs", async () => {
  const links = [
    { name: "Service_home", value: "https://example.com/app?tab=1#status" },
    { name: "Internal", value: "http://web.internal:8080" },
    { name: "Relative", value: "/health" },
    { name: "Relative host", value: "//example.com/health" },
    { name: "Encoded path", value: "javascript%3Aalert(1)" },
    { name: "Entity path", value: "java&#x73;cript:alert(1)" },
    { name: "Email", value: "mailto:operator@example.com" },
    { name: "Phone", value: "tel:+420123456789" },
    { name: "Files", value: "ftp://files.example.com" },
  ];
  service = { ...service, links };
  mount("?tab=configuration");
  const section = (await screen.findByRole("heading", { name: "Links" })).closest("section")!;
  for (const link of links) {
    const anchor = within(section).getByRole("link", { name: link.name.replaceAll("_", " ") });
    expect(anchor).toHaveAttribute("href", link.value);
    expect(anchor).toHaveAttribute("target", "_blank");
    expect(anchor).toHaveAttribute("rel", "noopener noreferrer");
    expect(["http:", "https:", "mailto:", "tel:", "ftp:"]).toContain((anchor as HTMLAnchorElement).protocol);
    expect(within(section).getByText(link.value)).toBeInTheDocument();
  }
});

it("keeps executable, unknown and malformed service URLs readable without anchors", async () => {
  const links = [
    { name: "Script", value: "javascript:alert(1)" },
    { name: "Mixed", value: " \tJaVa\nScRiPt:alert(1)" },
    { name: "Data", value: "data:text/html,<script>alert(1)</script>" },
    { name: "VBScript", value: "vbscript:msgbox(1)" },
    { name: "Custom", value: "custom-app:execute" },
    { name: "Malformed", value: "http://[" },
  ];
  service = { ...service, links };
  mount("?tab=configuration");
  const section = (await screen.findByRole("heading", { name: "Links" })).closest("section")!;
  expect(within(section).queryByRole("link")).not.toBeInTheDocument();
  for (const link of links) {
    expect(within(section).getByText(link.name)).toBeInTheDocument();
    expect(within(section).getByText((_, element) => element?.textContent === link.value)).toBeInTheDocument();
  }
});

it.each(["none", "syslog", "fluentd"].flatMap((driver) =>
  ["?tab=logs", "?log=1"].map((search) => [driver, search] as const),
))("does not mount or request logs for %s via %s", async (driver, search) => {
  service = { ...service, logdriver: { name: driver, opts: [] } };
  mount(search);
  await screen.findByRole("heading", { name: "web" });
  expect(screen.getByRole("tab", { name: "Logs" })).toBeDisabled();
  expect(screen.getByRole("tab", { name: "Overview" })).toHaveAttribute("aria-selected", "true");
  expect(screen.queryByRole("heading", { name: "Service logs" })).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(fetch).toHaveBeenCalledWith("/api/services/id", expect.anything());
});

it.each(["json-file", "journald", "local", "loki"])("opens and requests supported %s logs", async (driver) => {
  service = { ...service, logdriver: { name: driver, opts: [] } };
  mount("?tab=logs");
  expect(await screen.findByText("Service output")).toBeInTheDocument();
  expect(screen.getByRole("tab", { name: "Logs" })).toBeEnabled();
  expect(screen.getByRole("tab", { name: "Logs" })).toHaveAttribute("aria-selected", "true");
  expect(fetch).toHaveBeenCalledWith("/api/services/id/logs?since=15m", expect.anything());
});
