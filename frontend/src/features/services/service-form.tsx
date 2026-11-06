import { useId, useState } from "react";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { useApi, useCommand } from "@/lib/queries";
import { api, segment, url } from "@/lib/api";
import type {
  Service,
  Deploy,
  Network,
  Secret,
  Config,
  Volume,
} from "@/lib/types";
import {
  Page,
  Section,
  Field,
  Choice,
  Toggle,
  Pairs,
  Repeat,
  ErrorMessage,
  Loading,
  Submit,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { useSession } from "@/features/session/session";
import { defaultService, editableService, servicePayload } from "./payload";
import { HugeiconsIcon } from "@hugeicons/react";
import { Add01Icon, Cancel01Icon, Loading03Icon, PlugSocketIcon, Rocket01Icon } from "@hugeicons/core-free-icons";

function Arguments({
  label,
  value,
  onChange,
}: {
  label: string;
  value?: string[];
  onChange: (v: string[]) => void;
}) {
  return (
    <Repeat
      title={label}
      value={value || []}
      onChange={onChange}
      add={() => ""}
    >
      {(arg, set) => (
        <Field
          label="Argument"
          value={arg}
          onChange={(e) => set(e.target.value)}
        />
      )}
    </Repeat>
  );
}
function Suggest({
  label,
  value,
  onChange,
  options,
  required = false,
  disabled = false,
  hint,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: string[];
  required?: boolean;
  disabled?: boolean;
  hint?: string;
}) {
  const id = useId();
  return (
    <>
      <Field
        label={label}
        value={value}
        list={id}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        disabled={disabled}
        hint={hint}
      />
      <datalist id={id}>
        {options.map((o, i) => (
          <option key={i} value={o} />
        ))}
      </datalist>
    </>
  );
}
function DeployFields({
  label,
  value,
  onChange,
}: {
  label: string;
  value: Deploy;
  onChange: (v: Deploy) => void;
}) {
  return (
    <div className="space-y-4">
      <h3 className="font-medium">{label}</h3>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label={label + " parallelism"}
          type="number"
          min={0}
          step={1}
          value={value.parallelism}
          onChange={(e) =>
            onChange({ ...value, parallelism: Number(e.target.value) })
          }
        />
        <Field
          label={label + " delay (seconds)"}
          type="number"
          min={0}
          step="any"
          value={value.delay}
          onChange={(e) =>
            onChange({ ...value, delay: Number(e.target.value) })
          }
        />
        <Choice
          label={label + " order"}
          value={value.order}
          options={["stop-first", "start-first"]}
          onChange={(order) => onChange({ ...value, order })}
        />
        <Choice
          label={label + " failure action"}
          value={value.failureAction}
          options={
            label === "Update"
              ? ["pause", "continue", "rollback"]
              : ["pause", "continue"]
          }
          onChange={(failureAction) => onChange({ ...value, failureAction })}
        />
      </div>
    </div>
  );
}
export function ServiceForm() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const { me } = useSession();
  const query = useApi<Service>("/api/services/" + segment(id || ""), !!id);
  if (me?.role === "viewer")
    return (
      <ErrorMessage
        error={new Error("You do not have permission to configure services")}
      />
    );
  if (id && query.isPending) return <Loading />;
  if (id && !query.data) return <ErrorMessage error={query.error} />;
  if (query.data?.immutable)
    return (
      <ErrorMessage
        error={
          new Error(
            "This service is immutable. Change its owning stack configuration instead.",
          )
        }
      />
    );
  return (
    <ServiceEditor
      key={
        id || (params.get("repository") || "") + ":" + (params.get("tag") || "")
      }
      initial={
        (id ? query.data : undefined) || {
          ...defaultService(params.get("repository") || ""),
          repository: {
            name: params.get("repository") || "",
            tag: params.get("tag") || "",
          },
        }
      }
      editing={!!id}
    />
  );
}
export function ServiceEditor({
  initial,
  editing,
}: {
  initial: Service;
  editing: boolean;
}) {
  const navigate = useNavigate();
  const { version } = useSession();
  // Keep the version and edited fields together; query refreshes must not replace unsaved work.
  const [s, setService] = useState<Service>(() => editableService(initial));
  const [error, setError] = useState<unknown>();
  const [comment, setComment] = useState("");
  const [portsPending, setPortsPending] = useState(false);
  const command = useCommand<{ id?: string } | undefined>((data) =>
    navigate(
      "/services/" + segment(editing ? initial.id : data?.id || s.serviceName),
    ),
  );
  const networks = useApi<Network[]>("/api/networks");
  const secrets = useApi<Secret[]>("/api/secrets");
  const configs = useApi<Config[]>(
    "/api/configs",
    (version?.docker.api || 0) >= 1.3,
  );
  const volumes = useApi<Volume[]>("/api/volumes");
  const drivers = useApi<string[]>("/api/plugin/log");
  const placement = useApi<string[]>("/api/placement");
  const tags = useApi<string[]>(
    url("/api/repository/tags", { repository: s.repository.name }),
    !!s.repository.name,
  );
  const set = <K extends keyof Service>(key: K, value: Service[K]) =>
    setService((old) => ({ ...old, [key]: value }));
  const deploy = <K extends keyof Service["deployment"]>(
    key: K,
    value: Service["deployment"][K],
  ) => set("deployment", { ...s.deployment, [key]: value });
  async function suggestedPorts() {
    setPortsPending(true);
    setError(null);
    try {
      const ports = await api<Service["ports"]>(
        url("/api/repository/ports", {
          repository: s.repository.name,
          repositoryTag: s.repository.tag || "latest",
        }),
      );
      setService((old) => ({
        ...old,
        ports: [...old.ports, ...ports.map((p) => ({ ...p, mode: "ingress" }))],
      }));
    } catch (e) {
      setError(e);
    } finally {
      setPortsPending(false);
    }
  }
  const policy = s.deployment.restartPolicy;
  return (
    <Page
      title={editing ? "Edit " + s.serviceName : "Configure service"}
      description={
        editing
          ? "Deploy a new service version"
          : "Set container and deployment options"
      }
      actions={
        <Button variant="outline" asChild>
          <Link
            to={
              editing
                ? "/services/" + segment(initial.id)
                : "/services/create/wizard/image"
            }
          >
            <HugeiconsIcon icon={Cancel01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />
            Cancel
          </Link>
        </Button>
      }
    >
      <form
        className="space-y-6"
        onSubmit={(e) => {
          e.preventDefault();
          if (command.isPending) return;
          setError(null);
          try {
            command.mutate({
              path: url(
                editing
                  ? "/api/services/" + segment(initial.id)
                  : "/api/services",
                { comment: editing && comment ? comment : undefined },
              ),
              body: servicePayload(s, editing),
            });
          } catch (e) {
            setError(e);
          }
        }}
      >
        <Section title="General">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field
              label="Service name"
              value={s.serviceName}
              disabled={editing}
              hint="Leave empty to generate a name"
              onChange={(e) => set("serviceName", e.target.value)}
            />
            <Field
              label="Repository"
              required
              value={s.repository.name}
              disabled={editing}
              onChange={(e) =>
                set("repository", { ...s.repository, name: e.target.value })
              }
            />
            <Suggest
              label="Image tag"
              value={s.repository.tag}
              options={tags.data || []}
              onChange={(tag) => set("repository", { ...s.repository, tag })}
              hint="Leave empty for latest; custom tags are supported"
            />
            <Choice
              label="Mode"
              value={s.mode}
              options={["replicated", "global"]}
              disabled={editing}
              onChange={(v) => set("mode", v)}
            />
            {s.mode === "replicated" && (
              <Field
                label="Replicas"
                type="number"
                min={0}
                step={1}
                value={s.replicas ?? 0}
                onChange={(e) => set("replicas", Number(e.target.value))}
              />
            )}
            <Field
              label="Hostname"
              value={s.hostname || ""}
              onChange={(e) => set("hostname", e.target.value)}
            />
            <Choice
              label="Isolation"
              value={s.isolation || ""}
              options={["", "default", "process", "hyperv"]}
              onChange={(v) => set("isolation", v)}
            />
            {editing && (
              <>
                <Field
                  label="Container user"
                  value={s.user || ""}
                  readOnly
                  hint="Set by the originating stack"
                />
                <Field
                  label="Working directory"
                  value={s.dir || ""}
                  readOnly
                  hint="Set by the originating stack"
                />
                <Toggle
                  label="Allocate TTY"
                  checked={s.tty || false}
                  onChange={(v) => set("tty", v)}
                />
              </>
            )}
          </div>
          <ErrorMessage error={tags.error} />
          <p className="text-xs text-muted-foreground">
            Each argument below is passed unchanged, including spaces. An empty
            list uses the image default.
          </p>
          <Arguments
            label="Entrypoint"
            value={s.entrypoint}
            onChange={(v) => set("entrypoint", v)}
          />
          <Arguments
            label="Command arguments"
            value={s.command}
            onChange={(v) => set("command", v)}
          />
        </Section>
        <Section title="Network">
          <Repeat
            title="Published ports"
            value={s.ports}
            onChange={(v) => set("ports", v)}
            add={() => ({
              containerPort: 80,
              hostPort: 80,
              protocol: "tcp",
              mode: "ingress",
            })}
          >
            {(p, put) => (
              <>
                <Field
                  label="Container port"
                  required
                  type="number"
                  min={1}
                  max={65535}
                  value={p.containerPort}
                  onChange={(e) =>
                    put({ ...p, containerPort: Number(e.target.value) })
                  }
                />
                <Field
                  label="Published port"
                  required
                  type="number"
                  min={0}
                  max={65535}
                  value={p.hostPort}
                  onChange={(e) =>
                    put({ ...p, hostPort: Number(e.target.value) })
                  }
                />
                <Choice
                  label="Protocol"
                  value={p.protocol}
                  options={["tcp", "udp", "sctp"]}
                  onChange={(v) => put({ ...p, protocol: v })}
                />
                <Choice
                  label="Publish mode"
                  value={p.mode}
                  options={["ingress", "host"]}
                  onChange={(v) => put({ ...p, mode: v })}
                />
              </>
            )}
          </Repeat>
          <Button
            type="button"
            variant="outline"
            disabled={portsPending || !s.repository.name}
            onClick={() => void suggestedPorts()}
          >
            <HugeiconsIcon icon={portsPending ? Loading03Icon : PlugSocketIcon} strokeWidth={2} aria-hidden="true" className={"size-4 shrink-0" + (portsPending ? " animate-spin motion-reduce:animate-none" : "")} />
            {portsPending
              ? "Loading exposed ports…"
              : "Add image exposed ports"}
          </Button>
          <Repeat
            title="Networks"
            value={s.networks}
            onChange={(v) => set("networks", v)}
            add={() => ({ networkName: "", serviceAliases: [] as string[] })}
          >
            {(n, put) => (
              <>
                <Suggest
                  label="Network"
                  value={n.networkName}
                  required
                  options={networks.data?.map((n) => n.networkName) || []}
                  onChange={(v) => put({ ...n, networkName: v })}
                />
                {editing && (
                  <Arguments
                    label="Service aliases"
                    value={n.serviceAliases}
                    onChange={(v) => put({ ...n, serviceAliases: v })}
                  />
                )}
              </>
            )}
          </Repeat>
          <ErrorMessage error={networks.error} />
          <Pairs
            label="Extra hosts (name / IP address)"
            value={s.hosts}
            onChange={(v) => set("hosts", v)}
          />
        </Section>
        <Section title="Storage">
          <Repeat
            title="Mounts"
            value={s.mounts}
            onChange={(v) => set("mounts", v)}
            add={() => ({
              containerPath: "",
              host: "",
              type: "volume",
              readOnly: false,
            })}
          >
            {(m, put) => (
              <>
                <Choice
                  label="Mount type"
                  value={m.type}
                  options={["volume", "bind", "tmpfs", "npipe", "cluster"]}
                  onChange={(v) => put({ ...m, type: v })}
                />
                <Field
                  label="Container path"
                  required
                  value={m.containerPath}
                  onChange={(e) => put({ ...m, containerPath: e.target.value })}
                />
                {m.type !== "tmpfs" && (
                  <Suggest
                    label={
                      m.type === "volume"
                        ? "Volume (empty for anonymous)"
                        : "Host path"
                    }
                    value={m.host || ""}
                    required={m.type === "bind"}
                    options={
                      m.type === "volume"
                        ? volumes.data?.map((v) => v.volumeName) || []
                        : []
                    }
                    onChange={(v) => put({ ...m, host: v })}
                  />
                )}
                <Toggle
                  label="Read only"
                  checked={m.readOnly}
                  onChange={(v) => put({ ...m, readOnly: v })}
                />
                {editing && m.type === "volume" && (
                  <div className="col-span-full space-y-3">
                    <Field
                      label="Volume driver (empty for default)"
                      value={m.volumeOptions?.driver?.name || ""}
                      onChange={(e) =>
                        put({
                          ...m,
                          volumeOptions: {
                            ...m.volumeOptions,
                            driver: {
                              ...m.volumeOptions?.driver,
                              name: e.target.value,
                            },
                          },
                        })
                      }
                    />
                    <Pairs
                      label="Volume driver options"
                      value={m.volumeOptions?.driver?.options}
                      onChange={(v) =>
                        put({
                          ...m,
                          volumeOptions: {
                            ...m.volumeOptions,
                            driver: {
                              name: m.volumeOptions?.driver?.name || "local",
                              options: v,
                            },
                          },
                        })
                      }
                    />
                    <Pairs
                      label="Volume labels"
                      value={Object.entries(m.volumeOptions?.labels || {}).map(
                        ([name, value]) => ({ name, value }),
                      )}
                      onChange={(v) =>
                        put({
                          ...m,
                          volumeOptions: {
                            ...m.volumeOptions,
                            labels: Object.fromEntries(
                              v.map((p) => [p.name, p.value]),
                            ),
                          },
                        })
                      }
                    />
                  </div>
                )}
              </>
            )}
          </Repeat>
          <ErrorMessage error={volumes.error} />
          <Repeat
            title="Secrets"
            value={s.secrets}
            onChange={(v) => set("secrets", v)}
            add={() => ({ secretName: "", secretTarget: "" })}
          >
            {(m, put) => (
              <>
                <Suggest
                  label="Secret"
                  required
                  value={m.secretName}
                  options={secrets.data?.map((v) => v.secretName) || []}
                  onChange={(v) => put({ ...m, secretName: v })}
                />
                <Field
                  label="Secret target (empty for default)"
                  value={m.secretTarget}
                  onChange={(e) => put({ ...m, secretTarget: e.target.value })}
                />
              </>
            )}
          </Repeat>
          <ErrorMessage error={secrets.error} />
          {(version?.docker.api || 0) >= 1.3 && (
            <>
              <Repeat
                title="Configs"
                value={s.configs}
                onChange={(v) => set("configs", v)}
                add={() => ({ configName: "", configTarget: "" })}
              >
                {(m, put) => (
                  <>
                    <Suggest
                      label="Config"
                      required
                      value={m.configName}
                      options={configs.data?.map((v) => v.configName) || []}
                      onChange={(v) => put({ ...m, configName: v })}
                    />
                    <Field
                      label="Config target (empty for default)"
                      value={m.configTarget}
                      onChange={(e) =>
                        put({ ...m, configTarget: e.target.value })
                      }
                    />
                  </>
                )}
              </Repeat>
              <ErrorMessage error={configs.error} />
            </>
          )}
        </Section>
        <Section title="Environment">
          <Pairs
            label="Environment variables"
            value={s.variables}
            onChange={(v) => set("variables", v)}
          />
          <Pairs
            label="Service labels"
            value={s.labels}
            onChange={(v) => set("labels", v)}
          />
          {editing && (
            <>
              <Pairs
                label="Container labels"
                value={s.containerLabels}
                onChange={(v) => set("containerLabels", v)}
              />
              <Pairs
                label="Service links"
                value={s.links}
                onChange={(v) => set("links", v)}
              />
            </>
          )}
          <Pairs
            label="Kernel parameters (sysctls)"
            value={s.sysctls}
            onChange={(v) => set("sysctls", v)}
          />
        </Section>
        <Section title="Resources">
          <div className="grid gap-4 sm:grid-cols-2">
            {(["reservation", "limit"] as const).map((kind) => (
              <div key={kind} className="space-y-4">
                <h3 className="capitalize font-medium">{kind}</h3>
                <Field
                  label={kind + " CPU cores"}
                  type="number"
                  min={0}
                  step="any"
                  value={s.resources[kind].cpu}
                  onChange={(e) =>
                    set("resources", {
                      ...s.resources,
                      [kind]: {
                        ...s.resources[kind],
                        cpu: Number(e.target.value),
                      },
                    })
                  }
                />
                <Field
                  label={kind + " memory (MiB)"}
                  type="number"
                  min={0}
                  step={1}
                  value={s.resources[kind].memory}
                  onChange={(e) =>
                    set("resources", {
                      ...s.resources,
                      [kind]: {
                        ...s.resources[kind],
                        memory: Number(e.target.value),
                      },
                    })
                  }
                />
              </div>
            ))}
          </div>
          <p className="text-xs text-muted-foreground">
            Zero leaves a resource unconstrained.
          </p>
        </Section>
        <Section title="Healthcheck">
          <Toggle
            label="Override image healthcheck"
            checked={!!s.healthcheck}
            onChange={(v) =>
              set(
                "healthcheck",
                v
                  ? {
                      test: ["CMD-SHELL", ""],
                      interval: 30,
                      timeout: 30,
                      startPeriod: 0,
                      retries: 3,
                    }
                  : undefined,
              )
            }
          />
          {s.healthcheck && (
            <>
              <Arguments
                label="Healthcheck test (first argument: CMD, CMD-SHELL, or NONE)"
                value={s.healthcheck.test}
                onChange={(v) =>
                  set("healthcheck", { ...s.healthcheck, test: v })
                }
              />
              <div className="grid gap-4 sm:grid-cols-2">
                {(
                  ["interval", "timeout", "startPeriod", "retries"] as const
                ).map((name) => (
                  <Field
                    key={name}
                    label={
                      name === "retries"
                        ? "Healthcheck retries"
                        : "Healthcheck " + name + " (seconds)"
                    }
                    type="number"
                    min={0}
                    step={name === "retries" ? 1 : "any"}
                    value={s.healthcheck?.[name] ?? 0}
                    onChange={(e) =>
                      set("healthcheck", {
                        ...s.healthcheck,
                        [name]: Number(e.target.value),
                      })
                    }
                  />
                ))}
              </div>
            </>
          )}
        </Section>
        <Section title="Deployment">
          <div className="grid gap-4 sm:grid-cols-2">
            <Choice
              label="Restart condition"
              value={policy.condition}
              options={["any", "none", "on-failure"]}
              onChange={(v) =>
                deploy("restartPolicy", { ...policy, condition: v })
              }
            />
            <Field
              label="Restart delay (seconds)"
              type="number"
              min={0}
              step="any"
              value={policy.delay}
              onChange={(e) =>
                deploy("restartPolicy", {
                  ...policy,
                  delay: Number(e.target.value),
                })
              }
            />
            <Field
              label="Restart attempts (0 for unlimited)"
              type="number"
              min={0}
              step={1}
              value={policy.attempts}
              onChange={(e) =>
                deploy("restartPolicy", {
                  ...policy,
                  attempts: Number(e.target.value),
                })
              }
            />
            <Field
              label="Restart window (seconds)"
              type="number"
              min={0}
              step="any"
              value={policy.window ?? 0}
              onChange={(e) =>
                deploy("restartPolicy", {
                  ...policy,
                  window: Number(e.target.value),
                })
              }
            />
            <Field
              label="Maximum replicas per node (empty for unlimited)"
              type="number"
              min={0}
              step={1}
              value={s.deployment.maxReplicas ?? ""}
              onChange={(e) =>
                deploy(
                  "maxReplicas",
                  e.target.value === "" ? null : Number(e.target.value),
                )
              }
            />
            <Toggle
              label="Automatically redeploy changed image"
              checked={s.deployment.autoredeploy}
              onChange={(v) => deploy("autoredeploy", v)}
            />
          </div>
          <DeployFields
            label="Update"
            value={s.deployment.update}
            onChange={(v) => deploy("update", v)}
          />
          <DeployFields
            label="Rollback"
            value={s.deployment.rollback}
            onChange={(v) => deploy("rollback", v)}
          />
          <Repeat
            title="Placement constraints"
            value={s.deployment.placement || []}
            add={() => ({ rule: "" })}
            onChange={(v) => deploy("placement", v)}
          >
            {(p, put) => (
              <Suggest
                label="Constraint"
                value={p.rule}
                required
                options={placement.data || []}
                onChange={(v) => put({ rule: v })}
                hint="For example: node.role == manager"
              />
            )}
          </Repeat>
          <ErrorMessage error={placement.error} />
        </Section>
        <Section title="Logging">
          <Suggest
            label="Logging driver"
            value={s.logdriver.name}
            options={
              drivers.data || [
                "json-file",
                "local",
                "syslog",
                "journald",
                "gelf",
                "fluentd",
                "awslogs",
                "splunk",
                "none",
              ]
            }
            onChange={(v) => set("logdriver", { ...s.logdriver, name: v })}
          />
          <Pairs
            label="Logging options"
            value={s.logdriver.opts}
            onChange={(v) => set("logdriver", { ...s.logdriver, opts: v })}
          />
          <ErrorMessage error={drivers.error} />
        </Section>
        {editing && (
          <Section title="Change record">
            <Field
              label="Comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              hint="Recorded in the stack history when this service belongs to a stored stack."
            />
          </Section>
        )}
        <ErrorMessage error={error || command.error} />
        <Submit pending={command.isPending} icon={editing ? Rocket01Icon : Add01Icon}>
          {editing ? "Deploy changes" : "Create service"}
        </Submit>
      </form>
    </Page>
  );
}
