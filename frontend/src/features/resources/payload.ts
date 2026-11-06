import { encodeText } from "@/lib/format";
export function canWrite(role?: string) {
  return role === "admin" || role === "user";
}
export function canAdmin(role?: string) {
  return role === "admin";
}
export function contentPayload(
  kind: "secrets" | "configs",
  name: string,
  contents: string,
) {
  return {
    [kind === "secrets" ? "secretName" : "configName"]: name,
    data: encodeText(contents),
  };
}
