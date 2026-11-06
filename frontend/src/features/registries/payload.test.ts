import { expect, it } from "vitest";
import {
  registryPayload,
  registryId,
  canManageRegistry,
  repositoryImage,
} from "./payload";
it("uses exact ECR fields and omits blank replacement secrets while retaining record identity", () => {
  expect(
    registryPayload(
      "ecr",
      {
        _id: "reg1",
        _rev: "2",
        user: "deploy",
        region: "eu-west-1",
        accessKeyId: "key-id",
        accessKey: "",
        public: true,
        password: "wrong-field",
      },
      true,
    ),
  ).toEqual({
    _id: "reg1",
    _rev: "2",
    user: "deploy",
    region: "eu-west-1",
    accessKeyId: "key-id",
    public: true,
  });
});
it("creates ACR and GitLab using provider-specific credentials", () => {
  expect(
    registryPayload("acr", {
      name: "myacr",
      spName: "deploy",
      spId: "app-id",
      spPassword: "secret",
      public: false,
    }),
  ).toEqual({
    name: "myacr",
    spName: "deploy",
    spId: "app-id",
    spPassword: "secret",
    public: false,
  });
  expect(
    registryPayload("gitlab", {
      hosted: true,
      gitlabUrl: "https://git.example",
      url: "https://registry.example",
      username: "deploy",
      token: "pat",
      public: false,
    }),
  ).toEqual({
    hosted: true,
    gitlabUrl: "https://git.example",
    url: "https://registry.example",
    username: "deploy",
    token: "pat",
    public: false,
  });
});
it("does not clear stored Docker Hub and v2 passwords when editing", () => {
  expect(
    registryPayload(
      "dockerhub",
      { _id: "hub", username: "me", password: "", public: false },
      true,
    ),
  ).toEqual({ _id: "hub", username: "me", public: false });
  expect(
    registryPayload(
      "v2",
      {
        _id: "v2",
        name: "GHCR",
        url: "https://ghcr.io",
        username: "me",
        password: "",
        withAuth: true,
        customApi: true,
        public: false,
      },
      true,
    ),
  ).not.toHaveProperty("password");
});
it("uses backend IDs and restricts management of public accounts to their owner", () => {
  expect(registryId({ id: "new" })).toBe("new");
  expect(
    canManageRegistry(
      { owner: "someone", public: true },
      { username: "admin", role: "admin" },
    ),
  ).toBe(false);
  expect(
    canManageRegistry({ owner: "me" }, { username: "me", role: "user" }),
  ).toBe(true);
  expect(
    canManageRegistry({ owner: "me" }, { username: "me", role: "viewer" }),
  ).toBe(false);
  expect(repositoryImage("v2", "https://ghcr.io", "org/app")).toBe(
    "ghcr.io/org/app",
  );
  expect(
    repositoryImage("dockerhub", "https://hub.docker.com", "org/app"),
  ).toBe("org/app");
});
