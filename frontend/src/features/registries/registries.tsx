import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, Cancel01Icon, CubeIcon, Delete02Icon, FloppyDiskIcon, LinkSquare01Icon, PencilEdit01Icon, Search01Icon } from "@hugeicons/core-free-icons";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useApi, useCommand } from "@/lib/queries";
import { segment, url } from "@/lib/api";
import { registryTypes, type Registry, type RegistryType } from "@/lib/types";
import {
  Action,
  Choice,
  ErrorMessage,
  Field,
  Loading,
  Page,
  Properties,
  ResourceLink,
  ResourceTable,
  Section,
  Submit,
  Toggle,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { useSession } from "../session/session";
import { canWrite } from "../resources/payload";
import {
  canManageRegistry,
  registryId,
  registryPayload,
  repositoryImage,
} from "./payload";

const names: Record<RegistryType, string> = {
  dockerhub: "Docker Hub",
  v2: "Registry V2 / GHCR",
  ecr: "Amazon ECR",
  acr: "Azure ACR",
  gitlab: "GitLab",
};
function name(registry: Registry) {
  return String(
    registry.type === "ecr"
      ? registry.user
      : registry.type === "acr"
        ? registry.spName
        : registry.type === "dockerhub" || registry.type === "gitlab"
          ? registry.username
          : registry.name,
  );
}
function address(registry: Registry) {
  return registry.type === "dockerhub"
    ? "https://hub.docker.com"
    : registry.url || "";
}
function registryPath(type: RegistryType, id?: string) {
  return "/api/registry/" + type + (id ? "/" + segment(id) : "");
}
function registryRoute(type: RegistryType, id: string) {
  return "/registries/" + type + "/" + segment(id);
}

export function RegistriesList() {
  const { me } = useSession();
  const [browsing, setBrowsing] = useState<Registry>();
  const dockerhub = useApi<Registry[]>(registryPath("dockerhub"));
  const v2 = useApi<Registry[]>(registryPath("v2"));
  const ecr = useApi<Registry[]>(registryPath("ecr"));
  const acr = useApi<Registry[]>(registryPath("acr"));
  const gitlab = useApi<Registry[]>(registryPath("gitlab"));
  const queries = [dockerhub, v2, ecr, acr, gitlab];
  const rows = queries.flatMap((query, i) =>
    (query.data || []).map((row) => ({ ...row, type: registryTypes[i]! })),
  );
  return (
    <Page
      title="Registries"
      description="Your linked accounts and public accounts shared with you"
      actions={
        canWrite(me?.role) && (
          <Button asChild>
            <Link to="/registries/create"><HugeiconsIcon icon={LinkSquare01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Link registry</Link>
          </Button>
        )
      }
    >
      {queries.map((query, i) => (
        <ErrorMessage key={registryTypes[i]} error={query.error} />
      ))}
      {queries.some((q) => q.isPending) ? (
        <Loading variant="table" />
      ) : (
        <ResourceTable
          rows={rows}
          sortBy={name}
          search={(r) => [name(r), address(r), r.owner, r.type].join(" ")}
          columns={[
            {
              label: "Name",
              render: (r) =>
                canManageRegistry(r, me) ? (
                  <ResourceLink to={registryRoute(r.type, registryId(r))}>
                    {name(r)}
                  </ResourceLink>
                ) : (
                  name(r)
                ),
            },
            { label: "Type", render: (r) => names[r.type] },
            { label: "URL", render: address },
            { label: "Owner", render: (r) => r.owner || "—" },
            { label: "Public", render: (r) => (r.public ? "Yes" : "No") },
            {
              label: "Repositories",
              render: (r) =>
                canWrite(me?.role) ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setBrowsing(r)}
                  >
                    <HugeiconsIcon icon={Search01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                    Browse {name(r)}
                  </Button>
                ) : (
                  "Read only"
                ),
            },
          ]}
        />
      )}
      {browsing && (
        <Section title={"Repositories · " + name(browsing)}>
          <Button variant="ghost" onClick={() => setBrowsing(undefined)}>
            <HugeiconsIcon icon={Cancel01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
            Close repositories
          </Button>
          <Repositories key={registryId(browsing)} registry={browsing} />
        </Section>
      )}
    </Page>
  );
}
export function RegistryDetail() {
  const { registryType, id = "" } = useParams();
  const { me } = useSession();
  const navigate = useNavigate();
  const valid = registryTypes.includes(registryType as RegistryType);
  const type = registryType as RegistryType;
  const query = useApi<Registry>(
    valid ? registryPath(type, id) : "",
    valid && canWrite(me?.role),
  );
  if (!valid) return <ErrorMessage error="Unsupported registry type." />;
  if (!canWrite(me?.role))
    return (
      <ErrorMessage error="Registry details require a user or administrator account." />
    );
  if (query.isPending) return <Loading />;
  if (!query.data)
    return <ErrorMessage error={query.error || "Registry not found."} />;
  const registry: Registry = { ...query.data, type };
  const managed = canManageRegistry(registry, me);
  return (
    <Page
      title={name(registry)}
      description={names[type]}
      actions={
        managed && (
          <>
            <Button variant="outline" asChild>
              <Link to={registryRoute(type, id) + "/edit"}><HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Edit registry</Link>
            </Button>
            <Action
              label="Delete registry"
              icon={Delete02Icon}
              path={registryPath(type, id)}
              method="DELETE"
              destructive
              confirm="Remove this linked registry account? Services already deployed are retained."
              onSuccess={() => navigate("/registries")}
            />
          </>
        )
      }
    >
      <ErrorMessage error={query.error} />
      <Section title="Registry details">
        <Properties
          values={{
            ID: registryId(registry),
            URL: address(registry),
            Owner: registry.owner,
            Public: registry.public ? "Yes" : "No",
            ...(type === "ecr"
              ? {
                  Region: String(registry.region),
                  "Access key ID": String(registry.accessKeyId),
                }
              : {}),
            ...(type === "acr"
              ? {
                  "Registry name": registry.name,
                  "Service principal": String(registry.spName),
                  "Service principal ID": String(registry.spId),
                }
              : {}),
            ...(type === "gitlab"
              ? {
                  "GitLab URL": String(registry.gitlabUrl),
                  Username: registry.username,
                }
              : {}),
            ...(type === "v2"
              ? {
                  "Custom API": registry.customApi ? "Yes" : "No",
                  Authentication: registry.withAuth ? "Yes" : "No",
                  Username: registry.username,
                }
              : {}),
            ...(type === "dockerhub"
              ? {
                  Username: registry.username,
                  Namespaces: Array.isArray(registry.namespaces)
                    ? registry.namespaces.join(", ")
                    : "—",
                }
              : {}),
          }}
        />
      </Section>
      <Section title="Repositories">
        <Repositories registry={registry} />
      </Section>
    </Page>
  );
}
interface Repository {
  id?: number;
  name: string;
  description?: string;
  private?: boolean;
}
function Repositories({ registry }: { registry: Registry }) {
  const query = useApi<Repository[]>(
    registryPath(registry.type, registryId(registry)) + "/repositories",
  );
  const [selected, setSelected] = useState("");
  let image = "";
  try {
    if (selected)
      image = repositoryImage(registry.type, address(registry), selected);
  } catch {
    image = "";
  }
  return (
    <div className="space-y-4">
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading variant="table" />
      ) : (
        query.data && (
          <ResourceTable
            rows={query.data}
            sortBy={(repository) => repository.name || ""}
            search={(r) => r.name}
            columns={[
              {
                label: "Repository",
                render: (r) => (
                  <Button
                    variant="link"
                    className="p-0"
                    onClick={() => setSelected(r.name)}
                  >
                    <HugeiconsIcon icon={CubeIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                    {r.name}
                  </Button>
                ),
              },
              { label: "Description", render: (r) => r.description || "—" },
              {
                label: "Access",
                render: (r) =>
                  r.private === undefined
                    ? "—"
                    : r.private
                      ? "Private"
                      : "Public",
              },
            ]}
          />
        )
      )}
      {image && <RepositoryTags key={image} image={image} />}
    </div>
  );
}
function RepositoryTags({ image }: { image: string }) {
  const tags = useApi<string[]>(
    url("/api/repository/tags", { repository: image }),
  );
  const [tag, setTag] = useState("");
  const ports = useApi<{ containerPort: number; protocol: string }[]>(
    url("/api/repository/ports", { repository: image, repositoryTag: tag }),
    !!tag,
  );
  return (
    <div className="space-y-4 rounded-lg border p-4">
      <h3 className="font-medium">{image}</h3>
      <ErrorMessage error={tags.error} />
      {tags.isPending ? (
        <Loading />
      ) : tags.data?.length ? (
        <Choice
          label="Image tag"
          value={tag}
          options={["", ...tags.data]}
          onChange={setTag}
        />
      ) : (
        <p>No tags available.</p>
      )}
      {tag && (
        <>
          <ErrorMessage error={ports.error} />
          {ports.isPending ? (
            <Loading />
          ) : (
            <Properties
              values={{
                Image: image + ":" + tag,
                "Exposed ports":
                  ports.data
                    ?.map((p) => p.containerPort + "/" + p.protocol)
                    .join(", ") || "None",
              }}
            />
          )}
          <Button asChild>
            <Link
              to={url("/services/create/wizard/config", {
                repository: image,
                tag,
              })}
            >
              <HugeiconsIcon icon={Add01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Create service
            </Link>
          </Button>
        </>
      )}
    </div>
  );
}
function defaults(type: RegistryType): Record<string, unknown> {
  switch (type) {
    case "dockerhub":
      return { username: "", password: "", public: false };
    case "v2":
      return {
        name: "",
        url: "",
        username: "",
        password: "",
        public: false,
        withAuth: false,
        customApi: false,
      };
    case "ecr":
      return {
        user: "swarmpit-ecr",
        region: "eu-west-1",
        accessKeyId: "",
        accessKey: "",
        public: false,
      };
    case "acr":
      return {
        name: "",
        spName: "swarmpit-acr",
        spId: "",
        spPassword: "",
        public: false,
      };
    case "gitlab":
      return {
        hosted: false,
        gitlabUrl: "https://gitlab.com",
        url: "https://registry.gitlab.com",
        username: "",
        token: "",
        public: false,
      };
  }
}
export function RegistryForm() {
  const { registryType, id } = useParams();
  const { me } = useSession();
  const [selected, setSelected] = useState<RegistryType>("dockerhub");
  const valid =
    !registryType || registryTypes.includes(registryType as RegistryType);
  const type = (registryType || selected) as RegistryType;
  const query = useApi<Registry>(
    valid ? registryPath(type, id) : "",
    !!id && valid && canWrite(me?.role),
  );
  if (!valid) return <ErrorMessage error="Unsupported registry type." />;
  if (!canWrite(me?.role))
    return (
      <ErrorMessage error="Linking registries requires a user or administrator account." />
    );
  if (id && query.isPending) return <Loading />;
  if (id && !query.data)
    return <ErrorMessage error={query.error || "Registry not found."} />;
  if (id && query.data && !canManageRegistry(query.data, me))
    return (
      <ErrorMessage error="Only the registry owner can edit this account." />
    );
  return (
    <Page title={id ? "Edit registry" : "Link registry"}>
      {!id && !registryType && (
        <Choice
          label="Registry type"
          value={selected}
          options={registryTypes}
          onChange={(v) => setSelected(v as RegistryType)}
        />
      )}
      <ErrorMessage error={query.error} />
      <RegistryEditor
        key={type + ":" + (id || "new")}
        type={type}
        existing={id ? query.data : undefined}
      />
    </Page>
  );
}
function RegistryEditor({
  type,
  existing,
}: {
  type: RegistryType;
  existing?: Registry;
}) {
  const navigate = useNavigate();
  const [values, setValues] = useState<Record<string, unknown>>(() => ({
    ...defaults(type),
    ...existing,
    password: "",
    accessKey: "",
    spPassword: "",
    token: "",
  }));
  const command = useCommand<Record<string, unknown> | undefined>((data) => {
    const id = existing ? registryId(existing) : registryId(data || {});
    navigate(id ? registryRoute(type, id) : "/registries");
  });
  const set = (key: string, value: unknown) =>
    setValues((old) => ({ ...old, [key]: value }));
  function field(key: string, label: string, secret = false, disabled = false) {
    return (
      <Field
        key={key}
        label={label}
        value={String(values[key] ?? "")}
        type={
          secret
            ? "password"
            : key === "url" || key === "gitlabUrl"
              ? "url"
              : "text"
        }
        disabled={disabled}
        required={secret ? !existing : true}
        autoComplete={secret ? "new-password" : "off"}
        hint={
          secret && existing
            ? "Leave blank to keep the stored credential."
            : undefined
        }
        onChange={(e) => set(key, e.target.value)}
      />
    );
  }
  return (
    <Section title={names[type]}>
      <form
        className="max-w-xl space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          if (command.isPending) return;
          command.mutate({
            path: registryPath(
              type,
              existing ? registryId(existing) : undefined,
            ),
            body: registryPayload(
              type,
              {
                ...values,
                ...(existing
                  ? { _id: registryId(existing), _rev: existing._rev }
                  : {}),
              },
              !!existing,
            ),
          });
        }}
      >
        {type === "dockerhub" && (
          <>
            {field("username", "Username", false, !!existing)}
            {field("password", existing ? "New password" : "Password", true)}
          </>
        )}
        {type === "v2" && (
          <>
            {field("name", "Name", false, !!existing)}
            {field("url", "Registry URL")}
            <p className="text-sm text-muted-foreground">
              Use Registry V2 for GitHub Container Registry (ghcr.io).
            </p>
            <Toggle
              label="Custom API (URL already includes the API path)"
              checked={!!values.customApi}
              onChange={(v) => set("customApi", v)}
            />
            <Toggle
              label="Use authentication"
              checked={!!values.withAuth}
              onChange={(v) => set("withAuth", v)}
            />
            {!!values.withAuth && (
              <>
                {field("username", "Username")}
                {field(
                  "password",
                  existing ? "New password" : "Password",
                  true,
                )}
              </>
            )}
          </>
        )}
        {type === "ecr" && (
          <>
            {field("user", "User", false, !!existing)}
            {field("region", "Region")}
            {field("accessKeyId", "Access key ID")}
            {field("accessKey", "Secret access key", true)}
          </>
        )}
        {type === "acr" && (
          <>
            {field("name", "Registry name", false, !!existing)}
            {field("spName", "Service principal name", false, !!existing)}
            {field("spId", "Service principal ID")}
            {field("spPassword", "Service principal password", true)}
          </>
        )}
        {type === "gitlab" && (
          <>
            {!existing && (
              <Toggle
                label="Self-hosted GitLab"
                checked={!!values.hosted}
                onChange={(hosted) =>
                  setValues((old) => ({
                    ...old,
                    hosted,
                    ...(!hosted
                      ? {
                          gitlabUrl: "https://gitlab.com",
                          url: "https://registry.gitlab.com",
                        }
                      : {}),
                  }))
                }
              />
            )}
            <>
              {field(
                "gitlabUrl",
                "GitLab URL",
                false,
                !!existing || !values.hosted,
              )}
              {field(
                "url",
                "Registry URL",
                false,
                !!existing || !values.hosted,
              )}
            </>
            {field("username", "Username", false, !!existing)}
            {field("token", "Personal access token", true)}
            <p className="text-sm text-muted-foreground">
              The personal access token needs API scope.
            </p>
          </>
        )}
        <Toggle
          label="Make account public"
          checked={!!values.public}
          onChange={(v) => set("public", v)}
        />
        <p className="text-sm text-muted-foreground">
          A public account lets other users search and deploy its private
          repositories.
        </p>
        <ErrorMessage error={command.error} />
        <Submit pending={command.isPending} icon={existing ? FloppyDiskIcon : LinkSquare01Icon}>
          {existing ? "Save changes" : "Link registry"}
        </Submit>
      </form>
    </Section>
  );
}
