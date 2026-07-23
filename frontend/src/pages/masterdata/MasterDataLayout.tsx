import { Outlet } from "react-router-dom";
import { PageHeader, RouteTabs, RouteTabItem } from "@/components/ui";
import { TagIcon } from "@/components/icons";

/**
 * Shell for Master Data — the reference lists products and orders are built from
 * (categories today, more to come). Each list is a deep-linkable tab so new master
 * data can be added by appending to `tabs` and a matching route in App.tsx.
 */
export default function MasterDataLayout() {
  const tabs: RouteTabItem[] = [
    { to: "/master-data", label: "Categories", icon: TagIcon, end: true },
  ];

  return (
    <div className="space-y-6">
      <PageHeader title="Master Data" subtitle="Manage the reference lists your catalog is built from" />
      <RouteTabs items={tabs} />
      <Outlet />
    </div>
  );
}
