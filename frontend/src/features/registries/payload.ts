import type { RegistryType } from "@/lib/types";
const fields: Record<RegistryType, string[]> = {
  dockerhub: ["username", "password", "public"],
  v2: [
    "name",
    "url",
    "public",
    "customApi",
    "withAuth",
    "username",
    "password",
  ],
  ecr: ["user", "region", "accessKeyId", "accessKey", "public"],
  acr: ["name", "url", "spName", "spId", "spPassword", "public"],
  gitlab: ["hosted", "gitlabUrl", "url", "username", "token", "public"],
};
export function registryPayload(
  type: RegistryType,
  values: Record<string, unknown>,
  editing = false,
): Record<string, unknown> {
  const keys = editing ? ["_id", "_rev", ...fields[type]] : fields[type];
  return Object.fromEntries(
    keys
      .filter(
        (key) =>
          values[key] !== undefined &&
          !(
            editing &&
            ["password", "accessKey", "spPassword", "token"].includes(key) &&
            values[key] === ""
          ),
      )
      .map((key) => [key, values[key]]),
  );
}
export function registryId(value: Record<string, unknown>): string {
  return String(value._id || value.id || "");
}
export function canManageRegistry(
  registry: { owner?: string; public?: boolean },
  me?: { username: string; role: string },
): boolean {
  return (
    !!me &&
    ["admin", "user"].includes(me.role) &&
    registry.owner === me.username
  );
}
export function repositoryImage(
  type: RegistryType,
  registryUrl: string,
  name: string,
): string {
  if (type === "dockerhub") return name;
  const host = new URL(registryUrl).host;
  return name.startsWith(host + "/") ? name : host + "/" + name;
}
