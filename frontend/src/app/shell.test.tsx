import { beforeEach, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { HashRouter, Routes, Route } from "react-router";
import { Shell } from "./shell";
vi.mock("@/features/session/session", () => ({
  useSession: () => ({
    me: { username: "a", role: "admin" },
    version: { version: "test", docker: {} },
    logout: vi.fn(),
  }),
}));
beforeEach(() => {
  window.history.replaceState(null, "", "/react/#/services");
  vi.stubGlobal("matchMedia", () => ({
    matches: false,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
});
it("skip navigation focuses content without replacing the hash route", () => {
  render(
    <HashRouter>
      <Routes>
        <Route element={<Shell />}>
          <Route path="/services" element={<p>Services content</p>} />
        </Route>
      </Routes>
    </HashRouter>,
  );
  fireEvent.click(screen.getByText("Skip to content"));
  expect(document.getElementById("main-content")).toHaveFocus();
  expect(window.location.hash).toBe("#/services");
});
