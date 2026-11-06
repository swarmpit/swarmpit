import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useState } from "react";
import { EditorView } from "@uiw/react-codemirror";
import { afterAll, beforeAll, expect, it, vi } from "vitest";
import { ConfigValueField } from "./config-value-field";
import { CodeField } from "./code-field";
import { selectOption } from "@/test-utils";

// jsdom has no text geometry; CodeMirror measures selections after edits/focus.
const geometry = ["getClientRects", "getBoundingClientRect"] as const;
const descriptors = geometry.map((name) => Object.getOwnPropertyDescriptor(Range.prototype, name));
beforeAll(() => {
  Object.defineProperty(Range.prototype, "getClientRects", { configurable: true, value: () => [] });
  Object.defineProperty(Range.prototype, "getBoundingClientRect", { configurable: true, value: () => new DOMRect() });
});
afterAll(() => {
  geometry.forEach((name, index) => {
    const descriptor = descriptors[index];
    if (descriptor) Object.defineProperty(Range.prototype, name, descriptor);
    else Reflect.deleteProperty(Range.prototype, name);
  });
});

async function editor(name = "Value") {
  await waitFor(() => expect(screen.getByRole("textbox", { name }).tagName).toBe("DIV"));
  const content = screen.getByRole("textbox", { name });
  const view = EditorView.findFromDOM(content)!;
  expect(view).toBeDefined();
  return { content, view };
}

it("highlights named compose while preserving raw sources, readonly refreshes and the fallback layout", async () => {
  const raw = "# Привет\r\nservices:\r\n  web:\r\n    image: \"nginx:${TAG}\"\r\n";
  const next = "# next\r\nservices: {}\r\n";
  const onChange = vi.fn();
  const hint = "Stored YAML is sent unchanged.";
  function Preview({ value, readOnly = true }: { value: string; readOnly?: boolean }) {
    return <><CodeField label="Generated compose" value={value} onChange={onChange} readOnly={readOnly} hint={hint} rows={24} showSyntaxSelector={false} /><output aria-label="Raw source">{value}</output></>;
  }
  const mounted = render(<Preview value={raw} />);
  const fallback = screen.getByRole("textbox", { name: "Generated compose" });
  expect(fallback.tagName).toBe("TEXTAREA");
  expect(fallback).toHaveStyle({ height: "594px", minHeight: "594px" });
  expect(fallback).toHaveAccessibleDescription(hint);
  expect(screen.queryByRole("combobox", { name: "Syntax" })).not.toBeInTheDocument();
  expect(screen.getByLabelText("Raw source").textContent).toBe(raw);
  const { content, view } = await editor("Generated compose");
  expect(content).toHaveAccessibleDescription(hint);
  expect(content).toHaveAttribute("aria-readonly", "true");
  expect(content.querySelectorAll(".cm-line span").length).toBeGreaterThan(0);
  expect(content.closest(".cm-editor")?.parentElement?.parentElement).toHaveStyle({ height: "594px" });
  expect(view.state.doc.toString()).toBe(raw.replaceAll("\r\n", "\n"));
  expect(onChange).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Raw source").textContent).toBe(raw);

  mounted.rerender(<Preview value={next} />);
  await waitFor(() => expect(view.state.doc.toString()).toBe(next.replaceAll("\r\n", "\n")));
  expect(onChange).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Raw source").textContent).toBe(next);
  mounted.rerender(<Preview value={raw} readOnly={false} />);
  await waitFor(() => expect(view.state.doc.toString()).toBe(raw.replaceAll("\r\n", "\n")));
  expect(content).toHaveAttribute("contenteditable", "true");
  expect(onChange).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Raw source").textContent).toBe(raw);
});

it("edits compose through its original controlled callback without changing interpolation or Unicode", async () => {
  const raw = "# Привет\nservices:\n  web:\n    image: \"nginx:${TAG}\"\n";
  function Form() {
    const [value, setValue] = useState(raw);
    return <><CodeField label="Compose YAML" value={value} onChange={setValue} required showSyntaxSelector={false} /><output aria-label="Compose payload">{value}</output></>;
  }
  render(<Form />);
  const { content, view } = await editor("Compose YAML");
  expect(content).toHaveAttribute("aria-required", "true");
  expect(screen.getByLabelText("Compose payload").textContent).toBe(raw);
  act(() => view.dispatch({ changes: { from: 0, insert: "# changed\n" } }));
  expect(screen.getByLabelText("Compose payload").textContent).toBe("# changed\n" + raw);
});

it("preserves fallback typing/focus and controlled raw values through real syntax changes", async () => {
  const raw = 'message: "<b>Привет</b>"\ncount: 2\n';
  function Form() {
    const [value, setValue] = useState("");
    return <><ConfigValueField value={value} onChange={setValue} required /><output aria-label="Raw value">{value}</output></>;
  }
  render(<Form />);
  const fallback = screen.getByRole("textbox", { name: "Value" });
  expect(fallback.tagName).toBe("TEXTAREA");
  fallback.focus();
  fireEvent.change(fallback, { target: { value: raw } });
  (fallback as HTMLTextAreaElement).setSelectionRange(2, 7);
  fireEvent.select(fallback);
  const { content, view } = await editor();
  expect(content).toHaveFocus();
  expect(content).toHaveAttribute("aria-required", "true");
  expect(view.state.doc.toString()).toBe(raw);
  expect(view.state.selection.main).toMatchObject({ anchor: 2, head: 7 });
  expect(content.querySelector("b")).toBeNull();
  expect(content.querySelectorAll(".cm-line span").length).toBeGreaterThan(0);

  await selectOption("Syntax", "JSON");
  expect(EditorView.findFromDOM(screen.getByRole("textbox", { name: "Value" }))).toBe(view);
  expect(view.state.doc.toString()).toBe(raw);
  const jsonValue = '{"enabled":true,"message":"Привет"}';
  act(() => view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: jsonValue } }));
  expect(screen.getByLabelText("Raw value")).toHaveTextContent(jsonValue);
  expect(content.querySelectorAll(".cm-line span").length).toBeGreaterThan(0);
  await selectOption("Syntax", "Plain text");
  expect(view.state.doc.toString()).toBe(jsonValue);
  expect(content.querySelectorAll(".cm-line span")).toHaveLength(0);
  expect(content).toHaveAttribute("contenteditable", "true");
});

it("keeps details readable and selectable while disabling edits, including refreshed server values", async () => {
  const onChange = vi.fn();
  const mounted = render(<ConfigValueField value="first: true" onChange={onChange} readOnly />);
  const { content, view } = await editor();
  expect(content).toHaveAttribute("contenteditable", "false");
  expect(content).toHaveAttribute("aria-readonly", "true");
  expect(content).toHaveAttribute("tabindex", "0");
  expect(view.state.readOnly).toBe(true);
  content.focus();
  expect(content).toHaveFocus();
  fireEvent.keyDown(content, { key: "x" });
  expect(view.state.doc.toString()).toBe("first: true");
  expect(onChange).not.toHaveBeenCalled();
  mounted.rerender(<ConfigValueField value="next: false" onChange={onChange} readOnly />);
  await waitFor(() => expect(view.state.doc.toString()).toBe("next: false"));
  expect(onChange).not.toHaveBeenCalled();

  mounted.rerender(<ConfigValueField value="next: false" onChange={onChange} disabled />);
  expect(screen.getByRole("combobox", { name: "Syntax" })).toBeDisabled();
  expect(content).toHaveAttribute("aria-disabled", "true");
  expect(content).toHaveAttribute("tabindex", "-1");
  expect(view.state.readOnly).toBe(true);
});
