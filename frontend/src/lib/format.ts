export const date = (value?: string) =>
  value ? new Date(value).toLocaleString() : "—";
export function bytes(value?: number, binary = true) {
  if (value == null) return "—";
  if (!value) return "0 B";
  const base = binary ? 1024 : 1000;
  const units = binary ? ["B", "KiB", "MiB", "GiB", "TiB"] : ["B", "kB", "MB", "GB", "TB"];
  const i = Math.min(Math.floor(Math.log(Math.abs(value)) / Math.log(base)), 4);
  return (
    (value / base ** i).toFixed(i ? 1 : 0) +
    " " +
    units[i]
  );
}
export function encodeText(value: string) {
  const bytes = new TextEncoder().encode(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}
export function decodeText(value: string) {
  try {
    return new TextDecoder().decode(
      Uint8Array.from(atob(value), (c) => c.charCodeAt(0)),
    );
  } catch {
    return value;
  }
}
