import { useEffect, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, Link } from "react-router";
import { HugeiconsIcon } from "@hugeicons/react";
import {
  DashboardSquare01Icon,
  Layers01Icon,
  CubeIcon,
  NetworkIcon,
  HardDriveIcon,
  Key01Icon,
  File01Icon,
  ServerStack01Icon,
  Task01Icon,
  ContainerIcon,
  UserGroupIcon,
  Sun03Icon,
  Moon02Icon,
  ComputerIcon,
  Logout01Icon,
  LinkSquare01Icon,
  UnfoldMoreIcon,
  Settings01Icon,
} from "@hugeicons/core-free-icons";
import {
  DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuGroup, DropdownMenuLabel,
  DropdownMenuItem, DropdownMenuSeparator, DropdownMenuSub, DropdownMenuSubTrigger,
  DropdownMenuSubContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuPortal,
} from "@/components/ui/dropdown-menu";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { WorkspaceLoading } from "./workspace-loading";
import { Breadcrumb, BreadcrumbList, BreadcrumbItem, BreadcrumbLink, BreadcrumbSeparator } from "@/components/ui/breadcrumb";
import { useSession } from "@/features/session/session";
import { canAdmin, canWrite } from "@/features/resources/payload";
import {
  Sidebar, SidebarContent, SidebarFooter, SidebarGroup, SidebarGroupContent, SidebarHeader,
  SidebarInset, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
  SidebarProvider, SidebarTrigger, useSidebar,
} from "@/components/ui/sidebar";
const navigation = [
  { to: "/", label: "Overview", icon: DashboardSquare01Icon },
  { to: "/services", label: "Services", icon: CubeIcon },
  { to: "/stacks", label: "Stacks", icon: Layers01Icon },
  { to: "/nodes", label: "Nodes", icon: ServerStack01Icon },
  { to: "/tasks", label: "Tasks", icon: Task01Icon },
  { to: "/networks", label: "Networks", icon: NetworkIcon },
  { to: "/volumes", label: "Volumes", icon: HardDriveIcon },
  { to: "/secrets", label: "Secrets", icon: Key01Icon, write: true },
  { to: "/configs", label: "Configs", icon: File01Icon },
  { to: "/registries", label: "Registries", icon: ContainerIcon, write: true },
  { to: "/users", label: "Users", icon: UserGroupIcon, admin: true },
];
type Theme = "system" | "light" | "dark";
export function Shell({ pending = false }: { pending?: boolean }) {
  return <SidebarProvider className="h-dvh min-h-0 overflow-hidden"><ShellContent pending={pending} /></SidebarProvider>;
}
function ShellContent({ pending }: { pending: boolean }) {
  const { open, openMobile, isMobile, setOpenMobile } = useSidebar();
  const session = useSession();
  const location = useLocation();
  const contentRef = useRef<HTMLDivElement>(null);
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
    setOpenMobile(false);
    if (contentRef.current) contentRef.current.scrollTop = 0;
  }, [location.pathname, setOpenMobile]);
  useEffect(() => {
    const media = matchMedia("(prefers-color-scheme: dark)");
    const update = () => {
      const dark = theme === "dark" || (theme === "system" && media.matches);
      document.documentElement.classList.toggle("dark", dark);
      document.querySelector('meta[name="theme-color"]')?.setAttribute("content", dark ? "#000" : "#fafafa");
    };
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
  const themeIcon = theme === "dark" ? Moon02Icon : theme === "light" ? Sun03Icon : ComputerIcon;
  const username = session.me?.username || "Account";
  const appVersion = session.version?.version?.replace(/^v/, "");
  const brandLabel = appVersion ? `Swarmpit v${appVersion}` : "Swarmpit";
  const breadcrumb = navigation.find((item) =>
    item.to === "/" ? location.pathname === "/" : location.pathname === item.to || location.pathname.startsWith(item.to + "/"),
  ) || (location.pathname === "/account-settings" ? { to: "/account-settings", label: "Account settings" } : undefined);
  return (
    <>
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
      <Sidebar variant="inset" collapsible="icon">
        <SidebarHeader className="in-data-[mobile=true]:pr-10">
          <SidebarMenu>
            <SidebarMenuItem>
              {pending ? (
                <SidebarMenuButton size="lg" disabled aria-label="Swarmpit loading">
                  <img src="/img/icon.svg" alt="" className="size-8 shrink-0" />
                  <span className="grid flex-1 gap-1 text-left leading-tight group-data-[collapsible=icon]:hidden">
                    <span className="truncate font-semibold">Swarmpit</span>
                    <Skeleton className="h-3 w-20" />
                  </span>
                </SidebarMenuButton>
              ) : <SidebarMenuButton asChild size="lg" tooltip={brandLabel} className="hover:bg-transparent hover:text-sidebar-foreground active:bg-transparent active:text-sidebar-foreground">
                <a href="https://swarmpit.io/" onClick={() => setOpenMobile(false)} aria-label={brandLabel + " website"} title={session.version?.docker.engine ? `Docker Swarm ${session.version.docker.engine}` : undefined}>
                  <img src="/img/icon.svg" alt="" className="size-8 shrink-0" />
                  <span className="grid flex-1 text-left leading-tight">
                    <span className="truncate font-semibold">
                      Swarmpit
                    </span>
                    {appVersion && (
                      <span className="mt-1 truncate text-xs text-muted-foreground">
                        v{appVersion}
                      </span>
                    )}
                  </span>
                </a>
              </SidebarMenuButton>}
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <nav aria-label="Main navigation">
              <SidebarMenu>
                {pending ? navigation.map((item) => (
                  <SidebarMenuItem key={item.to} aria-hidden="true">
                    <div className="flex h-8 items-center gap-2 rounded-md px-2 group-data-[collapsible=icon]:justify-center">
                      <Skeleton className="size-4 shrink-0" />
                      <Skeleton className="h-4 w-24 group-data-[collapsible=icon]:hidden" />
                    </div>
                  </SidebarMenuItem>
                )) : navigation
                  .filter(
                    (n) => (!n.admin || canAdmin(session.me?.role)) &&
                      (!n.write || canWrite(session.me?.role)),
                  )
                  .map((n) => (
                    <SidebarMenuItem key={n.to}>
                      <SidebarMenuButton
                        asChild
                        tooltip={n.label}
                        isActive={n.to === "/" ? location.pathname === "/" : location.pathname === n.to || location.pathname.startsWith(n.to + "/")}
                      >
                        <NavLink end={n.to === "/"} to={n.to} aria-label={n.label} onClick={() => setOpenMobile(false)}>
                          <HugeiconsIcon icon={n.icon} strokeWidth={2} aria-hidden="true" />
                          <span>{n.label}</span>
                        </NavLink>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))}
              </SidebarMenu>
            </nav>
          </SidebarGroup>
          <SidebarGroup className="mt-auto">
            <SidebarGroupContent>
              <nav aria-label="Resources">
                <SidebarMenu>
                  {pending ? ["Swagger API", "Classic UI"].map((label) => (
                    <SidebarMenuItem key={label} aria-hidden="true">
                      <div className="flex h-7 items-center gap-2 rounded-md px-2 group-data-[collapsible=icon]:h-8 group-data-[collapsible=icon]:justify-center">
                        <Skeleton className="size-4 shrink-0" />
                        <Skeleton className="h-3 w-24 group-data-[collapsible=icon]:hidden" />
                      </div>
                    </SidebarMenuItem>
                  )) : (
                    <>
                      <SidebarMenuItem>
                        <SidebarMenuButton asChild size="sm" tooltip="Swagger API (opens in a new tab)">
                          <a href="/api-docs" target="_blank" rel="noopener noreferrer" aria-label="Swagger API (opens in a new tab)" onClick={() => setOpenMobile(false)}>
                            <HugeiconsIcon icon={LinkSquare01Icon} strokeWidth={2} aria-hidden="true" />
                            <span>Swagger API</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton asChild size="sm" tooltip="Classic UI">
                          <a href="/?ui=classic" aria-label="Classic UI" onClick={() => setOpenMobile(false)}>
                            <HugeiconsIcon icon={ComputerIcon} strokeWidth={2} aria-hidden="true" />
                            <span>Classic UI</span>
                          </a>
                        </SidebarMenuButton>
                      </SidebarMenuItem>
                    </>
                  )}
                </SidebarMenu>
              </nav>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu>
            <SidebarMenuItem>
              {pending ? (
                <div aria-hidden="true" className="flex h-12 items-center gap-2 rounded-md p-2 group-data-[collapsible=icon]:size-8 group-data-[collapsible=icon]:p-0">
                  <Skeleton className="size-8 shrink-0 rounded-lg" />
                  <div className="flex-1 space-y-1 group-data-[collapsible=icon]:hidden">
                    <Skeleton className="h-4 w-24" />
                    <Skeleton className="h-3 w-16" />
                  </div>
                </div>
              ) : <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <SidebarMenuButton
                    size="lg"
                    tooltip={username + " · User menu"}
                    aria-label={"User menu for " + username}
                    disabled={loggingOut}
                    className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground"
                  >
                    <Avatar aria-hidden="true" className="size-8 rounded-lg">
                      <AvatarFallback className="rounded-lg">{username.slice(0, 2).toUpperCase()}</AvatarFallback>
                    </Avatar>
                    <span className="grid min-w-0 flex-1 text-left text-sm leading-tight group-data-[collapsible=icon]:hidden">
                      <span className="truncate font-medium">{username}</span>
                      <span className="truncate text-xs">{session.me?.role}</span>
                    </span>
                    <HugeiconsIcon icon={UnfoldMoreIcon} strokeWidth={2} aria-hidden="true" className="ml-auto size-4 group-data-[collapsible=icon]:hidden" />
                  </SidebarMenuButton>
                </DropdownMenuTrigger>
                <DropdownMenuContent side={isMobile ? "bottom" : "right"} align="end" sideOffset={4} collisionPadding={16} className="w-(--radix-dropdown-menu-trigger-width) min-w-56 max-w-[calc(100vw-2rem)] rounded-lg">
                  <DropdownMenuLabel className="p-0 font-normal">
                    <div className="flex min-w-0 items-center gap-2 px-1 py-1.5 text-left text-sm">
                      <Avatar aria-hidden="true" className="size-8 rounded-lg">
                        <AvatarFallback className="rounded-lg">{username.slice(0, 2).toUpperCase()}</AvatarFallback>
                      </Avatar>
                      <div className="grid min-w-0 flex-1 text-left text-sm leading-tight">
                        <span className="truncate font-medium">{username}</span>
                        <span className="truncate text-xs">{session.me?.role}</span>
                      </div>
                    </div>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuGroup>
                    <DropdownMenuItem asChild>
                      <Link to="/account-settings" onClick={() => setOpenMobile(false)}><HugeiconsIcon icon={Settings01Icon} strokeWidth={2} aria-hidden="true" />Account settings</Link>
                    </DropdownMenuItem>
                    <DropdownMenuSub>
                      <DropdownMenuSubTrigger><HugeiconsIcon icon={themeIcon} strokeWidth={2} aria-hidden="true" />Theme</DropdownMenuSubTrigger>
                      <DropdownMenuPortal>
                        <DropdownMenuSubContent className="min-w-40 rounded-lg">
                          <DropdownMenuRadioGroup value={theme} onValueChange={(value) => { if (value === "light" || value === "dark" || value === "system") setTheme(value); }}>
                            <DropdownMenuRadioItem value="light"><HugeiconsIcon icon={Sun03Icon} strokeWidth={2} aria-hidden="true" />Light</DropdownMenuRadioItem>
                            <DropdownMenuRadioItem value="dark"><HugeiconsIcon icon={Moon02Icon} strokeWidth={2} aria-hidden="true" />Dark</DropdownMenuRadioItem>
                            <DropdownMenuRadioItem value="system"><HugeiconsIcon icon={ComputerIcon} strokeWidth={2} aria-hidden="true" />System</DropdownMenuRadioItem>
                          </DropdownMenuRadioGroup>
                        </DropdownMenuSubContent>
                      </DropdownMenuPortal>
                    </DropdownMenuSub>
                  </DropdownMenuGroup>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem
                    disabled={loggingOut}
                    onSelect={async () => {
                      setLoggingOut(true);
                      try { await session.logout(); } finally { setLoggingOut(false); }
                    }}
                  >
                    <HugeiconsIcon icon={Logout01Icon} strokeWidth={2} aria-hidden="true" />{loggingOut ? "Signing out…" : "Sign out"}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>}
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
      </Sidebar>
      <SidebarInset className="min-h-0 min-w-0 overflow-hidden md:border">
        <header className="z-20 flex h-16 shrink-0 items-center justify-between gap-4 border-b bg-card/95 px-4 backdrop-blur-sm md:rounded-t-xl lg:px-6">
          <div className="flex items-center gap-3">
            <SidebarTrigger
              className="size-9"
              aria-label={isMobile ? (openMobile ? "Close navigation" : "Open navigation") : (open ? "Collapse sidebar" : "Expand sidebar")}
              aria-expanded={isMobile ? openMobile : open}
            />
            <Breadcrumb aria-label="Breadcrumb" className="min-w-0">
              {pending ? <Skeleton className="h-4 w-28" /> : <BreadcrumbList className="min-w-0 flex-nowrap">
                <BreadcrumbItem className="shrink-0">
                  <BreadcrumbLink asChild className={location.pathname === "/" ? "font-medium text-foreground" : undefined}>
                    <Link to="/" aria-current={location.pathname === "/" ? "page" : undefined} className="rounded-sm focus-visible:outline-2 focus-visible:outline-ring">Overview</Link>
                  </BreadcrumbLink>
                </BreadcrumbItem>
                {breadcrumb && breadcrumb.to !== "/" && (
                  <>
                    <BreadcrumbSeparator />
                    <BreadcrumbItem className="min-w-0">
                      <BreadcrumbLink asChild className="font-medium text-foreground">
                        <Link to={breadcrumb.to} aria-current={location.pathname === breadcrumb.to ? "page" : undefined} className="truncate rounded-sm focus-visible:outline-2 focus-visible:outline-ring">
                          {breadcrumb.label}
                        </Link>
                      </BreadcrumbLink>
                    </BreadcrumbItem>
                  </>
                )}
              </BreadcrumbList>}
            </Breadcrumb>
          </div>

        </header>
        <div
          id="main-content"
          ref={contentRef}
          tabIndex={-1}
          className="min-h-0 flex-1 overflow-auto overscroll-none [scrollbar-gutter:stable] focus-visible:-outline-offset-2"
        >
          <div className="mx-auto w-full max-w-[1600px] p-4 pb-12 lg:p-6">
            {pending ? <WorkspaceLoading pathname={location.pathname} statistics={session.version?.statistics} /> : <Outlet />}
          </div>
        </div>
      </SidebarInset>
    </>
  );
}
