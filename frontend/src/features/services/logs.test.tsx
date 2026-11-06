import { expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ServiceLogs } from "./detail";
vi.mock("@/lib/queries", () => ({
  useApi: () => ({
    data: [
      { task: "task-a", line: "Selected task output" },
      { task: "task-b", line: "Other task output" },
    ],
    isPending: false,
    error: null,
    refetch: vi.fn(),
  }),
}));
it("keeps task log view scoped to its exact task ID", () => {
  render(<ServiceLogs id="service" initialTask="task-a" />);
  expect(screen.getByText("Selected task output")).toBeInTheDocument();
  expect(screen.queryByText("Other task output")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Task")).toHaveTextContent("task-a");
});
