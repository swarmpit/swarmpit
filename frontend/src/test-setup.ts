import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";
// Radix Select scrolls focused options; jsdom has no layout/scroll implementation.
if (typeof HTMLElement !== "undefined" && !HTMLElement.prototype.scrollIntoView)
  HTMLElement.prototype.scrollIntoView = () => {};
afterEach(() => cleanup());
