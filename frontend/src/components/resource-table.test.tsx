import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it } from "vitest";
import { ResourceTable } from "./common";

type Row = { port: number; name: string };
const columns = [
  { label: "Port", render: (row: Row) => row.port },
  { label: "Name", render: (row: Row) => row.name },
];
const cells = () => within(screen.getByRole("table")).getAllByRole("row").slice(1)
  .map((row) => within(row).getAllByRole("cell").map((cell) => cell.textContent));

it("sorts numeric primary values without mutating data and reapplies order to refreshed filtered rows", () => {
  const rows = Object.freeze([
    { port: 8080, name: "web" },
    { port: 443, name: "secure" },
    { port: 80, name: "http" },
  ]);
  const view = render(<ResourceTable rows={rows} sortBy={(row) => row.port}
    search={(row) => row.name} columns={columns} />);
  expect(cells()).toEqual([["80", "http"], ["443", "secure"], ["8080", "web"]]);
  expect(rows.map((row) => row.port)).toEqual([8080, 443, 80]);
  fireEvent.change(screen.getByRole("textbox", { name: "Filter resources" }), { target: { value: "web" } });
  expect(cells()).toEqual([["8080", "web"]]);
  const refreshed = Object.freeze([...rows, { port: 8000, name: "web-next" }]);
  view.rerender(<ResourceTable rows={refreshed} sortBy={(row) => row.port}
    search={(row) => row.name} columns={columns} />);
  expect(cells()).toEqual([["8000", "web-next"], ["8080", "web"]]);
  expect(refreshed.map((row) => row.port)).toEqual([8080, 443, 80, 8000]);
});
