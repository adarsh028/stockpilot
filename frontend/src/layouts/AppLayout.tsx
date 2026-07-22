import { ReactNode } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { Badge } from "@/components/states";

interface NavItem {
  to: string;
  label: string;
  icon: string;
  roles?: string[];
}

const NAV: NavItem[] = [
  { to: "/", label: "Dashboard", icon: "📊" },
  { to: "/products", label: "Products", icon: "📦" },
  { to: "/inventory", label: "Inventory", icon: "🏷️" },
  { to: "/channels", label: "Channels", icon: "🛒" },
  { to: "/sales", label: "Sales", icon: "💰" },
  { to: "/team", label: "Team", icon: "👥", roles: ["OWNER", "ADMIN"] },
  { to: "/settings", label: "Settings", icon: "⚙️" },
];

function Sidebar() {
  const { user } = useAuth();
  return (
    <aside className="flex h-full w-60 flex-col border-r border-slate-200 bg-white">
      <div className="flex items-center gap-2 px-5 py-5">
        <span className="text-xl">🚀</span>
        <span className="text-lg font-bold text-brand-700">StockPilot</span>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {NAV.filter((n) => !n.roles || (user && n.roles.includes(user.role))).map((n) => (
          <NavLink
            key={n.to}
            to={n.to}
            end={n.to === "/"}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition ${
                isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-50"
              }`
            }
          >
            <span>{n.icon}</span>
            {n.label}
          </NavLink>
        ))}
      </nav>
      <div className="border-t border-slate-100 px-5 py-4 text-xs text-slate-400">
        StockPilot v0.1
      </div>
    </aside>
  );
}

function Header({ children }: { children?: ReactNode }) {
  const { user, logout } = useAuth();
  return (
    <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
      <div>{children}</div>
      <div className="flex items-center gap-3">
        <div className="text-right">
          <p className="text-sm font-medium text-slate-700">{user?.fullName}</p>
          <p className="text-xs text-slate-400">{user?.organizationName || user?.email}</p>
        </div>
        <Badge color="brand">{user?.role}</Badge>
        <button onClick={logout} className="text-sm text-slate-500 hover:text-red-600">
          Logout
        </button>
      </div>
    </header>
  );
}

export function AppLayout() {
  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto bg-slate-50 p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
