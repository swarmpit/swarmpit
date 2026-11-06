import { fireEvent, screen } from "@testing-library/react";

export async function selectOption(
  control: string | HTMLElement,
  option: string | RegExp,
): Promise<void> {
  const trigger = typeof control === "string"
    ? screen.getByRole("combobox", { name: control })
    : control;
  fireEvent.keyDown(trigger, { key: "ArrowDown" });
  fireEvent.click(await screen.findByRole("option", { name: option }));
}
