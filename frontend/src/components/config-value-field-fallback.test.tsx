import { fireEvent, render, screen } from "@testing-library/react";
import { useState } from "react";
import { expect, it, vi } from "vitest";
import { ConfigValueField } from "./config-value-field";
import { CodeField } from "./code-field";

vi.mock("./config-value-editor", () => { throw new Error("Editor chunk unavailable"); });

it("keeps named compose accessible and editable if highlighting cannot load", async () => {
  const raw = "# source\nservices: {}\n";
  function Form() {
    const [value, setValue] = useState(raw);
    return <CodeField label="Compose YAML" value={value} onChange={setValue} required hint="Original YAML" rows={24} showSyntaxSelector={false} />;
  }
  render(<Form />);
  expect(await screen.findByRole("status")).toHaveTextContent("Syntax highlighting is unavailable");
  const input = screen.getByRole("textbox", { name: "Compose YAML" });
  expect(input).toHaveAccessibleDescription("Original YAML");
  expect(input).toHaveStyle({ height: "594px" });
  expect(input).toBeRequired();
  fireEvent.change(input, { target: { value: raw + "# Привет\n" } });
  expect(input).toHaveValue(raw + "# Привет\n");
  expect(screen.queryByRole("combobox", { name: "Syntax" })).not.toBeInTheDocument();
});

it("retains a working controlled textarea if the syntax editor cannot load", async () => {
  function Form() {
    const [value, setValue] = useState("initial: true");
    return <ConfigValueField value={value} onChange={setValue} required />;
  }
  render(<Form />);
  expect(await screen.findByRole("status")).toHaveTextContent("Syntax highlighting is unavailable");
  const input = screen.getByRole("textbox", { name: "Value" });
  expect(input.tagName).toBe("TEXTAREA");
  expect(input).toBeRequired();
  fireEvent.change(input, { target: { value: "retained: Привет" } });
  expect(input).toHaveValue("retained: Привет");
});
