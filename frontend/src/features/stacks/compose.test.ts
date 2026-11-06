import { describe, expect, it } from "vitest";
import { composeChanges, restoreCompose } from "./compose";

describe("compose comparison and restoration", () => {
  it("does not invent changes at the trailing newline", () => {
    expect(
      composeChanges("# comment\nservices: {}\n", "# comment\nservices: {}\n"),
    ).toEqual([]);
  });
  it("restores one engine hunk without discarding unrelated draft edits", () => {
    const baseline = "# header\nservices:\n  app: old\n\nvolumes: {}\n";
    const current = "# header\nservices:\n  app: new\n\nvolumes: {data: {}}\n";
    const changes = composeChanges(baseline, current);
    expect(changes).toHaveLength(2);
    expect(restoreCompose(current, changes[0]!)).toBe(
      "# header\nservices:\n  app: old\n\nvolumes: {data: {}}\n",
    );
  });
  it("restores deleted lines at the end without losing newline semantics", () => {
    for (const [baseline, current] of [
      ["a\nb", "a"],
      ["a\nb\n", "a\n"],
      ["a\n", "a"],
    ]) {
      const change = composeChanges(baseline!, current!)[0]!;
      expect(restoreCompose(current!, change)).toBe(baseline);
    }
  });
  it("can remove added lines and restore a completely empty document", () => {
    const changes = composeChanges("", "# draft\nservices: {}\n");
    expect(restoreCompose("# draft\nservices: {}\n", changes[0]!)).toBe("");
  });
});
