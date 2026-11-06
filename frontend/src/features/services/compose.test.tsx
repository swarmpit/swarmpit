import { beforeEach, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { ServiceDetail } from "./detail";
import { defaultService } from "./payload";

const state = vi.hoisted(() => ({
  pending: false,
  error: null as Error | null,
  compose: "",
}));
vi.mock("@/lib/queries", () => ({
  useApi: (path: string) => path.endsWith("/compose") ? {
    data: { name: "web", spec: { compose: state.compose } },
    isPending: state.pending,
    error: state.error,
  } : {
    data: path === "/api/services/id"
      ? { ...defaultService("nginx"), id: "id", serviceName: "web" }
      : [],
    isPending: false, error: null,
  },
  useCommand: () => ({ mutate: vi.fn(), isPending: false, error: null }),
}));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({ me: { role: "viewer" }, version: { statistics: false } }),
}));
vi.mock("@/features/resources/lists", () => ({ Live: () => null }));
vi.mock("@/components/code-field", () => ({
  CodeField: ({ label, value, readOnly }: { label: string; value: string; readOnly?: boolean }) => (
    <textarea aria-label={label} value={value} readOnly={readOnly} />
  ),
}));

beforeEach(() => {
  state.pending = false;
  state.error = null;
  state.compose = '# Generated\nservices:\n  web:\n    image: nginx:${TAG:-latest}\n    environment:\n      MESSAGE: "Привет <b>world</b>"\n';
});
function page() {
  return <MemoryRouter initialEntries={["/services/id?tab=compose"]}>
    <Routes><Route path="/services/:id" element={<ServiceDetail />} /></Routes>
  </MemoryRouter>;
}

it("keeps generated Compose read-only and verbatim for viewers, including refreshed results and errors", () => {
  const view = render(page());
  const field = screen.getByRole("textbox", { name: "Generated compose" });
  expect(field).toHaveValue(state.compose);
  expect(field).toHaveAttribute("readonly");
  expect(screen.queryByRole("button", { name: "Deploy" })).not.toBeInTheDocument();
  expect(field.querySelector("b")).toBeNull();

  state.compose = "# refreshed\nservices: {}\n";
  state.error = new Error("Refresh unavailable");
  view.rerender(page());
  expect(screen.getByRole("textbox", { name: "Generated compose" })).toHaveValue(state.compose);
  expect(screen.getByRole("alert")).toHaveTextContent("Refresh unavailable");
});

it("reserves the generated Compose editor layout while its data loads", () => {
  state.pending = true;
  const view = render(page());
  const loading = screen.getByRole("status", { name: "Loading generated compose" });
  expect(loading).toHaveAttribute("aria-busy", "true");
  expect(loading.querySelectorAll('[data-slot="skeleton"]')).toHaveLength(2);
  expect(screen.queryByRole("textbox", { name: "Generated compose" })).not.toBeInTheDocument();

  state.pending = false;
  view.rerender(page());
  expect(screen.queryByRole("status", { name: "Loading generated compose" })).not.toBeInTheDocument();
  expect(screen.getByRole("textbox", { name: "Generated compose" })).toHaveValue(state.compose);
});
