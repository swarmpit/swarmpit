import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, Link } from "react-router";
import {
  LayoutDashboard,
  Layers,
  Box,
  Network,
  HardDrive,
  KeyRound,
  FileText,
  Server,
  ListTodo,
  Container,
  Users,
  Settings,
  Sun,
  Moon,
  Monitor,
  LogOut,
  Menu,
  X,
  ExternalLink,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { useSession } from "@/features/session/session";
import { canAdmin, canWrite } from "@/features/resources/payload";
import { cn } from "@/lib/utils";
const navigation = [
  { to: "/", label: "Overview", icon: LayoutDashboard },
  { to: "/services", label: "Services", icon: Box },
  { to: "/stacks", label: "Stacks", icon: Layers },
  { to: "/nodes", label: "Nodes", icon: Server },
  { to: "/tasks", label: "Tasks", icon: ListTodo },
  { to: "/networks", label: "Networks", icon: Network },
  { to: "/volumes", label: "Volumes", icon: HardDrive },
  { to: "/secrets", label: "Secrets", icon: KeyRound, write: true },
  { to: "/configs", label: "Configs", icon: FileText },
  { to: "/registries", label: "Registries", icon: Container, write: true },
  { to: "/users", label: "Users", icon: Users, admin: true },
];
type Theme = "system" | "light" | "dark";
export function Shell() {
  const session = useSession();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [theme, setTheme] = useState<Theme>(() => {
    try {
      const s = localStorage.getItem("swarmpit.theme");
      return s === "light" || s === "dark" ? s : "system";
    } catch {
      return "system";
    }
  });
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);
  useEffect(() => {
    const media = matchMedia("(prefers-color-scheme: dark)");
    const update = () =>
      document.documentElement.classList.toggle(
        "dark",
        theme === "dark" || (theme === "system" && media.matches),
      );
    update();
    media.addEventListener("change", update);
    try {
      localStorage.setItem("swarmpit.theme", theme);
    } catch {}
    return () => media.removeEventListener("change", update);
  }, [theme]);
  useEffect(() => {
    document.title =
      (session.version?.instanceName || "Swarmpit") + " · Swarm management";
  }, [session.version?.instanceName]);
  const ThemeIcon = theme === "dark" ? Moon : theme === "light" ? Sun : Monitor;
  return (
    <div className="min-h-svh">
      <a
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById("main-content")?.focus();
        }}
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-card focus:p-3"
      >
        Skip to content
      </a>
      {open && (
        <button
          aria-label="Close navigation"
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
          onClick={() => setOpen(false)}
        />
      )}
      <aside
        aria-label="Main navigation"
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-60 flex-col border-r bg-card transition-transform md:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <Link to="/" className="flex h-20 items-center gap-3 px-6">
          <img src="/img/icon.svg" alt="" className="size-8" />
          <span className="truncate text-lg font-semibold tracking-tight">
            {session.version?.instanceName || "swarmpit"}
          </span>
        </Link>
        <div className="mx-4 mb-5 flex items-center gap-2 rounded-lg border bg-background px-3 py-2 text-xs">
          <span className="size-1.5 rounded-full bg-emerald-500" />
          <span className="truncate">Docker Swarm</span>
          <span className="ml-auto text-muted-foreground">
            {session.version?.docker.engine || "Cluster"}
          </span>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto px-3">
          {navigation
            .filter(
              (n) =>
                (!n.admin || canAdmin(session.me?.role)) &&
                (!n.write || canWrite(session.me?.role)),
            )
            .map((n) => (
              <NavLink
                key={n.to}
                end={n.to === "/"}
                to={n.to}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 rounded-md px-3 py-2 text-sm transition-colors",
                    isActive
                      ? "bg-primary/10 font-medium text-primary"
                      : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  )
                }
              >
                <n.icon className="size-4" />
                {n.label}
              </NavLink>
            ))}
        </nav>
        <div className="space-y-1 border-t p-3">
          <NavLink
            to="/account-settings"
            className="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
          >
            <Settings className="size-4" />
            Account settings
          </NavLink>
          <a
            href="/?ui=classic"
            className="flex items-center gap-3 rounded-md px-3 py-2 text-xs text-muted-foreground hover:bg-muted"
          >
            <ExternalLink className="size-3.5" />
            Classic UI
          </a>
          <div className="flex items-center justify-between px-3 pt-3 text-xs text-muted-foreground">
            <span>v{session.version?.version}</span>
            <span>React edition</span>
          </div>
        </div>
      </aside>
      <div className="md:pl-60">
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between gap-4 border-b bg-background/95 px-5 backdrop-blur-sm lg:px-8">
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={() => setOpen((v) => !v)}
              aria-label={open ? "Close navigation" : "Open navigation"}
              aria-expanded={open}
            >
              {open ? <X /> : <Menu />}
            </Button>
            <span className="text-sm text-muted-foreground">
              Workspace <span className="mx-2 text-border">/</span>
              <span className="font-medium text-foreground">
                {navigation.find((n) =>
                  n.to === "/"
                    ? location.pathname === "/"
                    : location.pathname.startsWith(n.to),
                )?.label || "Account"}
              </span>
            </span>
          </div>
          <div className="flex items-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              title={"Theme: " + theme}
              aria-label={"Theme: " + theme + ". Change theme"}
              onClick={() =>
                setTheme(
                  theme === "system"
                    ? "light"
                    : theme === "light"
                      ? "dark"
                      : "system",
                )
              }
            >
              <ThemeIcon className="size-4" />
            </Button>
            <div className="hidden text-right sm:block">
              <p className="text-xs font-medium">{session.me?.username}</p>
              <p className="text-[11px] text-muted-foreground">
                {session.me?.role}
              </p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              aria-label="Sign out"
              disabled={loggingOut}
              onClick={async () => {
                setLoggingOut(true);
                try {
                  await session.logout();
                } finally {
                  setLoggingOut(false);
                }
              }}
            >
              <LogOut className="size-4" />
            </Button>
          </div>
        </header>
        <main
          id="main-content"
          tabIndex={-1}
          className="mx-auto max-w-[1600px] p-5 pb-12 lg:p-8"
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
}
