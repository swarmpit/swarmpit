import { HugeiconsIcon } from "@hugeicons/react";
import { Delete02Icon, FloppyDiskIcon, PencilEdit01Icon, UserAdd01Icon } from "@hugeicons/core-free-icons";
import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { useApi, useCommand } from "@/lib/queries";
import { segment } from "@/lib/api";
import type { User } from "@/lib/types";
import {
  Action,
  Choice,
  ErrorMessage,
  Field,
  Loading,
  Page,
  Properties,
  Section,
  Submit,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { useSession } from "../session/session";
import { canAdmin } from "../resources/payload";
import { userPayload, type UserValues } from "./payload";

export function UserDetail() {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const { me } = useSession();
  const allowed = canAdmin(me?.role);
  const path = "/api/admin/users/" + segment(id);
  const query = useApi<User>(path, allowed);
  if (!allowed)
    return (
      <ErrorMessage error="User administration requires an administrator." />
    );
  if (query.isPending) return <Loading />;
  if (!query.data)
    return <ErrorMessage error={query.error || "User not found."} />;
  const user = query.data;
  return (
    <Page
      title={user.username}
      description="User account"
      actions={
        <>
          <Button variant="outline" asChild>
            <Link to={"/users/" + segment(id) + "/edit"}><HugeiconsIcon icon={PencilEdit01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Edit user</Link>
          </Button>
          <Action
            label="Delete user"
            icon={Delete02Icon}
            path={path}
            method="DELETE"
            destructive
            disabled={user.username === me?.username}
            confirm={
              "Delete " +
              user.username +
              " and all registries owned by this user?"
            }
            onSuccess={() => navigate("/users")}
          />
        </>
      }
    >
      <ErrorMessage error={query.error} />
      <Section title="Account details">
        <Properties
          values={{
            ID: user._id,
            Username: user.username,
            Email: user.email || "—",
            Role: user.role,
          }}
        />
        {user.username === me?.username && (
          <p className="mt-4 text-sm text-muted-foreground">
            Your own account cannot be deleted.
          </p>
        )}
      </Section>
    </Page>
  );
}
export function UserForm() {
  const { id } = useParams();
  const { me } = useSession();
  const allowed = canAdmin(me?.role);
  const query = useApi<User>(
    "/api/admin/users/" + segment(id || ""),
    allowed && !!id,
  );
  if (!allowed)
    return (
      <ErrorMessage error="User administration requires an administrator." />
    );
  if (id && query.isPending) return <Loading />;
  if (id && !query.data)
    return <ErrorMessage error={query.error || "User not found."} />;
  return (
    <>
      <ErrorMessage error={query.error} />
      <UserEditor key={id || "create"} existing={query.data} />
    </>
  );
}
function UserEditor({ existing }: { existing?: User }) {
  const navigate = useNavigate();
  const [values, setValues] = useState<UserValues>(() => ({
    username: existing?.username || "",
    email: existing?.email || "",
    role: existing?.role || "viewer",
    password: "",
  }));
  const command = useCommand<{ id?: string; _id?: string } | undefined>(
    (data) =>
      navigate(
        "/users/" + segment(existing?._id || data?._id || data?.id || ""),
      ),
  );
  return (
    <Page title={existing ? "Edit user" : "Create user"}>
      <Section title="Account">
        <form
          className="max-w-xl space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (command.isPending) return;
            command.mutate({
              path:
                "/api/admin/users" +
                (existing ? "/" + segment(existing._id) : ""),
              body: userPayload(values, existing),
            });
          }}
        >
          <Field
            label="Username"
            required
            minLength={3}
            autoComplete="off"
            disabled={!!existing}
            value={values.username}
            onChange={(e) => setValues({ ...values, username: e.target.value })}
          />
          {!existing && (
            <Field
              label="Password"
              type="password"
              required
              minLength={4}
              autoComplete="new-password"
              value={values.password}
              onChange={(e) =>
                setValues({ ...values, password: e.target.value })
              }
            />
          )}
          <Field
            label="Email"
            type="email"
            required
            value={values.email}
            onChange={(e) => setValues({ ...values, email: e.target.value })}
          />
          <Choice
            label="Role"
            value={values.role}
            options={["viewer", "user", "admin"]}
            onChange={(role) =>
              setValues({ ...values, role: role as User["role"] })
            }
          />
          <p className="text-sm text-muted-foreground">
            Viewers can inspect resources. Users can manage the swarm.
            Administrators can also manage users and nodes.
          </p>
          <ErrorMessage error={command.error} />
          <Submit pending={command.isPending} icon={existing ? FloppyDiskIcon : UserAdd01Icon}>
            {existing ? "Save changes" : "Create user"}
          </Submit>
        </form>
      </Section>
    </Page>
  );
}
