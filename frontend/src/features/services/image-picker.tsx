import { Fragment, useId, useState } from "react";
import { useNavigate } from "react-router";
import { useApi } from "@/lib/queries";
import { segment, url } from "@/lib/api";
import { registryTypes, type Registry } from "@/lib/types";
import {
  Page,
  Section,
  Field,
  ErrorMessage,
  Loading,
  ResourceTable,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useSession } from "@/features/session/session";
import { HugeiconsIcon } from "@hugeicons/react";
import { ArrowLeft02Icon, ArrowRight02Icon, CubeIcon, Search01Icon, Settings01Icon } from "@hugeicons/core-free-icons";

interface Repository {
  name: string;
  description?: string;
  stars?: number;
  pulls?: number;
  official?: boolean;
  private?: boolean;
}
function RegistrySelect({
  id,
  value,
  onValueChange,
}: {
  id: string;
  value: string;
  onValueChange: (value: string) => void;
}) {
  // Keep provider queries active while the menu is closed.
  const sources = {
    dockerhub: useApi<Registry[]>("/api/registry/dockerhub"),
    v2: useApi<Registry[]>("/api/registry/v2"),
    ecr: useApi<Registry[]>("/api/registry/ecr"),
    acr: useApi<Registry[]>("/api/registry/acr"),
    gitlab: useApi<Registry[]>("/api/registry/gitlab"),
  };
  const options = registryTypes.flatMap((type) =>
    (sources[type].data || []).map((r) => ({
      value: type + "/" + r._id,
      label:
        type + " · " + (r.name || r.username || String(r.user || r.spName || r._id)),
    })),
  );
  const selectedLabel =
    value === "public"
      ? "Public Docker Hub"
      : options.find((option) => option.value === value)?.label || value;
  return (
    <Select value={value} onValueChange={onValueChange}>
      <SelectTrigger id={id} className="w-full">
        <SelectValue>{selectedLabel}</SelectValue>
      </SelectTrigger>
      <SelectContent>
        <SelectItem value="public">Public Docker Hub</SelectItem>
        {registryTypes.map((type) => {
          const query = sources[type];
          return (
            <Fragment key={type}>
              {query.error ? (
                <SelectItem value={type + ":error"} disabled>
                  {type} unavailable: {query.error.message}
                </SelectItem>
              ) : query.isPending && query.isFetching ? (
                <SelectItem value={type + ":loading"} disabled>
                  Loading {type} registries…
                </SelectItem>
              ) : null}
              {options
                .filter((option) => option.value.startsWith(type + "/"))
                .map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
            </Fragment>
          );
        })}
      </SelectContent>
    </Select>
  );
}
function RepositoryResults({
  registry,
  search,
  page,
  select,
}: {
  registry: string;
  search: string;
  page: number;
  select: (repository: string) => void;
}) {
  const publicSource = registry === "public";
  const path = publicSource
    ? url("/api/public/repositories", { query: search, page })
    : "/api/registry/" +
      registry.split("/").map(segment).join("/") +
      "/repositories";
  const query = useApi<
    Repository[] | { results: Repository[]; next?: string | null }
  >(path, !publicSource || !!search.trim());
  const [type, id] = registry.split("/");
  const accounts = useApi<Registry[]>(
    "/api/registry/" + segment(type || ""),
    !publicSource,
  );
  const account = accounts.data?.find((row) => row._id === id);
  const repositories = Array.isArray(query.data)
    ? query.data
    : query.data?.results || [];
  const filtered = publicSource
    ? repositories
    : repositories.filter((r) =>
        r.name.toLowerCase().includes(search.toLowerCase()),
      );
  function choose(name: string) {
    const prefix = account?.url?.replace(/^https?:\/\//, "").replace(/\/$/, "");
    select(
      publicSource || type === "dockerhub" || !prefix
        ? name
        : prefix + "/" + name,
    );
  }
  return (
    <>
      <ErrorMessage error={query.error || accounts.error} />
      {(query.isPending && query.isFetching) ||
      (!publicSource && accounts.isPending) ? (
        <Loading variant="table" />
      ) : (
        <ResourceTable
          rows={filtered}
          sortBy={(repository) => repository.name || ""}
          columns={[
            {
              label: "Repository",
              render: (r) => (
                <Button
                  variant="link"
                  disabled={
                    !publicSource &&
                    type !== "dockerhub" &&
                    (!account?.url || !!accounts.error)
                  }
                  onClick={() => choose(r.name)}
                >
                  <HugeiconsIcon icon={CubeIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                  {r.name}
                  {r.official ? " · official" : ""}
                  {r.private ? " · private" : ""}
                </Button>
              ),
            },
            { label: "Description", render: (r) => r.description || "—" },
            {
              label: "Popularity",
              render: (r) =>
                r.stars != null
                  ? r.stars.toLocaleString() +
                    " stars · " +
                    (r.pulls || 0).toLocaleString() +
                    " pulls"
                  : "—",
            },
          ]}
        />
      )}
    </>
  );
}
export function ImagePicker() {
  const navigate = useNavigate();
  const { me } = useSession();
  const id = useId();
  const [repository, setRepository] = useState("");
  const [registry, setRegistry] = useState("public");
  const [search, setSearch] = useState("");
  const [draft, setDraft] = useState("");
  const [page, setPage] = useState(1);
  const choose = (name: string) =>
    navigate(url("/services/create/wizard/config", { repository: name }));
  if (me?.role === "viewer")
    return (
      <ErrorMessage
        error={new Error("You do not have permission to create services")}
      />
    );
  return (
    <Page title="Create service" description="Choose a container image">
      <Section title="Use a repository">
        <form
          className="flex items-end gap-3"
          onSubmit={(e) => {
            e.preventDefault();
            choose(repository.trim());
          }}
        >
          <div className="flex-1">
            <Field
              label="Repository"
              hint="Image repository without a tag, for example nginx or registry:5000/team/image"
              required
              value={repository}
              onChange={(e) => setRepository(e.target.value)}
            />
          </div>
          <Button type="submit"><HugeiconsIcon icon={Settings01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Configure service</Button>
        </form>
      </Section>
      <Section title="Browse images">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor={id}>
              Registry
            </label>
            <RegistrySelect
              id={id}
              value={registry}
              onValueChange={(value) => {
                setRegistry(value);
                setPage(1);
              }}
            />
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setSearch(draft);
              setPage(1);
            }}
            className="flex items-end gap-2"
          >
            <div className="flex-1">
              <Field
                label="Search repositories"
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
              />
            </div>
            <Button type="submit" variant="outline">
              <HugeiconsIcon icon={Search01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Search
            </Button>
          </form>
        </div>
        <RepositoryResults
          key={registry}
          registry={registry}
          search={search}
          page={page}
          select={choose}
        />
        {registry === "public" && search && (
          <div className="flex items-center justify-end gap-3">
            <Button
              variant="outline"
              disabled={page === 1}
              onClick={() => setPage(page - 1)}
            >
              <HugeiconsIcon icon={ArrowLeft02Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Previous
            </Button>
            <span className="text-sm">Page {page}</span>
            <Button variant="outline" onClick={() => setPage(page + 1)}>
              <HugeiconsIcon icon={ArrowRight02Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Next
            </Button>
          </div>
        )}
      </Section>
    </Page>
  );
}
