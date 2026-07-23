import { ComponentType, SVGProps, useEffect, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Badge } from "@/components/states";
import {
  ChannelsIcon,
  DashboardIcon,
  InventoryIcon,
  Logo,
  LogOutIcon,
  MenuIcon,
  ProductsIcon,
  SalesIcon,
  SettingsIcon,
  TeamIcon,
} from "@/components/icons";

interface NavItem {
  to: string;
  label: string;
  icon: ComponentType<SVGProps<SVGSVGElement>>;
  roles?: string[];
}

const NAV: NavItem[] = [
  { to: "/", label: "Dashboard", icon: DashboardIcon },
  { to: "/products", label: "Products", icon: ProductsIcon },
  { to: "/inventory", label: "Inventory", icon: InventoryIcon },
  { to: "/channels", label: "Channels", icon: ChannelsIcon },
  { to: "/sales", label: "Sales", icon: SalesIcon },
  { to: "/team", label: "Team", icon: TeamIcon, roles: ["OWNER", "ADMIN"] },
  { to: "/settings", label: "Settings", icon: SettingsIcon },
];

// Labels/wordmark: shown on the mobile drawer (full width) and on desktop only while
// the rail is expanded. Kept in the DOM and faded so the icon never shifts position.
function reveal(expanded: boolean): string {
  return `whitespace-nowrap transition-opacity duration-200 ${expanded ? "lg:opacity-100" : "lg:opacity-0"}`;
}

function initials(name?: string): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
}

function Sidebar({
  mobileOpen,
  onNavigate,
  onExpandedChange,
}: {
  mobileOpen: boolean;
  onNavigate: () => void;
  onExpandedChange: (expanded: boolean) => void;
}) {
  const { user } = useAuth();
  const items = NAV.filter((n) => !n.roles || (user && n.roles.includes(user.role)));

  // The collapsed desktop rail expands on hover OR keyboard focus; either one keeps
  // it open, and it only collapses once both are gone. `onExpandedChange` lets the
  // layout grow the reserved content gutter in lockstep so the rail never overlaps
  // the page — it pushes content aside instead of floating over it.
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const expanded = hovered || focused;

  useEffect(() => {
    onExpandedChange(expanded);
  }, [expanded, onExpandedChange]);

  return (
    <>
      {/* Mobile backdrop */}
      <div
        aria-hidden="true"
        onClick={onNavigate}
        className={`fixed inset-0 z-30 bg-slate-900/50 backdrop-blur-sm transition-opacity duration-200 lg:hidden ${
          mobileOpen ? "opacity-100" : "pointer-events-none opacity-0"
        }`}
      />

      <aside
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col overflow-hidden border-r border-slate-800 bg-slate-900 text-slate-300 shadow-xl transition-[transform,width] duration-200 ease-out ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        } lg:translate-x-0 ${expanded ? "lg:w-64 lg:shadow-2xl" : "lg:w-[76px] lg:shadow-none"}`}
      >
        <div className="flex h-16 shrink-0 items-center gap-3 px-[18px]">
          <Logo className="h-9 w-9 shrink-0" />
          <div className={`min-w-0 ${reveal(expanded)}`}>
            <p className="truncate text-[15px] font-bold leading-tight text-white">StockPilot</p>
            <p className="truncate text-[11px] text-slate-400">Inventory OS</p>
          </div>
        </div>

        <nav className="flex-1 space-y-1 overflow-y-auto overflow-x-hidden px-3 py-3">
          {items.map((n) => {
            const Icon = n.icon;
            return (
              <NavLink
                key={n.to}
                to={n.to}
                end={n.to === "/"}
                onClick={onNavigate}
                title={n.label}
                className={({ isActive }) =>
                  `group/item flex items-center gap-3 rounded-lg px-[11px] py-2.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/60 ${
                    isActive
                      ? "bg-brand-600 text-white shadow-sm"
                      : "text-slate-300 hover:bg-slate-800 hover:text-white"
                  }`
                }
              >
                {({ isActive }) => (
                  <>
                    <Icon
                      className={`shrink-0 text-xl transition-colors ${
                        isActive ? "text-white" : "text-slate-400 group-hover/item:text-white"
                      }`}
                    />
                    <span className={`truncate ${reveal(expanded)}`}>{n.label}</span>
                  </>
                )}
              </NavLink>
            );
          })}
        </nav>

        <div className="shrink-0 px-3 pb-4 pt-2">
          <p className={`px-2 text-[11px] text-slate-500 ${reveal(expanded)}`}>StockPilot v0.1</p>
        </div>
      </aside>
    </>
  );
}

function Header({ onMenu }: { onMenu: () => void }) {
  const { user, logout } = useAuth();
  return (
    <header className="flex h-16 shrink-0 items-center justify-between gap-3 border-b border-slate-200 bg-white/80 px-4 backdrop-blur sm:px-6">
      <div className="flex items-center gap-2">
        <button
          onClick={onMenu}
          aria-label="Open menu"
          className="rounded-lg p-2 text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 lg:hidden"
        >
          <MenuIcon className="text-xl" />
        </button>
        <div className="flex items-center gap-2 lg:hidden">
          <Logo className="h-8 w-8" />
          <span className="text-[15px] font-bold text-slate-900">StockPilot</span>
        </div>
      </div>
      <div className="flex items-center gap-2 sm:gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium leading-tight text-slate-800">{user?.fullName}</p>
          <p className="truncate text-xs text-slate-400">{user?.organizationName || user?.email}</p>
        </div>
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white">
          {initials(user?.fullName)}
        </span>
        <span className="hidden sm:inline-flex">
          <Badge color="brand">{user?.role}</Badge>
        </span>
        <div className="hidden h-6 w-px bg-slate-200 sm:block" />
        <button
          onClick={logout}
          title="Sign out"
          aria-label="Sign out"
          className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-red-600"
        >
          <LogOutIcon className="text-lg" />
        </button>
      </div>
    </header>
  );
}

export function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [sidebarExpanded, setSidebarExpanded] = useState(false);

  // Escape closes the mobile drawer.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") setMobileOpen(false);
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <Sidebar
        mobileOpen={mobileOpen}
        onNavigate={() => setMobileOpen(false)}
        onExpandedChange={setSidebarExpanded}
      />
      {/* Reserves the rail's current width on desktop, growing/shrinking in step with
          it so expanding the rail pushes this content aside instead of covering it. */}
      <div
        className={`hidden shrink-0 transition-[width] duration-200 ease-out lg:block ${
          sidebarExpanded ? "lg:w-64" : "lg:w-[76px]"
        }`}
        aria-hidden="true"
      />

      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <Header onMenu={() => setMobileOpen(true)} />
        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
