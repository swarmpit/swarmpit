import { useEffect, useId, useRef, useState, type FormEvent, type ReactNode } from "react";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  Cancel01Icon, ComputerIcon, Delete02Icon, Exchange01Icon, FloppyDiskIcon,
  PencilEdit01Icon, PlayCircleIcon, Rocket01Icon, SourceCodeIcon,
} from "@hugeicons/core-free-icons";
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
  Toggle,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
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
import { CodeField } from "@/components/code-field";

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
  const { me } = useSession();
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
  const title = create ? "Create stack" : activate ? "Activate stack" : "Edit stack";
  const page = (children: ReactNode) => (
    <Page
      title={title}
      description="Compose YAML is sent unchanged, including comments and interpolation."
      actions={<Button variant="outline" asChild>
        <Link to={create ? "/stacks" : "/stacks/" + segment(name)}><HugeiconsIcon icon={Cancel01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Cancel</Link>
      </Button>}
    >{children}</Page>
  );
  const loading = () => page(<StackEditorSkeleton create={create} editable={!!me && me.role !== "viewer"} />);
  if (create) {
    if (from && imported.isPending) return loading();
    if (from && imported.error) return page(<ErrorMessage error={imported.error} />);
    return page(
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
    return loading();
  const error = absent
    ? activate || requested !== "stored"
      ? new Error("This stack has no stored compose version.")
      : fallback.error
    : file.error;
  if (error && !file.data && !fallback.data)
    return page(<ErrorMessage error={error} />);
  const stored = file.data;
  const initial =
    requested === "previous"
      ? stored?.previousSpec?.compose
      : requested.startsWith("history-")
        ? history.data?.[Number(requested.slice(8))]?.spec.compose
        : (stored?.spec.compose ?? fallback.data?.spec.compose);
  if (initial === undefined)
    return page(
      <>
        <ErrorMessage
          error={
            history.error ||
            new Error("The requested compose version is not available.")
          }
        />
        <Button asChild variant="outline">
          <Link to={"/stacks/" + segment(name) + "/compose"}>
            <HugeiconsIcon icon={SourceCodeIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
            Open stored compose
          </Link>
        </Button>
      </>
    );
  return page(
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

function StackEditorSkeleton({ create, editable }: { create: boolean; editable: boolean }) {
  return (
    <div role="status" aria-label="Loading stack compose" aria-busy="true" className="space-y-6">
      <span className="sr-only">Loading stack compose…</span>
      <Section title="Compose configuration">
        <div aria-hidden="true" className="space-y-4">
          {Array.from({ length: 1 + Number(!create) + Number(editable) }, (_, index) => (
            <div key={index} className="space-y-2">
              <Skeleton className="h-3.5 w-28" />
              <Skeleton className="h-9 w-full" />
            </div>
          ))}
          <div className="space-y-2">
            <Skeleton className="h-3.5 w-28" />
            <Skeleton className="h-[37.125rem] w-full" />
            <Skeleton className="h-4 w-96 max-w-full" />
          </div>
          {editable && <>
            <Skeleton className="h-5 w-40" />
            <div className="space-y-2">
              <Skeleton className="h-3.5 w-28" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-4 w-80 max-w-full" />
            </div>
          </>}
        </div>
      </Section>
      {!create && <Section title="Compare with engine state"><Skeleton className="h-5 w-52" /></Section>}
      {editable && <Skeleton className="h-9 w-24" />}
    </div>
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
  return (
    <>
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
                <Select
                  value={source}
                  disabled={busy}
                  onValueChange={chooseSource}
                >
                  <SelectTrigger id={sourceId} className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="stored" disabled={!stored?.spec}>
                      Stored / last deployed
                    </SelectItem>
                    <SelectItem value="engine">
                      Current engine state (reconstructed)
                    </SelectItem>
                    <SelectItem value="previous" disabled={!stored?.previousSpec}>
                      Previously deployed
                    </SelectItem>
                    {source === "upload" && (
                      <SelectItem value="upload">Uploaded file</SelectItem>
                    )}
                    {history
                      .map((entry, index) => ({ entry, index }))
                      .reverse()
                      .map(({ entry, index }) => (
                        <SelectItem key={index} value={"history-" + index}>
                          {historyLabel(entry)}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
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
            <CodeField
              label="Compose YAML"
              showSyntaxSelector={false}
              disabled={busy}
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
                        <CodeField
                          label="Current engine compose"
                          showSyntaxSelector={false}
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
                            <HugeiconsIcon icon={ComputerIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
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
              <Submit pending={busy} icon={activate ? PlayCircleIcon : Rocket01Icon}>
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
                  <HugeiconsIcon icon={FloppyDiskIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                  Save stackfile
                </Button>
              )}
            </div>
          )}
        </fieldset>
      </form>
      {editable && activate && stored && (
        <Action
          icon={Delete02Icon}
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
              <HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
              Keep editing
            </Button>
            <Button onClick={confirmSource}><HugeiconsIcon icon={Exchange01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Replace draft</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
