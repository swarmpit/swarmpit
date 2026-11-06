import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { Status } from "./common";

it("keeps status text and semantic tones while adding a decorative icon", () => {
  const { rerender } = render(<Status value="running" />);
  for (const [value, tone] of [
    ["running", "text-success-foreground"],
    ["leader", "text-success-foreground"],
    ["completed", "text-success-foreground"],
    ["failed", "text-destructive"],
    ["shutdown", "text-destructive"],
    ["down", "text-destructive"],
    ["updating", "text-muted-foreground"],
    ["rollback", "text-muted-foreground"],
    ["pending", "text-muted-foreground"],
    ["starting", "text-muted-foreground"],
    ["drain", "text-muted-foreground"],
    ["pause", "text-muted-foreground"],
    ["deployed", "text-muted-foreground"],
    ["inactive", "text-muted-foreground"],
  ]) {
    rerender(<Status value={value} />);
    const badge = screen.getByText(value);
    expect(badge).toHaveClass(tone);
    expect(badge.querySelectorAll("svg")).toHaveLength(1);
    expect(badge.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
    expect(badge.querySelector("svg")).toHaveAttribute("stroke-width", "2");
    expect(badge.querySelector("svg")).toHaveClass("size-3", "shrink-0");
  }
  expect(screen.queryByRole("img")).not.toBeInTheDocument();
});

it("safely renders fallback icons for missing and unfamiliar backend status values", () => {
  const { rerender } = render(<Status />);
  for (const value of [undefined, "", "future_status", "__proto__", "constructor"]) {
    rerender(<Status value={value} />);
    const badge = screen.getByText(value || "unknown");
    expect(badge).toHaveClass("text-muted-foreground");
    expect(badge.querySelectorAll("svg")).toHaveLength(1);
    expect(badge.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
    expect(badge.querySelector("svg")?.children.length).toBeGreaterThan(0);
  }
});
