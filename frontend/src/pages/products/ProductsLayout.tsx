import { Link, Outlet } from "react-router-dom";
import { Button, PageHeader, RouteTabs } from "@/components/ui";
import { ListIcon, PlusIcon, UploadIcon } from "@/components/icons";
import { useAuth } from "@/context/AuthContext";

/**
 * Shell for the Products section. The catalog and the bulk importer are two views of
 * the same thing, so they live side-by-side as deep-linkable tabs rather than pages
 * hopped between with buttons. The New-product CTA stays pinned in the header.
 */
export default function ProductsLayout() {
  const { user } = useAuth();
  const canEdit = user?.role === "OWNER" || user?.role === "ADMIN";

  return (
    <div className="space-y-6">
      <PageHeader title="Products" subtitle="Your product catalog, variants and bulk imports">
        {canEdit && (
          <Link to="/products/new">
            <Button>
              <PlusIcon className="text-base" /> New product
            </Button>
          </Link>
        )}
      </PageHeader>

      <RouteTabs
        items={[
          { to: "/products", label: "Catalog", icon: ListIcon, end: true },
          { to: "/products/import", label: "Import", icon: UploadIcon },
        ]}
      />

      <Outlet />
    </div>
  );
}
