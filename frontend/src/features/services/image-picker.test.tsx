import { expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router";
import { ImagePicker } from "./image-picker";
const state = vi.hoisted(() => ({ paths: [] as string[] }));
vi.mock("@/features/session/session", () => ({
  useSession: () => ({ me: { username: "me", role: "user" } }),
}));
vi.mock("@/lib/queries", () => ({
  useApi: (path: string) => {
    state.paths.push(path);
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
        path === "/api/registry/v2"
          ? [shared]
          : path.endsWith("/repositories")
            ? [{ name: "team/app" }]
            : [],
      error:
        path === "/api/registry/v2/shared"
          ? new Error("Unauthorized owner access")
          : null,
      isPending: false,
      isFetching: false,
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
it("selects images from another owner public registry using allowed list metadata", () => {
  render(
    <MemoryRouter initialEntries={["/services/create/wizard/image"]}>
      <ImagePicker />
      <Location />
    </MemoryRouter>,
  );
  fireEvent.change(screen.getByLabelText("Registry"), {
    target: { value: "v2/shared" },
  });
  const choice = screen.getByRole("button", { name: "team/app" });
  expect(choice).toBeEnabled();
  fireEvent.click(choice);
  expect(screen.getByTestId("location")).toHaveTextContent(
    "/services/create/wizard/config?repository=registry.example%2Fteam%2Fapp",
  );
  expect(state.paths).not.toContain("/api/registry/v2/shared");
});
