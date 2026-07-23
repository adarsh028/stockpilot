import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { productsApi } from "@/api/products";
import { Button, Card, Input } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";
import { Product } from "@/types/api";

/** Primary image of a product's first variant that has one, else any first image. */
function primaryImage(p: Product): string | null {
  for (const sku of p.skus) {
    const primary = sku.images?.find((img) => img.primary);
    if (primary) return primary.url;
  }
  for (const sku of p.skus) {
    if (sku.images?.length) return sku.images[0].url;
  }
  return p.imageUrl ?? null;
}

function Thumbnail({ src }: { src: string | null }) {
  if (!src) {
    return <div className="h-10 w-10 shrink-0 rounded-md bg-slate-100" />;
  }
  return <img src={src} alt="" className="h-10 w-10 shrink-0 rounded-md border border-slate-200 object-cover" />;
}

export default function ProductList() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { user } = useAuth();
  const canEdit = user?.role === "OWNER" || user?.role === "ADMIN";
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ["products", search, page],
    queryFn: () => productsApi.list({ search, page, size: 10 }),
  });

  const remove = useMutation({
    mutationFn: (id: string) => productsApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["products"] }),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-800">Products</h1>
        {canEdit && (
          <div className="flex gap-2">
            <Link to="/products/import">
              <Button variant="secondary">Import CSV/Excel</Button>
            </Link>
            <Link to="/products/new">
              <Button>+ New product</Button>
            </Link>
          </div>
        )}
      </div>

      <Card>
        <div className="mb-4 max-w-sm">
          <Input
            placeholder="Search by name, brand, category..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>

        {query.isLoading ? (
          <LoadingSpinner />
        ) : query.isError ? (
          <ErrorState message={apiErrorMessage(query.error)} />
        ) : query.data && query.data.content.length === 0 ? (
          <EmptyState title="No products yet" hint="Create one or import a spreadsheet." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-slate-500">
                    <th className="py-2">Name</th>
                    <th className="py-2">Category</th>
                    <th className="py-2">Brand</th>
                    <th className="py-2 text-center">SKUs</th>
                    <th className="py-2">Status</th>
                    {canEdit && <th className="py-2 text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {query.data?.content.map((p) => (
                    <tr key={p.id} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="py-2 font-medium text-slate-700">
                        <div className="flex items-center gap-3">
                          <Thumbnail src={primaryImage(p)} />
                          <span>{p.name}</span>
                        </div>
                      </td>
                      <td className="py-2 text-slate-500">{p.category ?? "—"}</td>
                      <td className="py-2 text-slate-500">{p.brandName ?? "—"}</td>
                      <td className="py-2 text-center">{p.skus.length}</td>
                      <td className="py-2">
                        <Badge color={p.status === "ACTIVE" ? "green" : "slate"}>{p.status}</Badge>
                      </td>
                      {canEdit && (
                        <td className="py-2 text-right">
                          <button
                            onClick={() => navigate(`/products/${p.id}/edit`)}
                            className="mr-3 text-brand-600 hover:underline"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => {
                              if (confirm(`Delete "${p.name}"?`)) remove.mutate(p.id);
                            }}
                            className="text-red-600 hover:underline"
                          >
                            Delete
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {query.data && query.data.totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between text-sm">
                <span className="text-slate-500">
                  Page {query.data.page + 1} of {query.data.totalPages}
                </span>
                <div className="flex gap-2">
                  <Button variant="secondary" disabled={query.data.first} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button variant="secondary" disabled={query.data.last} onClick={() => setPage((p) => p + 1)}>
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
}
