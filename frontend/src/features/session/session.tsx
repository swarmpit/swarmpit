import { HugeiconsIcon } from "@hugeicons/react";
import { LinkSquare01Icon, Login01Icon, UserAdd01Icon } from "@hugeicons/core-free-icons";
import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { Navigate, useLocation } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import {
  api,
  basicAuth,
  saveToken,
  storedToken,
  isSessionError,
} from "@/lib/api";
import { useApi } from "@/lib/queries";
import type { Me, Version } from "@/lib/types";
import {
  Loading,
  ErrorMessage,
  Section,
  Field,
  Submit,
} from "@/components/common";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

interface Session {
  me?: Me;
  version?: Version;
  token: string | null;
  notice: string;
  login: (
    username: string,
    password: string,
    initialize: boolean,
  ) => Promise<void>;
  logout: () => Promise<void>;
  loading: boolean;
  error: unknown;
}
const Context = createContext<Session | null>(null);
export function SessionProvider({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const [token, setToken] = useState(storedToken);
  const [notice, setNotice] = useState("");
  const version = useApi<Version>("/version");
  const me = useApi<Me>("/api/me", !!token);
  function clear(message?: string) {
    let warning = message;
    try {
      saveToken(null);
    } catch {
      warning = [
        message,
        "Browser storage could not be cleared. Close this browser session before using a shared device.",
      ]
        .filter(Boolean)
        .join(" ");
    }
    setToken(null);
    client.clear();
    if (warning !== undefined) setNotice(warning);
  }
  useEffect(() => {
    function expired() {
      clear("Your session expired. Sign in again.");
    }
    function storage(e: StorageEvent) {
      if (e.key === "token") {
        client.clear();
        setToken(storedToken());
      }
    }
    window.addEventListener("swarmpit:session-expired", expired);
    window.addEventListener("storage", storage);
    return () => {
      window.removeEventListener("swarmpit:session-expired", expired);
      window.removeEventListener("storage", storage);
    };
  }, [client]);
  useEffect(() => {
    if (isSessionError(me.error)) {
      clear("Your session expired. Sign in again.");
    }
  }, [me.error]);
  async function login(
    username: string,
    password: string,
    initialize: boolean,
  ) {
    if (initialize)
      await api("/initialize", {
        method: "POST",
        body: { username, password },
      });
    const response = await api<{ token: string }>("/login", {
      method: "POST",
      headers: { Authorization: basicAuth(username, password) },
    });
    if (!response.token)
      throw new Error("Server did not return an authorization token");
    client.clear();
    saveToken(response.token);
    setToken(response.token);
    setNotice("");
    await client.invalidateQueries();
  }
  async function logout() {
    const authorization = token;
    clear("");
    if (!authorization) return;
    try {
      await api("/logout", {
        method: "POST",
        headers: { Authorization: authorization },
        signal: AbortSignal.timeout(10000),
      });
    } catch (error) {
      setNotice((previous) =>
        [
          previous,
          "Signed out locally. Server token revocation failed: " +
            (error instanceof Error ? error.message : "Request failed"),
        ]
          .filter(Boolean)
          .join(" "),
      );
    }
  }
  return (
    <Context.Provider
      value={{
        me: me.data,
        version: version.data,
        token,
        notice,
        login,
        logout,
        loading: version.isPending || (!!token && me.isPending),
        error: version.error || (isSessionError(me.error) ? null : me.error),
      }}
    >
      {children}
    </Context.Provider>
  );
}
export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("SessionProvider missing");
  return value;
}
export function RequireSession({ children, fallback = <Loading /> }: { children: ReactNode; fallback?: ReactNode }) {
  const session = useSession();
  const location = useLocation();
  if (!session.token)
    return (
      <Navigate
        to="/login"
        state={{ from: location.pathname + location.search }}
        replace
      />
    );
  if (session.loading) return fallback;
  if (session.error) return <ErrorMessage error={session.error} />;
  return children;
}
export function Login() {
  const session = useSession();
  const location = useLocation();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const from = (location.state as { from?: string } | null)?.from;
  const target =
    from?.startsWith("/") && !from.startsWith("//") && from !== "/login"
      ? from
      : "/services";
  if (session.token && session.me) return <Navigate to={target} replace />;
  const initialize = session.version?.initialized === false;
  return (
    <div className="flex min-h-svh items-center justify-center bg-muted/30 p-6">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <img src="/img/icon.svg" className="mx-auto mb-4 size-12" alt="" />
          <h1 className="text-2xl font-semibold">
            {session.version?.instanceName || "Swarmpit"}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Your swarm, in view.
          </p>
        </div>
        <Section
          title={
            initialize ? "Create your first administrator" : "Welcome back"
          }
        >
          {session.loading && !session.version ? (
            <div role="status" aria-label="Sign-in form loading" aria-busy="true" className="space-y-4">
              <span className="sr-only">Loading sign-in form…</span>
              {[0, 1].map((field) => (
                <div key={field} aria-hidden="true" className="space-y-2">
                  <Skeleton className="h-3.5 w-20" />
                  <Skeleton className="h-9 w-full" />
                </div>
              ))}
              <Skeleton className="h-9 w-20" />
            </div>
          ) : (
            <form
              className="space-y-4"
              onSubmit={async (e) => {
                e.preventDefault();
                if (pending) return;
                setPending(true);
                setError(null);
                try {
                  await session.login(username, password, initialize);
                } catch (e) {
                  setError(e);
                } finally {
                  setPending(false);
                }
              }}
            >
              <Field
                label="Username"
                required
                autoComplete="username"
                minLength={initialize ? 3 : undefined}
                value={username}
                onChange={(e) => setUsername(e.target.value)}
              />
              <Field
                label="Password"
                type="password"
                required
                minLength={initialize ? 8 : undefined}
                autoComplete={initialize ? "new-password" : "current-password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <ErrorMessage error={error || session.error} />
              {session.notice && (
                <p role="status" className="text-sm text-muted-foreground">
                  {session.notice}
                </p>
              )}
              <Submit pending={pending || session.loading} icon={initialize ? UserAdd01Icon : Login01Icon}>
                {initialize ? "Create account & sign in" : "Sign in"}
              </Submit>
            </form>
          )}
        </Section>
        <p className="text-center text-xs text-muted-foreground">
          Docker Swarm management · {session.version?.version || ""}
        </p>
        <Button variant="link" asChild className="mx-auto flex">
          <a href="/?ui=classic"><HugeiconsIcon icon={LinkSquare01Icon} strokeWidth={2} aria-hidden="true" className="size-4 shrink-0" />Open classic UI</a>
        </Button>
      </div>
    </div>
  );
}
