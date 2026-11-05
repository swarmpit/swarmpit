import { afterEach, expect, it, vi } from "vitest";
import { encodeSubscription, connectEvents } from "./events";
import { api } from "./api";
vi.mock("./api", () => ({
  api: vi.fn(),
  url: (p: string, q: Record<string, string>) =>
    p + "?" + new URLSearchParams(q),
}));
afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});
it("encodes keyword route maps with escaped Unicode parameters", () => {
  const value = encodeSubscription({
    handler: "service-info",
    params: { id: 'a"\\雪' },
  });
  const decoded = new TextDecoder().decode(
    Uint8Array.from(atob(value), (c) => c.charCodeAt(0)),
  );
  expect(decoded).toBe('{:handler :service-info :params {:id "a\\"\\\\雪"}}');
});
it("rejects arbitrary EDN keyword injection", () => {
  expect(() =>
    encodeSubscription({ handler: "index } :pwn", params: {} }),
  ).toThrow();
});
it("fetches a new single-use token on errors and cancels stream and timer", async () => {
  vi.useFakeTimers();
  const streams: {
    onerror: (() => void) | null;
    close: ReturnType<typeof vi.fn>;
  }[] = [];
  class Stream {
    onmessage = null;
    onopen = null;
    onerror = null;
    close = vi.fn();
    constructor(public url: string) {
      streams.push(this);
    }
  }
  vi.stubGlobal("EventSource", Stream);
  vi.mocked(api)
    .mockResolvedValueOnce({ slt: "one" })
    .mockResolvedValueOnce({ slt: "two" });
  const stop = connectEvents({ handler: "index", params: {} }, vi.fn());
  await vi.waitFor(() => expect(streams).toHaveLength(1));
  streams[0].onerror?.();
  expect(streams[0].close).toHaveBeenCalled();
  await vi.advanceTimersByTimeAsync(5000);
  expect(api).toHaveBeenCalledTimes(2);
  expect(streams).toHaveLength(2);
  stop();
  expect(streams[1].close).toHaveBeenCalled();
  await vi.advanceTimersByTimeAsync(10000);
  expect(api).toHaveBeenCalledTimes(2);
});
it("does not open a stream after its route has unmounted", async () => {
  let resolve!: (value: { slt: string }) => void;
  vi.mocked(api).mockReturnValue(
    new Promise((r) => {
      resolve = r;
    }),
  );
  const source = vi.fn();
  vi.stubGlobal("EventSource", source);
  const stop = connectEvents({ handler: "index", params: {} }, vi.fn());
  stop();
  resolve({ slt: "late" });
  await Promise.resolve();
  expect(source).not.toHaveBeenCalled();
});
