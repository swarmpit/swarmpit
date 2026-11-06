import { afterEach, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Add01Icon, Delete02Icon } from "@hugeicons/core-free-icons";
import { Action, Submit } from "./common";

afterEach(() => vi.unstubAllGlobals());

function icon(button: HTMLElement) {
  expect(button.querySelectorAll("svg")).toHaveLength(1);
  const svg = button.querySelector("svg")!;
  expect(svg).toHaveAttribute("aria-hidden", "true");
  return svg;
}

it("keeps a named submit action with one decorative icon through pending and idle states", () => {
  const view = render(<Submit pending={false} icon={Add01Icon}>Create config</Submit>);
  const idle = screen.getByRole("button", { name: "Create config" });
  expect(idle).toHaveAttribute("type", "submit");
  expect(icon(idle)).not.toHaveClass("animate-spin");
  view.rerender(<Submit pending icon={Add01Icon}>Create config</Submit>);
  const pending = screen.getByRole("button", { name: "Working…" });
  expect(pending).toBeDisabled();
  expect(icon(pending)).toHaveClass("animate-spin", "motion-reduce:animate-none");
  view.rerender(<Submit pending={false}>Create config</Submit>);
  expect(icon(screen.getByRole("button", { name: "Create config" }))).not.toHaveClass("animate-spin");
});

it("preserves confirmation and disabled mutation behavior with icons on all dialog actions", async () => {
  let finish!: (response: Response) => void;
  vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>((resolve) => { finish = resolve; })));
  const saved = vi.fn();
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  render(<QueryClientProvider client={client}>
    <Action label="Delete config" icon={Delete02Icon} path="/api/configs/c1" method="DELETE" destructive onSuccess={saved} />
  </QueryClientProvider>);
  icon(screen.getByRole("button", { name: "Delete config" }));
  fireEvent.click(screen.getByRole("button", { name: "Delete config" }));
  let dialog = await screen.findByRole("dialog", { name: "Delete config?" });
  icon(within(dialog).getByRole("button", { name: "Cancel" }));
  icon(within(dialog).getByRole("button", { name: "Delete config" }));
  fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));
  await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
  expect(fetch).not.toHaveBeenCalled();

  fireEvent.click(screen.getByRole("button", { name: "Delete config" }));
  dialog = await screen.findByRole("dialog", { name: "Delete config?" });
  fireEvent.click(within(dialog).getByRole("button", { name: "Delete config" }));
  const pending = await within(dialog).findByRole("button", { name: "Working…" });
  expect(pending).toBeDisabled();
  expect(within(dialog).getByRole("button", { name: "Cancel" })).toBeDisabled();
  expect(icon(pending)).toHaveClass("animate-spin", "motion-reduce:animate-none");
  await act(async () => finish(new Response(null, { status: 204 })));
  await waitFor(() => expect(saved).toHaveBeenCalledOnce());
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Delete config" })).toBeEnabled();
});
