import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { productsApi } from "@/api/products";
import { Button, Card, Input, PageHeader, Pagination } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { EditIcon, PlusIcon, ProductsIcon, SearchIcon, TrashIcon, UploadIcon } from "@/components/icons";
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
    return (
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-base text-slate-300">
        <ProductsIcon />
      </div>
    );
  }
  return (
    <img
      src={src}
      alt=""
      className="h-10 w-10 shrink-0 rounded-lg border border-slate-200 object-cover"
    />
  );
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
    <div className="space-y-6">
      <PageHeader title="Products" subtitle="Your product catalog and variants">
        {canEdit && (
          <>
            <Link to="/products/import">
              <Button variant="secondary">
                <UploadIcon className="text-base" /> Import CSV/Excel
              </Button>
            </Link>
            <Link to="/products/new">
              <Button>
                <PlusIcon className="text-base" /> New product
              </Button>
            </Link>
          </>
        )}
      </PageHeader>

      <Card>
        <div className="mb-4 max-w-sm">
          <div className="relative">
            <SearchIcon className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-base text-slate-400" />
            <Input
              placeholder="Search by name or brand..."
              className="pl-9"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        {query.isLoading ? (
          <LoadingSpinner />
        ) : query.isError ? (
          <ErrorState message={apiErrorMessage(query.error)} />
        ) : query.data && query.data.content.length === 0 ? (
          <EmptyState
            icon={<ProductsIcon />}
            title="No products yet"
            hint="Create one or import a spreadsheet to get started."
          />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Brand</th>
                    <th className="text-center">SKUs</th>
                    <th>Status</th>
                    {canEdit && <th className="text-right">Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {query.data?.content.map((p) => (
                    <tr key={p.id}>
                      <td className="font-medium text-slate-800">
                        <div className="flex items-center gap-3">
                          <Thumbnail src={primaryImage(p)} />
                          <span>{p.name}</span>
                        </div>
                      </td>
                      <td>{p.categoryName ?? "—"}</td>
                      <td>{p.brandName ?? "—"}</td>
                      <td className="text-center tabular-nums">{p.skus.length}</td>
                      <td>
                        <Badge color={p.status === "ACTIVE" ? "green" : "slate"}>{p.status}</Badge>
                      </td>
                      {canEdit && (
                        <td className="text-right">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              onClick={() => navigate(`/products/${p.id}/edit`)}
                              title="Edit"
                              className="rounded-md p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-brand-600"
                            >
                              <EditIcon className="text-base" />
                            </button>
                            <button
                              onClick={() => {
                                if (confirm(`Delete "${p.name}"?`)) remove.mutate(p.id);
                              }}
                              title="Delete"
                              className="rounded-md p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                            >
                              <TrashIcon className="text-base" />
                            </button>
                          </div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {query.data && query.data.totalPages > 1 && (
              <Pagination
                page={query.data.page}
                totalPages={query.data.totalPages}
                first={query.data.first}
                last={query.data.last}
                onPrev={() => setPage((p) => p - 1)}
                onNext={() => setPage((p) => p + 1)}
              />
            )}
          </>
        )}
      </Card>
    </div>
  );
}
