import { Outlet } from "react-router-dom";
import { PageHeader, RouteTabs, RouteTabItem } from "@/components/ui";
import { BuildingIcon, PlugIcon, TagIcon } from "@/components/icons";
import { useAuth } from "@/context/AuthContext";

/**
 * Shell for Settings. The organization profile, storage integrations and product
 * categories used to be scattered across cards and a separate route; they're now
 * peer tabs. The Categories tab only appears for roles allowed to manage them.
 */
export default function SettingsLayout() {
  const { user } = useAuth();
  const canManageCategories = user?.role === "OWNER" || user?.role === "ADMIN";

  const tabs: RouteTabItem[] = [
    { to: "/settings", label: "General", icon: BuildingIcon, end: true },
    { to: "/settings/integrations", label: "Integrations", icon: PlugIcon },
    ...(canManageCategories
      ? [{ to: "/settings/categories", label: "Categories", icon: TagIcon } as RouteTabItem]
      : []),
  ];

  return (
    <div className="space-y-6">
      <PageHeader title="Settings" subtitle="Manage your organization, integrations and catalog setup" />
      <RouteTabs items={tabs} />
      <Outlet />
    </div>
  );
}
