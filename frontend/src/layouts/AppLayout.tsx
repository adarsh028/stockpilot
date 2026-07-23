import { ComponentType, ReactNode, SVGProps, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Badge } from "@/components/states";
import {
  ChannelsIcon,
  DashboardIcon,
  InventoryIcon,
  Logo,
  LogOutIcon,
  PanelLeftIcon,
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

const COLLAPSE_KEY = "sp.sidebar.collapsed";

function initials(name?: string): string {
  if (!name) return "?";
  const parts = name.trim().split(/\s+/);
  return ((parts[0]?.[0] ?? "") + (parts.length > 1 ? parts[parts.length - 1][0] : "")).toUpperCase();
}

function Sidebar({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  const { user } = useAuth();
  const items = NAV.filter((n) => !n.roles || (user && n.roles.includes(user.role)));

  return (
    <aside
      className={`flex h-full shrink-0 flex-col border-r border-slate-200 bg-white transition-[width] duration-200 ${
        collapsed ? "w-[72px]" : "w-64"
      }`}
    >
      <div
        className={`flex h-16 items-center gap-2.5 border-b border-slate-100 px-4 ${
          collapsed ? "justify-center" : ""
        }`}
      >
        <Logo className="h-9 w-9 shrink-0" />
        {!collapsed && (
          <div className="min-w-0 flex-1">
            <p className="truncate text-[15px] font-bold leading-tight text-slate-900">StockPilot</p>
            <p className="truncate text-[11px] text-slate-400">Inventory OS</p>
          </div>
        )}
        {!collapsed && (
          <button
            onClick={onToggle}
            aria-label="Collapse sidebar"
            className="rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          >
            <PanelLeftIcon className="text-lg" />
          </button>
        )}
      </div>

      <nav className={`flex-1 space-y-1 overflow-y-auto py-4 ${collapsed ? "px-3" : "px-3"}`}>
        {!collapsed && (
          <p className="mb-1 px-3 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
            Menu
          </p>
        )}
        {items.map((n) => {
          const Icon = n.icon;
          return (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === "/"}
              title={collapsed ? n.label : undefined}
              className={({ isActive }) =>
                `group relative flex items-center rounded-lg text-sm font-medium transition ${
                  collapsed ? "justify-center px-0 py-2.5" : "gap-3 px-3 py-2"
                } ${
                  isActive
                    ? "bg-brand-50 text-brand-700"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                }`
              }
            >
              {({ isActive }) => (
                <>
                  {isActive && (
                    <span className="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-r-full bg-brand-600" />
                  )}
                  <Icon
                    className={`text-xl transition-colors ${
                      isActive ? "text-brand-600" : "text-slate-400 group-hover:text-slate-600"
                    }`}
                  />
                  {!collapsed && <span className="truncate">{n.label}</span>}
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      <div className="border-t border-slate-100 p-3">
        {collapsed ? (
          <button
            onClick={onToggle}
            aria-label="Expand sidebar"
            className="flex w-full items-center justify-center rounded-lg py-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          >
            <PanelLeftIcon className="text-lg" />
          </button>
        ) : (
          <p className="px-2 text-[11px] text-slate-400">StockPilot v0.1</p>
        )}
      </div>
    </aside>
  );
}

function Header({ children }: { children?: ReactNode }) {
  const { user, logout } = useAuth();
  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-slate-200 bg-white/80 px-6 backdrop-blur">
      <div className="min-w-0">{children}</div>
      <div className="flex items-center gap-3">
        <div className="hidden text-right sm:block">
          <p className="text-sm font-medium leading-tight text-slate-800">{user?.fullName}</p>
          <p className="truncate text-xs text-slate-400">{user?.organizationName || user?.email}</p>
        </div>
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white">
          {initials(user?.fullName)}
        </span>
        <Badge color="brand">{user?.role}</Badge>
        <div className="h-6 w-px bg-slate-200" />
        <button
          onClick={logout}
          title="Sign out"
          className="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-red-600"
        >
          <LogOutIcon className="text-lg" />
        </button>
      </div>
    </header>
  );
}

export function AppLayout() {
  const [collapsed, setCollapsed] = useState<boolean>(
    () => localStorage.getItem(COLLAPSE_KEY) === "1",
  );

  const toggle = () => {
    setCollapsed((c) => {
      const next = !c;
      localStorage.setItem(COLLAPSE_KEY, next ? "1" : "0");
      return next;
    });
  };

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar collapsed={collapsed} onToggle={toggle} />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto bg-slate-50 p-6 lg:p-8">
          <div className="mx-auto max-w-[1400px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
