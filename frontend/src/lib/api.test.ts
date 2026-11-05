import { beforeEach, expect, it, vi } from "vitest";
import { api, ApiError, isSessionError } from "./api";

beforeEach(() => {
  localStorage.clear();
  vi.stubGlobal("fetch", vi.fn());
});
it("forwards the returned authorization value once and accepts an empty body", async () => {
  localStorage.setItem("token", "Bearer signed-token");
  vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 202 }));
  await expect(
    api("/api/services/x/redeploy", { method: "POST" }),
  ).resolves.toBeUndefined();
  expect(
    new Headers(vi.mocked(fetch).mock.calls[0][1]?.headers).get(
      "Authorization",
    ),
  ).toBe("Bearer signed-token");
});
it("uses caller Basic authorization instead of the saved session", async () => {
  localStorage.setItem("token", "Bearer old");
  vi.mocked(fetch).mockResolvedValue(new Response('{"token":"Bearer fresh"}'));
  await api("/login", {
    method: "POST",
    headers: { Authorization: "Basic abc" },
  });
  expect(
    new Headers(vi.mocked(fetch).mock.calls[0][1]?.headers).get(
      "Authorization",
    ),
  ).toBe("Basic abc");
});
it("distinguishes registry errors from a rejected Swarmpit session", async () => {
  vi.mocked(fetch).mockResolvedValueOnce(
    new Response('{"error":"Registry denied"}', { status: 401 }),
  );
  const registry = await api("/api/registry/v2/a").catch((e) => e);
  expect(registry).toBeInstanceOf(ApiError);
  expect(isSessionError(registry)).toBe(false);
  vi.mocked(fetch).mockResolvedValueOnce(
    new Response('{"error":"Token expired"}', {
      status: 401,
      headers: { "X-Backend-Server": "swarmpit" },
    }),
  );
  expect(isSessionError(await api("/api/me").catch((e) => e))).toBe(true);
});
it("does not send blank or malformed saved authorization", async () => {
  for (const token of ["null", "undefined", ""]) {
    localStorage.setItem("token", token);
    vi.mocked(fetch).mockResolvedValue(new Response("{}"));
    await api("/version");
    expect(
      new Headers(vi.mocked(fetch).mock.lastCall?.[1]?.headers).has(
        "Authorization",
      ),
    ).toBe(false);
  }
});
it("sends JSON and preserves cancellation", async () => {
  const signal = new AbortController().signal;
  vi.mocked(fetch).mockResolvedValue(new Response("{}"));
  await api("/initialize", {
    method: "POST",
    body: { username: "admin" },
    signal,
  });
  expect(vi.mocked(fetch).mock.lastCall?.[1]).toMatchObject({
    signal,
    body: '{"username":"admin"}',
  });
});
