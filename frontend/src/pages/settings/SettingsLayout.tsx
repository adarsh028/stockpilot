import { Outlet } from "react-router-dom";
import { PageHeader, RouteTabs, RouteTabItem } from "@/components/ui";
import { BuildingIcon, PlugIcon } from "@/components/icons";

/**
 * Shell for Settings. The organization profile and storage integrations are peer
 * tabs. Product categories moved out to the Master Data section.
 */
export default function SettingsLayout() {
  const tabs: RouteTabItem[] = [
    { to: "/settings", label: "General", icon: BuildingIcon, end: true },
    { to: "/settings/integrations", label: "Integrations", icon: PlugIcon },
  ];

  return (
    <div className="space-y-6">
      <PageHeader title="Settings" subtitle="Manage your organization and integrations" />
      <RouteTabs items={tabs} />
      <Outlet />
    </div>
  );
}
