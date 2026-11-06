import { expect, it } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import type { Network } from "@/lib/types";
import { defaultService } from "@/features/services/payload";
import { NetworkMap } from "./network-map";

const network: Network = {
  id: "network-id", networkName: "frontend", driver: "overlay", scope: "swarm",
  internal: false, attachable: true, ingress: false, enableIPv6: false,
  options: [], ipam: { subnet: "10.0.4.0/24", gateway: "10.0.4.1" },
};
const web = {
  ...defaultService("nginx"), id: "service/web", serviceName: "web",
  status: { tasks: { running: 2, total: 3 } },
  networks: [
    { id: "other", networkName: "database", serviceAliases: ["private-alias"] },
    { id: network.id, networkName: network.networkName, serviceAliases: ["public-web"] },
  ],
};

it("selects a service attachment with its own network aliases and encoded detail link", () => {
  render(<MemoryRouter><NetworkMap network={network} services={[web]} /></MemoryRouter>);
  expect(screen.getByRole("region", { name: "Network topology" })).toBeInTheDocument();
  const service = screen.getByRole("button", { name: "Inspect web" });
  expect(service.tagName).toBe("BUTTON");
  service.focus();
  expect(service).toHaveFocus();
  fireEvent.click(service);
  expect(service).toHaveAttribute("aria-pressed", "true");
  const details = within(screen.getByRole("region", { name: "Selected service" }));
  expect(details.getByText("public-web")).toBeInTheDocument();
  expect(details.queryByText("private-alias")).not.toBeInTheDocument();
  expect(details.getByText("2 / 3")).toBeInTheDocument();
  expect(details.getByRole("link", { name: "Open service" })).toHaveAttribute("href", "/services/service%2Fweb");
});

it("shows refreshed attachment values and clears removed selections", () => {
  const view = render(<MemoryRouter><NetworkMap network={network} services={[web]} /></MemoryRouter>);
  fireEvent.click(screen.getByRole("button", { name: "Inspect web" }));
  view.rerender(<MemoryRouter><NetworkMap network={network} services={[{
    ...web, networks: [{ networkName: "frontend", serviceAliases: ["updated-alias"] }],
  }]} /></MemoryRouter>);
  expect(within(screen.getByRole("region", { name: "Selected service" })).getByText("updated-alias")).toBeInTheDocument();
  expect(screen.queryByText("public-web")).not.toBeInTheDocument();
  view.rerender(<MemoryRouter><NetworkMap network={network} services={[]} /></MemoryRouter>);
  expect(screen.queryByRole("region", { name: "Selected service" })).not.toBeInTheDocument();
  expect(screen.queryByRole("link", { name: "Open service" })).not.toBeInTheDocument();
  expect(screen.getByText("No services are connected to this network.")).toBeInTheDocument();
});

it("keeps the network visible without inventing aliases or task counts", () => {
  const service = { ...web, status: undefined, networks: [{ networkName: "frontend" }] };
  render(<MemoryRouter><NetworkMap network={network} services={[service]} /></MemoryRouter>);
  expect(screen.getByText("10.0.4.0/24")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Inspect web" }));
  const details = within(screen.getByRole("region", { name: "Selected service" }));
  expect(details.getByText("No service aliases reported")).toBeInTheDocument();
  expect(details.queryByText("Running tasks")).not.toBeInTheDocument();
});
