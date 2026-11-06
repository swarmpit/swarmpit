import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, writeFileSync, rmSync, readFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { execFileSync, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
const script = fileURLToPath(new URL("./check-upstream.sh", import.meta.url));
function fixture(run) {
  const cwd = mkdtempSync(join(tmpdir(), "swarmpit-upstream-"));
  const git = (...args) =>
    execFileSync("git", args, {
      cwd,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"],
    }).trim();
  try {
    git("init", "-b", "react-ui");
    git("config", "user.name", "Fixture");
    git("config", "user.email", "fixture@example.invalid");
    writeFileSync(join(cwd, "backend"), "base\n");
    git("add", ".");
    git("commit", "-m", "test: initial");
    git("branch", "upstream");
    run({
      cwd,
      git,
      check: () =>
        spawnSync("bash", [script, "HEAD", "upstream"], {
          cwd,
          encoding: "utf8",
        }),
    });
  } finally {
    rmSync(cwd, { recursive: true, force: true });
  }
}
test("up-to-date history is a no-op and leaves HEAD unchanged", () =>
  fixture(({ git, check }) => {
    const head = git("rev-parse", "HEAD");
    const result = check();
    assert.equal(result.status, 0);
    assert.match(result.stdout, /changed=false/);
    assert.equal(git("rev-parse", "HEAD"), head);
    assert.equal(git("status", "--porcelain"), "");
  }));
test("upstream additions can merge without modifying the checkout", () =>
  fixture(({ cwd, git, check }) => {
    git("switch", "upstream");
    writeFileSync(join(cwd, "new-backend"), "update");
    git("add", ".");
    git("commit", "-m", "test: upstream");
    git("switch", "react-ui");
    writeFileSync(join(cwd, "react-ui"), "custom");
    git("add", ".");
    git("commit", "-m", "test: custom");
    const head = git("rev-parse", "HEAD");
    const result = check();
    assert.equal(result.status, 0);
    assert.match(result.stdout, /changed=true/);
    assert.match(result.stdout, /conflict=false/);
    assert.equal(git("rev-parse", "HEAD"), head);
    assert.equal(git("status", "--porcelain"), "");
  }));
test("conflicts are reported without discarding custom changes", () =>
  fixture(({ cwd, git, check }) => {
    git("switch", "upstream");
    writeFileSync(join(cwd, "backend"), "upstream\n");
    git("add", ".");
    git("commit", "-m", "test: upstream");
    git("switch", "react-ui");
    writeFileSync(join(cwd, "backend"), "custom\n");
    git("add", ".");
    git("commit", "-m", "test: custom");
    const result = check();
    assert.equal(result.status, 2);
    assert.match(result.stdout, /conflict=true/);
    assert.equal(readFileSync(join(cwd, "backend"), "utf8"), "custom\n");
    assert.equal(git("status", "--porcelain"), "");
  }));
test("invalid refs fail before any merge", () =>
  fixture(({ git }) => {
    const result = spawnSync("bash", [script, "HEAD", "missing-ref"], {
      cwd: git("rev-parse", "--show-toplevel"),
      encoding: "utf8",
    });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /Invalid upstream reference/);
  }));
