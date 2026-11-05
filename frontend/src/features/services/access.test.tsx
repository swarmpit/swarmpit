import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router";
import { defaultService } from "./payload";
import { ServiceDetail } from "./detail";
import { ServiceForm } from "./service-form";
const state = vi.hoisted(() => ({
  role: "user",
  service: undefined as unknown,
  mutate: vi.fn(),
}));
vi.mock("@/lib/queries", () => ({
  useApi: (path: string) => ({
    data: path === "/api/services/id" ? state.service : [],
    error: null,
    isPending: false,
    isFetching: false,
    refetch: vi.fn(),
  }),
  useCommand: () => ({ mutate: state.mutate, isPending: false, error: null }),
}));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "operator", role: state.role },
    version: { docker: { api: 1.45 }, statistics: false },
  }),
}));
vi.mock("@/features/resources/lists", () => ({ Live: () => null }));
beforeEach(() => {
  state.role = "user";
  state.service = { ...defaultService("nginx"), id: "id", serviceName: "web" };
  state.mutate.mockClear();
});
function show(element: React.ReactNode, path = "/services/id") {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/services/:id" element={element} />
        <Route path="/services/create/wizard/config" element={element} />
      </Routes>
    </MemoryRouter>,
  );
}
it("hides mutating service controls from viewers", () => {
  state.role = "viewer";
  show(<ServiceDetail />);
  for (const name of ["Edit", "Delete", "Stop", "Redeploy", "Rollback"])
    expect(screen.queryByRole("button", { name })).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Pin to dashboard" }),
  ).toBeInTheDocument();
});
it("blocks direct editing of immutable services", () => {
  state.service = { ...defaultService("nginx"), id: "id", immutable: true };
  show(<ServiceForm />);
  expect(screen.getByRole("alert")).toHaveTextContent("immutable");
  expect(
    screen.queryByRole("button", { name: "Deploy changes" }),
  ).not.toBeInTheDocument();
});
it("requires confirmation before deleting a service", () => {
  show(<ServiceDetail />);
  fireEvent.click(screen.getByRole("button", { name: "Delete" }));
  expect(state.mutate).not.toHaveBeenCalled();
  expect(screen.getByRole("dialog")).toBeInTheDocument();
  const buttons = screen.getAllByRole("button", { name: "Delete" });
  fireEvent.click(buttons.at(-1)!);
  expect(state.mutate).toHaveBeenCalledWith({
    path: "/api/services/id",
    method: "DELETE",
    body: undefined,
  });
});
it("honors selected registry repository and tag", () => {
  show(
    <ServiceForm />,
    "/services/create/wizard/config?repository=registry%3A5000%2Fteam%2Fimage&tag=stable",
  );
  expect(screen.getByLabelText("Repository")).toHaveValue(
    "registry:5000/team/image",
  );
  expect(screen.getByLabelText("Image tag")).toHaveValue("stable");
});
it("honors bookmarked legacy service log query links", () => {
  show(<ServiceDetail />, "/services/id?log=1");
  expect(
    screen.getByRole("heading", { name: "Service logs" }),
  ).toBeInTheDocument();
  expect(screen.getByRole("tab", { name: "Logs" })).toHaveAttribute(
    "aria-selected",
    "true",
  );
});
