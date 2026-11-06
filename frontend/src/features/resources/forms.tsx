import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { useApi, useCommand } from "@/lib/queries";
import { segment } from "@/lib/api";
import type { Network, Volume, Node } from "@/lib/types";
import {
  Page,
  Section,
  Field,
  Choice,
  Toggle,
  Pairs,
  TextField,
  Submit,
  ErrorMessage,
  Loading,
} from "@/components/common";
import { useSession } from "@/features/session/session";
import { canWrite, canAdmin, contentPayload } from "./payload";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, ArrowReloadHorizontalIcon, Loading03Icon, TextIcon } from "@hugeicons/core-free-icons";
import { Button } from "@/components/ui/button";
import { ConfigValueField } from "@/components/config-value-field";
export function Forbidden() {
  return (
    <Page title="Access restricted">
      <p className="text-muted-foreground">
        Your account does not have permission to access this page.
      </p>
    </Page>
  );
}
export function NetworkForm() {
  const { me } = useSession();
  const nav = useNavigate();
  const drivers = useApi<string[]>("/api/plugin/network", canWrite(me?.role));
  const command = useCommand<{ id: string }>((r) =>
    nav("/networks/" + segment(r.id)),
  );
  const [value, set] = useState<
    Pick<
      Network,
      | "networkName"
      | "driver"
      | "internal"
      | "attachable"
      | "ingress"
      | "enableIPv6"
      | "ipam"
      | "options"
    >
  >({
    networkName: "",
    driver: "overlay",
    internal: false,
    attachable: false,
    ingress: false,
    enableIPv6: false,
    ipam: { subnet: "", gateway: "" },
    options: [],
  });
  if (!canWrite(me?.role)) return <Forbidden />;
  return (
    <Page
      title="Create network"
      description="Connect services across your swarm"
    >
      <form
        className="max-w-3xl space-y-5"
        onSubmit={(e) => {
          e.preventDefault();
          if (!command.isPending)
            command.mutate({ path: "/api/networks", body: value });
        }}
      >
        <Section title="Network">
          <Field
            label="Network name"
            value={value.networkName}
            required
            onChange={(e) => set({ ...value, networkName: e.target.value })}
          />
          <Field
            label="Driver"
            value={value.driver}
            list="network-drivers"
            required
            onChange={(e) => set({ ...value, driver: e.target.value })}
          />
          <datalist id="network-drivers">
            {(drivers.data || []).map((d) => (
              <option key={d} value={d} />
            ))}
          </datalist>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="Subnet"
              placeholder="10.0.0.0/24"
              value={value.ipam.subnet}
              onChange={(e) =>
                set({
                  ...value,
                  ipam: { ...value.ipam, subnet: e.target.value },
                })
              }
            />
            <Field
              label="Gateway"
              value={value.ipam.gateway}
              onChange={(e) =>
                set({
                  ...value,
                  ipam: { ...value.ipam, gateway: e.target.value },
                })
              }
            />
          </div>
          {(["internal", "attachable", "ingress", "enableIPv6"] as const).map(
            (key) => (
              <Toggle
                key={key}
                label={
                  key === "enableIPv6"
                    ? "Enable IPv6"
                    : key[0].toUpperCase() + key.slice(1)
                }
                checked={value[key]}
                onChange={(v) => set({ ...value, [key]: v })}
              />
            ),
          )}
        </Section>
        <Section title="Driver options">
          <Pairs
            label="Options"
            value={value.options}
            onChange={(options) => set({ ...value, options })}
          />
        </Section>
        <ErrorMessage error={command.error} />
        <Submit pending={command.isPending} icon={Add01Icon}>Create network</Submit>
      </form>
    </Page>
  );
}
export function VolumeForm() {
  const { me } = useSession();
  const nav = useNavigate();
  const drivers = useApi<string[]>("/api/plugin/volume", canWrite(me?.role));
  const command = useCommand<Volume>((r) =>
    nav("/volumes/" + segment(r.volumeName)),
  );
  const [value, set] = useState({
    volumeName: "",
    driver: "local",
    options: [] as Volume["options"],
  });
  if (!canWrite(me?.role)) return <Forbidden />;
  return (
    <Page
      title="Create volume"
      description="Persistent storage for your services"
    >
      <form
        className="max-w-3xl space-y-5"
        onSubmit={(e) => {
          e.preventDefault();
          if (!command.isPending)
            command.mutate({ path: "/api/volumes", body: value });
        }}
      >
        <Section title="Volume">
          <Field
            label="Volume name"
            hint="Leave empty to generate a name."
            value={value.volumeName}
            onChange={(e) => set({ ...value, volumeName: e.target.value })}
          />
          <Field
            label="Driver"
            value={value.driver}
            list="volume-drivers"
            required
            onChange={(e) => set({ ...value, driver: e.target.value })}
          />
          <datalist id="volume-drivers">
            {(drivers.data || []).map((d) => (
              <option key={d} value={d} />
            ))}
          </datalist>
          <Pairs
            label="Driver options"
            value={value.options}
            onChange={(options) => set({ ...value, options })}
          />
        </Section>
        <ErrorMessage error={command.error} />
        <Submit pending={command.isPending} icon={Add01Icon}>Create volume</Submit>
      </form>
    </Page>
  );
}
export function ContentForm({ kind }: { kind: "secrets" | "configs" }) {
  const { me } = useSession();
  const nav = useNavigate();
  const [name, setName] = useState("");
  const [contents, setContents] = useState("");
  const [file, setFile] = useState<{ name: string; data: string } | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [reading, setReading] = useState(false);
  const command = useCommand<{ id: string }>((r) =>
    nav("/" + kind + "/" + segment(r.id)),
  );
  const singular = kind === "secrets" ? "secret" : "config";
  if (!canWrite(me?.role)) return <Forbidden />;
  return (
    <Page title={"Create " + singular}>
      <form
        className="max-w-3xl space-y-5"
        onSubmit={(e) => {
          e.preventDefault();
          if (command.isPending || reading) return;
          if (kind === "configs") {
            if (!file && !contents.length) {
              setError(new Error("Config value is required."));
              return;
            }
            setError(null);
          }
          command.mutate({
            path: "/api/" + kind,
            body: file
              ? {
                  [kind === "secrets" ? "secretName" : "configName"]: name,
                  data: file.data,
                }
              : contentPayload(kind, name, contents),
          });
        }}
      >
        <Section
          title={singular === "secret" ? "Secret contents" : "Configuration"}
        >
          <Field
            label="Name"
            value={name}
            required
            onChange={(e) => setName(e.target.value)}
          />
          <Field
            label="Upload file (optional)"
            type="file"
            onChange={async (e) => {
              const input = e.target.files?.[0];
              setError(null);
              if (!input) {
                setFile(null);
                return;
              }
              setReading(true);
              try {
                const data = await input.arrayBuffer();
                let binary = "";
                for (const byte of new Uint8Array(data))
                  binary += String.fromCharCode(byte);
                setFile({ name: input.name, data: btoa(binary) });
              } catch (error) {
                setError(error);
                setFile(null);
              } finally {
                setReading(false);
              }
            }}
          />
          {file ? (
            <div className="flex items-center justify-between rounded-lg border p-3 text-sm">
              <span>{file.name} selected</span>
              <button
                type="button"
                className="inline-flex items-center gap-2 underline"
                onClick={() => setFile(null)}
              >
                <HugeiconsIcon icon={TextIcon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
                Use text instead
              </button>
            </div>
          ) : kind === "configs" ? (
            <ConfigValueField
              value={contents}
              onChange={setContents}
              required
              disabled={command.isPending || reading}
            />
          ) : (
            <TextField
              label="Contents"
              value={contents}
              onChange={setContents}
              rows={14}
              required
            />
          )}
          {kind === "secrets" && (
            <p className="text-xs text-muted-foreground">
              Secret contents cannot be retrieved from Docker after creation.
            </p>
          )}
        </Section>
        <ErrorMessage error={error || command.error} />
        <Submit pending={command.isPending || reading} icon={Add01Icon}>
          Create {singular}
        </Submit>
      </form>
    </Page>
  );
}
export function NodeForm() {
  const { id = "" } = useParams();
  const { me } = useSession();
  const query = useApi<Node>("/api/nodes/" + segment(id), canAdmin(me?.role));
  if (!canAdmin(me?.role)) return <Forbidden />;
  return (
    <>
      <ErrorMessage error={query.error} />
      {query.isPending ? (
        <Loading />
      ) : (
        query.data && (
          <NodeEditor
            key={id}
            node={query.data}
            reload={async () => {
              const result = await query.refetch({ throwOnError: true });
              return result.data!;
            }}
          />
        )
      )}
    </>
  );
}
function nodeValues(node: Node) {
  return {
    nodeName: node.nodeName,
    version: node.version,
    role: node.role,
    availability: node.availability,
    labels: node.labels || [],
  };
}
function NodeEditor({
  node,
  reload,
}: {
  node: Node;
  reload: () => Promise<Node>;
}) {
  const nav = useNavigate();
  const [value, set] = useState(() => nodeValues(node));
  const [reloading, setReloading] = useState(false);
  const command = useCommand(() => nav("/nodes/" + segment(node.id)));
  async function reloadValues() {
    if (
      command.isPending ||
      reloading ||
      !window.confirm(
        "Discard unsaved changes and reload the latest server values?",
      )
    )
      return;
    setReloading(true);
    try {
      set(nodeValues(await reload()));
      command.reset();
    } catch {
      /* NodeForm displays the query error and retains this draft. */
    } finally {
      setReloading(false);
    }
  }
  return (
    <Page
      title={"Edit " + value.nodeName}
      description="Node configuration"
      actions={
        <Button
          type="button"
          variant="outline"
          disabled={command.isPending || reloading}
          onClick={() => void reloadValues()}
        >
          <HugeiconsIcon icon={reloading ? Loading03Icon : ArrowReloadHorizontalIcon} strokeWidth={2} aria-hidden="true" className={reloading ? "size-4 shrink-0 animate-spin motion-reduce:animate-none" : "size-4 shrink-0"} />
          {reloading ? "Reloading…" : "Reload server values"}
        </Button>
      }
    >
      <form
        className="max-w-3xl space-y-5"
        onSubmit={(e) => {
          e.preventDefault();
          if (!command.isPending && !reloading)
            command.mutate({
              path: "/api/nodes/" + segment(node.id),
              body: value,
            });
        }}
      >
        <fieldset disabled={reloading}>
          <Section title="Node">
            <Field label="Node name" value={value.nodeName} disabled />
            <Choice
              label="Role"
              disabled={reloading}
              options={["worker", "manager"]}
              value={value.role}
              onChange={(role) => set({ ...value, role })}
            />
            <Choice
              label="Availability"
              disabled={reloading}
              options={["active", "pause", "drain"]}
              value={value.availability}
              onChange={(availability) => set({ ...value, availability })}
            />
            <Pairs
              label="Labels"
              value={value.labels}
              onChange={(labels) => set({ ...value, labels })}
            />
          </Section>
        </fieldset>
        <ErrorMessage error={command.error} />
        {command.error && (
          <p className="text-sm text-muted-foreground">
            If the node changed on the server, reload server values before
            editing again. Reloading discards your unsaved changes.
          </p>
        )}
        <Submit pending={command.isPending || reloading} />
      </form>
    </Page>
  );
}
