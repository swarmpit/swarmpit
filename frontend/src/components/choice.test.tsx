import { useState } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Choice } from "./common";
import { selectOption } from "@/test-utils";

it("selects and restores the empty Default option without changing its API value", async () => {
  const changed = vi.fn();
  function Form() {
    const [value, setValue] = useState("");
    return <Choice label="Mode" value={value} options={["", "value:mode", "global"]}
      onChange={(next) => { changed(next); setValue(next); }} />;
  }
  render(<Form />);
  const control = screen.getByRole("combobox", { name: "Mode" });
  expect(control).toHaveTextContent("Default");
  expect(changed).not.toHaveBeenCalled();
  await selectOption(control, "value:mode");
  expect(changed).toHaveBeenLastCalledWith("value:mode");
  expect(control).toHaveTextContent("value:mode");
  await selectOption(control, "Default");
  expect(changed).toHaveBeenLastCalledWith("");
  expect(control).toHaveTextContent("Default");
});

it("retains an existing value absent from the current option list until the user chooses", () => {
  const changed = vi.fn();
  render(<Choice label="Driver" value="custom-driver" options={["bridge", "overlay"]} onChange={changed} />);
  expect(screen.getByRole("combobox", { name: "Driver" })).toHaveTextContent("custom-driver");
  expect(changed).not.toHaveBeenCalled();
});

it("cannot change a disabled choice", () => {
  const changed = vi.fn();
  render(<Choice label="Role" value="worker" options={["worker", "manager"]} disabled onChange={changed} />);
  const control = screen.getByRole("combobox", { name: "Role" });
  expect(control).toBeDisabled();
  fireEvent.keyDown(control, { key: "ArrowDown" });
  expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
  expect(changed).not.toHaveBeenCalled();
});
