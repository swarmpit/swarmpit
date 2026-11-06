import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { afterEach, expect, it } from "vitest";

const html = readFileSync("index.html", "utf8");
const originalHead = document.head.innerHTML;
afterEach(() => {
  document.head.innerHTML = originalHead;
  document.documentElement.classList.remove("dark");
});

function start(theme: string | null, osDark: boolean, unavailable?: "storage" | "media" | "missing-media") {
  const parsed = new DOMParser().parseFromString(html, "text/html");
  document.head.innerHTML = parsed.head.innerHTML;
  document.documentElement.classList.remove("dark");
  const script = document.head.querySelector("script")!;
  const context: Record<string, unknown> = {
    document,
    localStorage: { getItem: (key: string) => {
      if (unavailable === "storage") throw new Error("Storage denied");
      return key === "swarmpit.theme" ? theme : null;
    } },
  };
  if (unavailable !== "missing-media") context.matchMedia = (query: string) => {
    if (unavailable === "media") throw new Error("Media unavailable");
    return { matches: query === "(prefers-color-scheme: dark)" && osDark };
  };
  runInNewContext(script.textContent!, context);
}

it.each([
  ["dark", false, true],
  ["light", true, false],
  ["system", true, true],
  ["system", false, false],
  [null, true, true],
  [null, false, false],
  ["invalid", true, true],
] as const)("resolves %s before React and styles the %s system canvas", (theme, osDark, dark) => {
  start(theme, osDark);
  expect(document.documentElement.classList.contains("dark")).toBe(dark);
  const style = getComputedStyle(document.documentElement);
  expect(style.backgroundColor).toBe(dark ? "rgb(0, 0, 0)" : "rgb(250, 250, 250)");
  expect(style.color).toBe(dark ? "rgb(222, 222, 222)" : "rgb(33, 33, 33)");
  expect(style.colorScheme).toBe(dark ? "dark" : "light");
  expect(document.head.querySelector('meta[name="theme-color"]')).toHaveAttribute("content", dark ? "#000" : "#fafafa");
});

it("falls back to the system preference when storage is blocked", () => {
  start(null, true, "storage");
  expect(getComputedStyle(document.documentElement).backgroundColor).toBe("rgb(0, 0, 0)");
});

it.each(["media", "missing-media"] as const)("keeps startup usable when %s is unavailable", (unavailable) => {
  start("system", true, unavailable);
  expect(getComputedStyle(document.documentElement).backgroundColor).toBe("rgb(250, 250, 250)");
  start("dark", false, unavailable);
  expect(getComputedStyle(document.documentElement).backgroundColor).toBe("rgb(0, 0, 0)");
});

it("places a synchronous theme bootstrap and critical canvas CSS ahead of the app module", () => {
  const parsed = new DOMParser().parseFromString(html, "text/html");
  const bootstrap = parsed.head.querySelector("script")!;
  expect(bootstrap.getAttribute("type")).not.toBe("module");
  expect(bootstrap.hasAttribute("async")).toBe(false);
  expect(bootstrap.hasAttribute("defer")).toBe(false);
  expect(bootstrap.hasAttribute("src")).toBe(false);
  expect(html.indexOf("<script>")).toBeLessThan(html.indexOf('type="module"'));
  expect(html.indexOf("<style>")).toBeLessThan(html.indexOf('type="module"'));
  expect(parsed.getElementById("root")?.innerHTML).toBe("");
});
