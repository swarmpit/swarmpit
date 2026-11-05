import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router";
import { api, segment, url } from "@/lib/api";
import { useApi, useCommand } from "@/lib/queries";
import type { Compose, History } from "@/lib/types";
import {
  Action,
  ErrorMessage,
  Field,
  Loading,
  Page,
  Section,
  Submit,
  TextField,
  Toggle,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useSession } from "@/features/session/session";
import { historyLabel, missingStackfile } from "./compose";
import { ComposeComparison } from "./compose-comparison";

export function StackForm() {
  const location = useLocation();
  return <StackFormLoader key={location.pathname + location.search} />;
}

function StackFormLoader() {
  const { name = "" } = useParams();
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const from = params.get("from") || "";
  const create = !name;
  const activate = location.pathname.endsWith("/activate");
  const requested = location.pathname.endsWith("/previous")
    ? "previous"
    : params.has("history")
      ? "history-" + params.get("history")
      : "stored";
  const path = "/api/stacks/" + segment(name);
  const file = useApi<Compose>(path + "/file", !create);
  const history = useApi<History[]>(path + "/history", !create);
  const absent = !file.data && missingStackfile(file.error);
  const fallback = useApi<Compose>(
    path + "/compose",
    !create && absent && !activate && requested === "stored",
  );
  const imported = useApi<Compose>(
    "/api/services/" + segment(from) + "/compose",
    create && !!from,
  );
  if (create) {
    if (from && imported.isPending) return <Loading />;
    if (from && imported.error) return <ErrorMessage error={imported.error} />;
    return (
      <StackEditor
        initial={{
          name: "",
          spec: { compose: imported.data?.spec.compose || "" },
        }}
        source="new"
        history={[]}
        create
      />
    );
  }
  if (
    file.isPending ||
    (absent && fallback.isFetching && !fallback.data) ||
    (requested.startsWith("history-") && history.isPending)
  )
    return <Loading />;
  const error = absent
    ? activate || requested !== "stored"
      ? new Error("This stack has no stored compose version.")
      : fallback.error
    : file.error;
  if (error && !file.data && !fallback.data)
    return <ErrorMessage error={error} />;
  const stored = file.data;
  const initial =
    requested === "previous"
      ? stored?.previousSpec?.compose
      : requested.startsWith("history-")
        ? history.data?.[Number(requested.slice(8))]?.spec.compose
        : (stored?.spec.compose ?? fallback.data?.spec.compose);
  if (initial === undefined)
    return (
      <Page title={name}>
        <ErrorMessage
          error={
            history.error ||
            new Error("The requested compose version is not available.")
          }
        />
        <Button asChild variant="outline">
          <Link to={"/stacks/" + segment(name) + "/compose"}>
            Open stored compose
          </Link>
        </Button>
      </Page>
    );
  return (
    <StackEditor
      initial={{
        name,
        spec: { compose: initial },
        skipImageResolve: stored?.skipImageResolve,
      }}
      source={stored ? requested : "engine"}
      stored={stored}
      history={history.data || []}
      historyError={
        history.error ||
        (file.data ? file.error : fallback.data ? fallback.error : null)
      }
      activate={activate}
    />
  );
}

interface EditorProps {
  initial: Compose;
  source: string;
  stored?: Compose;
  history: History[];
  historyError?: unknown;
  create?: boolean;
  activate?: boolean;
}
function StackEditor({
  initial,
  source: initialSource,
  stored,
  history,
  historyError,
  create = false,
  activate = false,
}: EditorProps) {
  // Initialize once: query snapshots must never overwrite a draft.
  const [name, setName] = useState(initial.name);
  const [compose, setCompose] = useState(initial.spec.compose);
  const [loadedCompose, setLoadedCompose] = useState(initial.spec.compose);
  const [source, setSource] = useState(initialSource);
  const [skipImageResolve, setSkipImageResolve] = useState(
    !!initial.skipImageResolve,
  );
  const [comment, setComment] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [loadingSource, setLoadingSource] = useState(false);
  const [pendingSource, setPendingSource] = useState<string | null>(null);
  const [showComparison, setShowComparison] = useState(false);
  const request = useRef<AbortController | null>(null);
  const intent = useRef("deploy");
  const navigate = useNavigate();
  const { me } = useSession();
  const editable = !!me && me.role !== "viewer";
  const sourceId = useId();
  const uploadId = useId();
  const path = "/api/stacks/" + segment(name);
  const baseline = useApi<Compose>(
    path + "/compose",
    !create && showComparison,
  );
  const command = useCommand(() =>
    navigate(
      intent.current === "save" ? "/stacks" : "/stacks/" + segment(name),
    ),
  );
  const busy = command.isPending || loadingSource;
  useEffect(() => () => request.current?.abort(), []);

  async function loadSource(next: string) {
    setPendingSource(null);
    setError(null);
    let nextCompose: string | undefined;
    if (next === "engine") {
      const controller = new AbortController();
      request.current?.abort();
      request.current = controller;
      setLoadingSource(true);
      try {
        nextCompose = (
          await api<Compose>(path + "/compose", { signal: controller.signal })
        ).spec.compose;
      } catch (e) {
        if (!controller.signal.aborted) setError(e);
        return;
      } finally {
        if (!controller.signal.aborted) setLoadingSource(false);
      }
    } else if (next === "stored") nextCompose = stored?.spec.compose;
    else if (next === "previous") nextCompose = stored?.previousSpec?.compose;
    else if (next.startsWith("history-"))
      nextCompose = history[Number(next.slice(8))]?.spec.compose;
    if (nextCompose !== undefined) {
      setSource(next);
      setCompose(nextCompose);
      setLoadedCompose(nextCompose);
    }
  }
  function chooseSource(next: string) {
    if (next === source) return;
    if (compose !== loadedCompose) setPendingSource(next);
    else void loadSource(next);
  }
  async function upload(file?: File) {
    if (!file) return;
    setError(null);
    try {
      const text = await file.text();
      if (compose !== loadedCompose) {
        setUploaded(text);
        setPendingSource("upload");
      } else {
        setCompose(text);
        setSource("upload");
        setLoadedCompose(text);
      }
    } catch (e) {
      setError(e);
    }
  }
  const [uploaded, setUploaded] = useState("");
  function confirmSource() {
    if (pendingSource === "upload") {
      setCompose(uploaded);
      setLoadedCompose(uploaded);
      setSource("upload");
      setPendingSource(null);
    } else if (pendingSource) void loadSource(pendingSource);
  }
  function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (busy || !editable) return;
    setError(null);
    if (!name.trim() || !compose.trim()) {
      setError(new Error("Stack name and compose YAML are required."));
      return;
    }
    const saving =
      (e.nativeEvent as SubmitEvent).submitter?.getAttribute("value") ===
      "save";
    intent.current = saving ? "save" : "deploy";
    command.mutate({
      path: saving
        ? path + "/file"
        : url(create ? "/api/stacks" : path, { comment: comment || undefined }),
      body: { name, spec: { compose }, skipImageResolve },
    });
  }
  const title = create
    ? "Create stack"
    : activate
      ? "Activate stack"
      : "Edit stack";
  return (
    <Page
      title={title}
      description="Compose YAML is sent unchanged, including comments and interpolation."
      actions={
        <Button variant="outline" asChild>
          <Link to={create ? "/stacks" : "/stacks/" + segment(name)}>
            Cancel
          </Link>
        </Button>
      }
    >
      <form onSubmit={submit}>
        <fieldset disabled={busy} className="space-y-6">
          <Section title="Compose configuration">
            <Field
              label="Stack name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              readOnly={!create || !editable}
            />
            {!create && (
              <div className="space-y-2">
                <Label htmlFor={sourceId}>Compose source</Label>
                <select
                  id={sourceId}
                  value={source}
                  disabled={busy}
                  onChange={(e) => chooseSource(e.target.value)}
                  className="h-9 w-full rounded-md border bg-background px-3 text-sm"
                >
                  <option value="stored" disabled={!stored?.spec}>
                    Stored / last deployed
                  </option>
                  <option value="engine">
                    Current engine state (reconstructed)
                  </option>
                  <option value="previous" disabled={!stored?.previousSpec}>
                    Previously deployed
                  </option>
                  {source === "upload" && (
                    <option value="upload">Uploaded file</option>
                  )}
                  {history
                    .map((entry, index) => ({ entry, index }))
                    .reverse()
                    .map(({ entry, index }) => (
                      <option key={index} value={"history-" + index}>
                        {historyLabel(entry)}
                      </option>
                    ))}
                </select>
                <ErrorMessage error={historyError} />
              </div>
            )}
            {editable && (
              <div className="space-y-2">
                <Label htmlFor={uploadId}>Upload compose file</Label>
                <input
                  id={uploadId}
                  type="file"
                  accept=".yml,.yaml,.txt,text/yaml,text/plain,application/yaml"
                  disabled={busy}
                  className="block w-full rounded-md border p-2 text-sm"
                  onChange={(e) => {
                    void upload(e.target.files?.[0]);
                    e.target.value = "";
                  }}
                />
              </div>
            )}
            {loadingSource && (
              <p role="status" className="text-sm text-muted-foreground">
                Loading compose source…
              </p>
            )}
            <TextField
              label="Compose YAML"
              value={compose}
              onChange={setCompose}
              rows={24}
              required
              readOnly={!editable || busy}
              hint={
                source === "engine"
                  ? "Reconstructed from engine state; original YAML comments and syntax are unavailable."
                  : "Stored compose preserves the original YAML, including comments."
              }
            />
            {editable && (
              <Toggle
                label="Skip image resolution"
                checked={skipImageResolve}
                onChange={setSkipImageResolve}
              />
            )}
            {editable && (
              <Field
                label="Change comment"
                value={comment}
                maxLength={120}
                onChange={(e) => setComment(e.target.value)}
                hint="Optional deployment history note (up to 120 characters)."
              />
            )}
          </Section>
          {!create && (
            <Section title="Compare with engine state">
              <Toggle
                label="Show current engine compose"
                checked={showComparison}
                onChange={setShowComparison}
              />
              {showComparison && (
                <>
                  <ErrorMessage error={baseline.error} />
                  {baseline.isPending ? (
                    <Loading />
                  ) : (
                    baseline.data && (
                      <>
                        <TextField
                          label="Current engine compose"
                          value={baseline.data.spec.compose}
                          readOnly
                          rows={18}
                        />
                        <ComposeComparison
                          baseline={baseline.data.spec.compose}
                          current={compose}
                          onRestore={editable && !busy ? setCompose : undefined}
                        />
                        {editable && (
                          <Button
                            type="button"
                            variant="outline"
                            disabled={busy}
                            onClick={() => chooseSource("engine")}
                          >
                            Use current engine compose
                          </Button>
                        )}
                      </>
                    )
                  )}
                </>
              )}
            </Section>
          )}
          <ErrorMessage error={error || command.error} />
          {editable && (
            <div className="flex flex-wrap items-center gap-3">
              <Submit pending={busy}>
                {activate ? "Activate stack" : "Deploy"}
              </Submit>
              {create && (
                <Button
                  type="submit"
                  name="intent"
                  value="save"
                  variant="outline"
                  disabled={busy}
                >
                  Save stackfile
                </Button>
              )}
            </div>
          )}
        </fieldset>
      </form>
      {editable && activate && stored && (
        <Action
          label="Delete stackfile"
          path={path + "/file"}
          method="DELETE"
          destructive
          disabled={busy}
          confirm="This removes the saved compose and its history. The inactive stack will no longer be available to activate."
          onSuccess={() => navigate("/stacks")}
        />
      )}
      <Dialog
        open={pendingSource !== null}
        onOpenChange={(open) => {
          if (!open) setPendingSource(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Replace unsaved compose edits?</DialogTitle>
            <DialogDescription>
              Loading another source replaces your current compose draft.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPendingSource(null)}>
              Keep editing
            </Button>
            <Button onClick={confirmSource}>Replace draft</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Page>
  );
}
