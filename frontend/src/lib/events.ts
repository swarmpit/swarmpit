import { api, url, isSessionError } from "./api";
import { encodeText } from "./format";
export interface Subscription {
  handler: string;
  params: Record<string, string>;
}
export function encodeSubscription({ handler, params }: Subscription) {
  const keyword = /^[a-z][a-zA-Z0-9-]*$/;
  if (
    !keyword.test(handler) ||
    Object.keys(params).some((key) => !keyword.test(key))
  )
    throw new Error("Invalid event subscription");
  const body = Object.entries(params)
    .map(([key, value]) => ":" + key + " " + JSON.stringify(value))
    .join(" ");
  return encodeText("{:handler :" + handler + " :params {" + body + "}}");
}
export function connectEvents(
  subscription: Subscription,
  onData: (data: unknown) => void,
  onStatus?: (status: "connecting" | "live" | "reconnecting") => void,
) {
  let closed = false;
  let stream: EventSource | undefined;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let controller: AbortController | undefined;
  const encoded = encodeSubscription(subscription);
  async function open() {
    controller = new AbortController();
    onStatus?.("connecting");
    try {
      const { slt } = await api<{ slt: string }>("/slt", {
        signal: controller.signal,
      });
      if (closed) return;
      stream = new EventSource(url("/events", { slt, subscription: encoded }));
      stream.onopen = () => {
        if (!closed) onStatus?.("live");
      };
      stream.onmessage = (event) => {
        if (closed) return;
        try {
          onData(JSON.parse(event.data));
        } catch {
          /* Ignore incomplete event frames; the next snapshot refreshes the view. */
        }
      };
      stream.onerror = retry;
    } catch (error) {
      if (isSessionError(error)) {
        window.dispatchEvent(new Event("swarmpit:session-expired"));
        return;
      }
      if (!closed) retry();
    }
  }
  function retry() {
    stream?.close();
    if (!closed && !timer) {
      onStatus?.("reconnecting");
      timer = setTimeout(() => {
        timer = undefined;
        void open();
      }, 5000);
    }
  }
  void open();
  return () => {
    closed = true;
    controller?.abort();
    stream?.close();
    clearTimeout(timer);
  };
}
