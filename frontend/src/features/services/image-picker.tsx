import { useId, useState } from "react";
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
import { useSession } from "@/features/session/session";

interface Repository {
  name: string;
  description?: string;
  stars?: number;
  pulls?: number;
  official?: boolean;
  private?: boolean;
}
function RegistryOptions({ type }: { type: string }) {
  const query = useApi<Registry[]>("/api/registry/" + type);
  return (
    <>
      {query.error && (
        <option disabled>
          {type} unavailable: {query.error.message}
        </option>
      )}
      {query.data?.map((r) => (
        <option key={r._id} value={type + "/" + r._id}>
          {type} · {r.name || r.username || String(r.user || r.spName || r._id)}
        </option>
      ))}
    </>
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
      {query.isFetching || (!publicSource && accounts.isPending) ? (
        <Loading />
      ) : (
        <ResourceTable
          rows={filtered}
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
          <Button type="submit">Configure service</Button>
        </form>
      </Section>
      <Section title="Browse images">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor={id}>
              Registry
            </label>
            <select
              id={id}
              className="h-9 w-full rounded-md border bg-background px-3 text-sm"
              value={registry}
              onChange={(e) => {
                setRegistry(e.target.value);
                setPage(1);
              }}
            >
              <option value="public">Public Docker Hub</option>
              {registryTypes.map((type) => (
                <RegistryOptions key={type} type={type} />
              ))}
            </select>
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
              Previous
            </Button>
            <span className="text-sm">Page {page}</span>
            <Button variant="outline" onClick={() => setPage(page + 1)}>
              Next
            </Button>
          </div>
        )}
      </Section>
    </Page>
  );
}
