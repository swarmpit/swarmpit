import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { Loading } from "./common";

it.each(["content", "table", "chart"] as const)("shows accessible shadcn skeletons for %s loading", (variant) => {
  render(<Loading variant={variant} label="Services loading" />);
  const status = screen.getByRole("status", { name: "Services loading" });
  expect(status).toHaveAttribute("aria-busy", "true");
  const placeholders = status.querySelectorAll('[data-slot="skeleton"]');
  expect(placeholders.length).toBeGreaterThan(0);
  for (const placeholder of placeholders) expect(placeholder.closest('[aria-hidden="true"]')).not.toBeNull();
  expect(status.querySelector("svg")).toBeNull();
});

it("uses the default accessible loading label at existing callsites", () => {
  render(<Loading />);
  expect(screen.getByRole("status", { name: "Loading…" })).toBeInTheDocument();
});
