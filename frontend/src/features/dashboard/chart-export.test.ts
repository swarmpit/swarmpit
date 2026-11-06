import { afterEach, expect, it, vi } from "vitest";
import { exportChartImage } from "./chart-export";

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  document.querySelectorAll("[data-export-test]").forEach((element) => element.remove());
});

function chart() {
  const container = document.createElement("div");
  container.dataset.exportTest = "";
  container.style.setProperty("--card", "#1a1a1a");
  container.style.color = "rgb(222, 222, 222)";
  container.innerHTML = '<svg class="recharts-surface" viewBox="0 0 320 200" aria-label="CPU (%)"><path class="theme-line" d="M0,0L10,10" /></svg><div class="recharts-legend-wrapper"><button aria-pressed="true"><div style="background-color: rgb(179, 157, 219)"></div>worker</button><button aria-pressed="false">hidden</button></div>';
  const stylesheet = document.createElement("style");
  stylesheet.textContent = ".theme-line { stroke: rgb(179, 157, 219); fill: none; }";
  container.append(stylesheet);
  document.body.append(container);
  const svg = container.querySelector("svg")!;
  vi.spyOn(svg, "getBoundingClientRect").mockReturnValue(new DOMRect(0, 0, 320, 200));
  return container;
}

it("downloads a PNG with resolved theme styles, real dimensions and visible series labels", async () => {
  const container = chart();
  const blobs: Blob[] = [];
  const createUrl = vi.fn((blob: Blob) => { blobs.push(blob); return "blob:" + blobs.length; });
  const revokeUrl = vi.fn();
  vi.stubGlobal("URL", { createObjectURL: createUrl, revokeObjectURL: revokeUrl });
  vi.stubGlobal("Image", class {
    onload: (() => void) | null = null;
    set src(_value: string) { queueMicrotask(() => this.onload?.()); }
  });
  const context = { scale: vi.fn(), fillRect: vi.fn(), fillText: vi.fn(), drawImage: vi.fn(), fillStyle: "", font: "" };
  vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockReturnValue(context as unknown as CanvasRenderingContext2D);
  vi.spyOn(HTMLCanvasElement.prototype, "toBlob").mockImplementation((callback) => callback(new Blob(["png"], { type: "image/png" })));
  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  await exportChartImage(container, "cpu");
  const svgText = await new Promise<string>((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.readAsText(blobs[0]);
  });
  expect(svgText).toContain("stroke: rgb(179, 157, 219)");
  expect(svgText).toContain('width="320"');
  expect(svgText).toContain('height="200"');
  expect(context.drawImage).toHaveBeenCalled();
  expect(context.fillText).toHaveBeenCalledWith("worker", 28, 248, 280);
  expect(context.fillText).not.toHaveBeenCalledWith("hidden", expect.anything(), expect.anything(), expect.anything());
  expect(blobs[1].type).toBe("image/png");
  expect(click).toHaveBeenCalledOnce();
  expect(click.mock.instances[0]).toHaveProperty("download", "cpu.png");
  expect(revokeUrl.mock.calls).toEqual([["blob:1"], ["blob:2"]]);
});

it("rejects image decode failures and releases the SVG without downloading a blank PNG", async () => {
  const container = chart();
  const revokeUrl = vi.fn();
  vi.stubGlobal("URL", { createObjectURL: () => "blob:failed", revokeObjectURL: revokeUrl });
  vi.stubGlobal("Image", class {
    onerror: (() => void) | null = null;
    set src(_value: string) { queueMicrotask(() => this.onerror?.()); }
  });
  const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
  await expect(exportChartImage(container, "cpu")).rejects.toThrow("could not load");
  expect(revokeUrl).toHaveBeenCalledWith("blob:failed");
  expect(click).not.toHaveBeenCalled();
});
