import { Outlet } from "react-router-dom";
import { PageHeader, RouteTabs } from "@/components/ui";
import { ClockIcon, SalesIcon, UploadIcon } from "@/components/icons";

/**
 * Shell for the Sales section. Recording orders, importing a spreadsheet and reviewing
 * past import runs were three separate routes reached via header buttons; they're now
 * deep-linkable tabs on one page. Each tab's own primary action lives in its content.
 */
export default function SalesLayout() {
  return (
    <div className="space-y-6">
      <PageHeader title="Sales" subtitle="Orders, imports and history across every channel" />

      <RouteTabs
        items={[
          { to: "/sales", label: "Orders", icon: SalesIcon, end: true },
          { to: "/sales/import", label: "Import", icon: UploadIcon },
          { to: "/sales/batches", label: "Import history", icon: ClockIcon },
        ]}
      />

      <Outlet />
    </div>
  );
}
