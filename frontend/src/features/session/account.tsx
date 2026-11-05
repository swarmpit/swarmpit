import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { useApi, useCommand } from "@/lib/queries";
import {
  ErrorMessage,
  Field,
  Loading,
  Page,
  Properties,
  Section,
  Submit,
  TextField,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { Me } from "@/lib/types";
import { useSession } from "./session";
interface TokenMetadata {
  mask?: string;
  expiresAt?: string;
}
interface NewToken extends TokenMetadata {
  token: string;
}
function expiry(value: TokenMetadata) {
  if (!value.expiresAt) return "This token does not expire.";
  const date = new Date(value.expiresAt);
  return (
    "Expires: " +
    (Number.isNaN(date.valueOf()) ? value.expiresAt : date.toLocaleString())
  );
}
export function Account() {
  const { me } = useSession();
  return (
    <Page title="Account settings">
      <Section title="Identity">
        <Properties
          values={{
            Username: me?.username,
            Email: me?.email || "—",
            Role: me?.role,
          }}
        />
      </Section>
      <Password />
      <ApiAccess />
    </Page>
  );
}
function Password() {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [validation, setValidation] = useState("");
  const [notice, setNotice] = useState("");
  const command = useCommand(() => {
    setCurrent("");
    setNext("");
    setConfirmation("");
    setNotice("Password changed.");
  });
  return (
    <Section title="Password change">
      <form
        className="max-w-xl space-y-4"
        onSubmit={(e) => {
          e.preventDefault();
          if (command.isPending) return;
          setNotice("");
          if (next !== confirmation) {
            setValidation("New password and confirmation must match.");
            return;
          }
          setValidation("");
          command.mutate({
            path: "/password",
            body: {
              password: current,
              "new-password": next,
              "confirm-password": confirmation,
            },
          });
        }}
      >
        <Field
          label="Current password"
          type="password"
          required
          autoComplete="current-password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
        />
        <Field
          label="New password"
          type="password"
          required
          autoComplete="new-password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
        />
        <Field
          label="Confirm password"
          type="password"
          required
          autoComplete="new-password"
          value={confirmation}
          onChange={(e) => setConfirmation(e.target.value)}
        />
        <ErrorMessage error={validation || command.error} />
        {notice && <p role="status">{notice}</p>}
        <Submit pending={command.isPending}>Change password</Submit>
      </form>
    </Section>
  );
}
function ApiAccess() {
  const client = useQueryClient();
  const query = useApi<Me & { "api-token"?: TokenMetadata }>("/api/me");
  const [generated, setGenerated] = useState<NewToken | null | undefined>();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>();
  const [dialog, setDialog] = useState<"replace" | "revoke" | null>(null);
  const token = generated === undefined ? query.data?.["api-token"] : generated;
  async function run(method: "POST" | "DELETE") {
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      const result = await api<NewToken | undefined>("/api-token", { method });
      if (method === "POST" && !result?.token)
        throw new Error("The server did not return an API token.");
      setGenerated(method === "POST" ? result : null);
      setDialog(null);
      await client.invalidateQueries({ queryKey: ["api", "/api/me"] });
    } catch (e) {
      setError(e);
    } finally {
      setPending(false);
    }
  }
  return (
    <Section title="API access">
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading />
      ) : (
        <div className="max-w-2xl space-y-4">
          {token ? (
            <>
              <p className="text-sm text-muted-foreground">
                {generated?.token
                  ? "Copy this token and store it safely. The full value is displayed only once."
                  : "An API token already exists. Generating another immediately revokes the former token."}
              </p>
              <TextField
                label="Authorization token"
                readOnly
                value={generated?.token || "Bearer ..." + token.mask}
                rows={3}
              />
              <p className="text-sm">{expiry(token)}</p>
              {generated?.token && (
                <Button
                  variant="outline"
                  onClick={() =>
                    setGenerated({
                      mask: generated.token.slice(-5),
                      expiresAt: generated.expiresAt,
                      token: "",
                    })
                  }
                >
                  Hide token
                </Button>
              )}
            </>
          ) : (
            <p className="text-sm text-muted-foreground">
              You do not have an API token.
            </p>
          )}
          <ErrorMessage error={!dialog ? error : undefined} />
          <div className="flex flex-wrap gap-3">
            <Button
              disabled={pending}
              onClick={() => (token ? setDialog("replace") : void run("POST"))}
            >
              {pending
                ? "Working…"
                : token
                  ? "Regenerate API token"
                  : "Generate API token"}
            </Button>
            {token && (
              <Button
                disabled={pending}
                variant="destructive"
                onClick={() => setDialog("revoke")}
              >
                Revoke API token
              </Button>
            )}
          </div>
          <Dialog
            open={dialog !== null}
            onOpenChange={(open) => {
              if (!open && !pending) setDialog(null);
            }}
          >
            <DialogContent>
              <DialogHeader>
                <DialogTitle>
                  {dialog === "replace"
                    ? "Replace API token?"
                    : "Revoke API token?"}
                </DialogTitle>
                <DialogDescription>
                  {dialog === "replace"
                    ? "The existing token stops working immediately. Copy the new token after generation."
                    : "Applications using this API token will lose access immediately."}
                </DialogDescription>
              </DialogHeader>
              <ErrorMessage error={error} />
              <DialogFooter>
                <Button
                  variant="outline"
                  disabled={pending}
                  onClick={() => setDialog(null)}
                >
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  disabled={pending}
                  onClick={() =>
                    void run(dialog === "replace" ? "POST" : "DELETE")
                  }
                >
                  {pending
                    ? "Working…"
                    : dialog === "replace"
                      ? "Replace token"
                      : "Revoke token"}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      )}
    </Section>
  );
}
