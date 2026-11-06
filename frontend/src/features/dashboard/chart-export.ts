const svgStyleProperties = [
  "fill", "fill-opacity", "stroke", "stroke-width", "stroke-opacity", "stroke-dasharray",
  "opacity", "font-family", "font-size", "font-weight", "text-anchor", "dominant-baseline",
];

export async function exportChartImage(container: HTMLDivElement | null, filename: string) {
  const source = container?.querySelector<SVGSVGElement>("svg.recharts-surface");
  if (!container || !source) throw new Error("Chart unavailable");
  const { width, height } = source.getBoundingClientRect();
  if (width <= 0 || height <= 0) throw new Error("Chart is not visible");
  const clone = source.cloneNode(true) as SVGSVGElement;
  const originals = [source, ...source.querySelectorAll<SVGElement>("*")];
  const copies = [clone, ...clone.querySelectorAll<SVGElement>("*")];
  // Resolve theme variables and stylesheet rules before the SVG leaves the document.
  originals.forEach((element, index) => {
    const style = getComputedStyle(element);
    for (const property of svgStyleProperties) {
      copies[index].style.setProperty(property, style.getPropertyValue(property));
    }
  });
  clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
  clone.setAttribute("width", String(width));
  clone.setAttribute("height", String(height));
  const svgUrl = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(clone)], { type: "image/svg+xml" }));
  let pngUrl: string | undefined;
  try {
    const image = new Image();
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve();
      image.onerror = () => reject(new Error("Chart image could not load"));
      image.src = svgUrl;
    });
    const legend = [...container.querySelectorAll<HTMLButtonElement>(".recharts-legend-wrapper button[aria-pressed=true]")];
    const canvas = document.createElement("canvas");
    const scale = 2;
    canvas.width = Math.ceil(width * scale);
    canvas.height = Math.ceil((height + 32 + legend.length * 20) * scale);
    const context = canvas.getContext("2d");
    if (!context) throw new Error("Canvas unavailable");
    context.scale(scale, scale);
    const style = getComputedStyle(container);
    context.fillStyle = style.getPropertyValue("--card").trim() || "#fff";
    context.fillRect(0, 0, canvas.width / scale, canvas.height / scale);
    context.fillStyle = style.color;
    context.font = "14px " + style.fontFamily;
    context.fillText(source.getAttribute("aria-label")?.split(": resource samples from")[0] || filename, 12, 20, width - 24);
    context.drawImage(image, 0, 32, width, height);
    legend.forEach((item, index) => {
      const y = height + 48 + index * 20;
      const swatch = item.querySelector<HTMLElement>("div");
      context.fillStyle = swatch ? getComputedStyle(swatch).backgroundColor : style.color;
      context.fillRect(12, y - 9, 8, 8);
      context.fillStyle = style.color;
      context.fillText(item.textContent || "", 28, y, width - 40);
    });
    const png = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error("PNG encoding failed")), "image/png");
    });
    pngUrl = URL.createObjectURL(png);
    const link = document.createElement("a");
    link.href = pngUrl;
    link.download = filename + ".png";
    link.click();
  } finally {
    URL.revokeObjectURL(svgUrl);
    if (pngUrl) URL.revokeObjectURL(pngUrl);
  }
}
