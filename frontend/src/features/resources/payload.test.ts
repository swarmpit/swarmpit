import { expect, it } from "vitest";
import { contentPayload, canWrite, canAdmin } from "./payload";
it("encodes Unicode secret and config content as UTF-8 base64", () => {
  const p = contentPayload("secrets", "my-secret", "Hello 雪");
  expect(p).toEqual({ secretName: "my-secret", data: "SGVsbG8g6Zuq" });
});
it("keeps viewers read-only and admin operations admin-only", () => {
  expect(canWrite("viewer")).toBe(false);
  expect(canWrite(undefined)).toBe(false);
  expect(canWrite("user")).toBe(true);
  expect(canAdmin("user")).toBe(false);
  expect(canAdmin("admin")).toBe(true);
});
