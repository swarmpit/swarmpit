import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router";
import { ImagePicker } from "./image-picker";
import { selectOption } from "@/test-utils";
const state = vi.hoisted(() => ({
  paths: [] as string[],
  enabledPaths: [] as string[],
  errorPath: "",
  pendingPath: "",
  fetchingPath: "",
}));
beforeEach(() => {
  state.paths = [];
  state.enabledPaths = [];
  state.errorPath = "";
  state.pendingPath = "";
  state.fetchingPath = "";
});
vi.mock("@/features/session/session", () => ({
  useSession: () => ({ me: { username: "me", role: "user" } }),
}));
vi.mock("@/lib/queries", () => ({
  useApi: (path: string, enabled = true) => {
    state.paths.push(path);
    if (enabled) state.enabledPaths.push(path);
    const shared = {
      _id: "shared",
      type: "v2",
      name: "Shared",
      owner: "other",
      public: true,
      url: "https://registry.example",
    };
    return {
      data:
        path === state.pendingPath
          ? undefined
          : path === "/api/registry/v2"
            ? [shared]
            : path.endsWith("/repositories")
              ? [{ name: "team/app" }]
              : path.includes("/api/public/repositories?query=nginx&")
                ? [{ name: "nginx" }]
                : [],
      error:
        path === state.errorPath
          ? new Error("Registry request failed")
          : path === "/api/registry/v2/shared"
          ? new Error("Unauthorized owner access")
          : null,
      isPending: path === state.pendingPath,
      isFetching: path === state.pendingPath || path === state.fetchingPath,
    };
  },
}));
function Location() {
  return (
    <p data-testid="location">
      {useLocation().pathname + useLocation().search}
    </p>
  );
}
it("selects images from another owner public registry using allowed list metadata", async () => {
  render(
    <MemoryRouter initialEntries={["/services/create/wizard/image"]}>
      <ImagePicker />
      <Location />
    </MemoryRouter>,
  );
  await selectOption("Registry", "v2 · Shared");
  const choice = screen.getByRole("button", { name: "team/app" });
  expect(choice).toBeEnabled();
  fireEvent.click(choice);
  expect(screen.getByTestId("location")).toHaveTextContent(
    "/services/create/wizard/config?repository=registry.example%2Fteam%2Fapp",
  );
  expect(state.paths).not.toContain("/api/registry/v2/shared");
});

it("shows loading for the first repository request", async () => {
  state.pendingPath = "/api/registry/v2/shared/repositories";
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  await selectOption("Registry", "v2 · Shared");
  expect(screen.getByRole("status")).toBeInTheDocument();
  expect(screen.queryByText("No resources found.")).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "team/app" }),
  ).not.toBeInTheDocument();
});

it("shows loading instead of unrelated results after changing public search", () => {
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Search repositories"), {
    target: { value: "nginx" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
  expect(screen.getByRole("button", { name: "nginx" })).toBeEnabled();
  state.pendingPath = "/api/public/repositories?query=redis&page=1";
  fireEvent.change(screen.getByLabelText("Search repositories"), {
    target: { value: "redis" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
  expect(state.paths).toContain(state.pendingPath);
  expect(screen.getByRole("status")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "nginx" })).not.toBeInTheDocument();
});

it("keeps repository rows available during a background refetch", async () => {
  state.fetchingPath = "/api/registry/v2/shared/repositories";
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  await selectOption("Registry", "v2 · Shared");
  expect(screen.getByRole("button", { name: "team/app" })).toBeEnabled();
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

it("loads every registry provider with the menu closed and defaults to public search", () => {
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  expect(screen.getByRole("combobox", { name: "Registry" })).toHaveTextContent(
    "Public Docker Hub",
  );
  expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  for (const provider of ["dockerhub", "v2", "ecr", "acr", "gitlab"]) {
    expect(state.enabledPaths).toContain("/api/registry/" + provider);
  }
  expect(state.enabledPaths).not.toContain("/api/registry/public");
  expect(
    state.enabledPaths.some((path) => path.startsWith("/api/public/repositories")),
  ).toBe(false);
  fireEvent.change(screen.getByLabelText("Search repositories"), {
    target: { value: "nginx" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
  expect(state.enabledPaths).toContain("/api/public/repositories?query=nginx&page=1");
});

it("resets public pagination when changing registry sources", async () => {
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  fireEvent.change(screen.getByLabelText("Search repositories"), {
    target: { value: "nginx" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Search" }));
  fireEvent.click(screen.getByRole("button", { name: "Next" }));
  expect(screen.getByText("Page 2")).toBeInTheDocument();
  await selectOption("Registry", "v2 · Shared");
  expect(screen.getByRole("combobox", { name: "Registry" })).toHaveTextContent(
    "v2 · Shared",
  );
  await selectOption("Registry", "Public Docker Hub");
  expect(screen.getByText("Page 1")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Previous" })).toBeDisabled();
  expect(state.enabledPaths).not.toContain("/api/registry/v2/shared");
});

it("shows disabled loading and error entries without blocking available registries", async () => {
  state.pendingPath = "/api/registry/dockerhub";
  state.errorPath = "/api/registry/ecr";
  render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  fireEvent.keyDown(screen.getByRole("combobox", { name: "Registry" }), {
    key: "ArrowDown",
  });
  expect(
    await screen.findByRole("option", { name: "Loading dockerhub registries…" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(
    screen.getByRole("option", { name: "ecr unavailable: Registry request failed" }),
  ).toHaveAttribute("aria-disabled", "true");
  fireEvent.click(screen.getByRole("option", { name: "v2 · Shared" }));
  expect(screen.getByRole("button", { name: "team/app" })).toBeEnabled();
});

it("keeps selected registry metadata available during a provider refetch", async () => {
  const { rerender } = render(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  await selectOption("Registry", "v2 · Shared");
  state.fetchingPath = "/api/registry/v2";
  rerender(
    <MemoryRouter>
      <ImagePicker />
    </MemoryRouter>,
  );
  expect(screen.getByRole("combobox", { name: "Registry" })).toHaveTextContent(
    "v2 · Shared",
  );
  expect(screen.getByRole("button", { name: "team/app" })).toBeEnabled();
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});
