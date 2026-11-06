export class ApiError extends Error {
  constructor(
    public status: number,
    public backend: boolean,
    public body: unknown,
  ) {
    super(
      typeof body === "object" && body !== null && "error" in body
        ? String(body.error)
        : typeof body === "string"
          ? body
          : "Request failed (" + status + ")",
    );
    this.name = "ApiError";
  }
}
export function isSessionError(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401 && error.backend;
}
let discardedToken: string | undefined;
export function storedToken(): string | null {
  try {
    const value = localStorage.getItem("token");
    return value &&
      value !== discardedToken &&
      !["null", "undefined"].includes(value)
      ? value
      : null;
  } catch {
    return null;
  }
}
export function saveToken(token: string | null) {
  if (token) {
    localStorage.setItem("token", token);
    discardedToken = undefined;
  } else {
    discardedToken = storedToken() ?? discardedToken;
    localStorage.removeItem("token");
  }
}
export type ApiOptions = Omit<RequestInit, "body"> & { body?: unknown };
export async function api<T = void>(
  path: string,
  options: ApiOptions = {},
): Promise<T> {
  const { body, ...init } = options;
  const headers = new Headers(init.headers);
  const token = storedToken();
  if (!headers.has("Authorization") && token)
    headers.set("Authorization", token);
  headers.set("Accept", "application/json");
  if (body !== undefined) headers.set("Content-Type", "application/json");
  const response = await fetch(path, {
    ...init,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    credentials: "same-origin",
  });
  const text = await response.text();
  let data: unknown;
  try {
    data = text ? JSON.parse(text) : undefined;
  } catch {
    data = text;
  }
  if (!response.ok)
    throw new ApiError(
      response.status,
      response.headers.get("X-Backend-Server") === "swarmpit",
      data,
    );
  return data as T;
}
export const segment = (value: string) => encodeURIComponent(value);
export function url(
  path: string,
  params: Record<string, string | number | boolean | undefined> = {},
) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params))
    if (value !== undefined) query.set(key, String(value));
  return path + (query.size ? "?" + query.toString() : "");
}
export function basicAuth(username: string, password: string) {
  const bytes = new TextEncoder().encode(username + ":" + password);
  return "Basic " + btoa(String.fromCharCode(...bytes));
}
