import { expect, it } from "vitest";
import { userPayload } from "./payload";
it("preserves user identity, dashboards and API token metadata without resubmitting a password", () => {
  const existing = {
    _id: "u1",
    _rev: "3",
    type: "user",
    username: "alice",
    email: "old@example.com",
    role: "viewer" as const,
    password: "server-hash",
    "node-dashboard": ["node1"],
    "service-dashboard": ["svc1"],
    "api-token": {
      jti: "jti",
      mask: "12345",
      expiresAt: "2027-01-01T00:00:00Z",
    },
  };
  expect(
    userPayload(
      {
        username: "ignored",
        email: "new@example.com",
        role: "user",
        password: "ignored",
      },
      existing,
    ),
  ).toEqual({
    _id: "u1",
    _rev: "3",
    type: "user",
    username: "alice",
    email: "new@example.com",
    role: "user",
    "node-dashboard": ["node1"],
    "service-dashboard": ["svc1"],
    "api-token": {
      jti: "jti",
      mask: "12345",
      expiresAt: "2027-01-01T00:00:00Z",
    },
  });
});
it("includes a new password only for user creation", () => {
  expect(
    userPayload({
      username: "bob",
      email: "bob@example.com",
      role: "viewer",
      password: "secret",
    }),
  ).toEqual({
    username: "bob",
    email: "bob@example.com",
    role: "viewer",
    password: "secret",
  });
});
