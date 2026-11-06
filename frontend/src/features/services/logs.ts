export function supportsLogs(driver?: string) {
  return ["json-file", "journald", "local", "loki"].includes(driver || "");
}
