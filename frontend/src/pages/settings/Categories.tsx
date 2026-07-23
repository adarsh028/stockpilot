import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { categoriesApi, CategoryInput } from "@/api/categories";
import { Button, Card, Input, Modal, PageHeader } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { ArrowLeftIcon, PlusIcon, ProductsIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { Category } from "@/types/api";
import { useAuth } from "@/context/AuthContext";

export default function Categories() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const canEdit = user?.role === "OWNER" || user?.role === "ADMIN";
  const [editing, setEditing] = useState<Category | null>(null);
  const [creating, setCreating] = useState(false);

  const query = useQuery({ queryKey: ["categories"], queryFn: () => categoriesApi.list() });

  const save = useMutation({
    mutationFn: (v: { id?: string; body: CategoryInput }) =>
      v.id ? categoriesApi.update(v.id, v.body) : categoriesApi.create(v.body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["categories"] });
      setEditing(null);
      setCreating(false);
    },
  });

  const deactivate = useMutation({
    mutationFn: (id: string) => categoriesApi.deactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["categories"] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Categories" subtitle="Manage the categories products can be assigned to">
        <Link
          to="/settings"
          className="inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
        >
          <ArrowLeftIcon className="text-base" /> Back to settings
        </Link>
        {canEdit && (
          <Button onClick={() => setCreating(true)}>
            <PlusIcon className="text-base" /> New category
          </Button>
        )}
      </PageHeader>

      <Card>
        {query.isLoading ? (
          <LoadingSpinner />
        ) : query.isError ? (
          <ErrorState message={apiErrorMessage(query.error)} />
        ) : query.data && query.data.length === 0 ? (
          <EmptyState
            icon={<ProductsIcon />}
            title="No categories yet"
            hint="Create one so it's available when adding a product."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Status</th>
                  {canEdit && <th className="text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {query.data?.map((c) => (
                  <tr key={c.id}>
                    <td className="font-medium text-slate-800">{c.name}</td>
                    <td>
                      <Badge color={c.isActive ? "green" : "slate"}>{c.isActive ? "Active" : "Inactive"}</Badge>
                    </td>
                    {canEdit && (
                      <td className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            onClick={() => setEditing(c)}
                            className="rounded-md px-2.5 py-1 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                          >
                            Edit
                          </button>
                          {c.isActive && (
                            <button
                              onClick={() => deactivate.mutate(c.id)}
                              className="rounded-md px-2.5 py-1 text-sm font-medium text-red-600 transition hover:bg-red-50"
                            >
                              Deactivate
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {(creating || editing) && (
        <CategoryModal
          key={editing?.id ?? "new"}
          category={editing}
          onClose={() => {
            setEditing(null);
            setCreating(false);
          }}
          onSave={(body) => save.mutate({ id: editing?.id, body })}
          error={save.isError ? apiErrorMessage(save.error) : ""}
          saving={save.isPending}
        />
      )}
    </div>
  );
}

function CategoryModal({
  category,
  onClose,
  onSave,
  error,
  saving,
}: {
  category: Category | null;
  onClose: () => void;
  onSave: (body: CategoryInput) => void;
  error: string;
  saving: boolean;
}) {
  const [name, setName] = useState(category?.name ?? "");
  const [isActive, setIsActive] = useState(category?.isActive ?? true);

  return (
    <Modal open title={category ? "Edit category" : "New category"} onClose={onClose}>
      <div className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Apparel" />
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/30"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
          />
          Active
        </label>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => onSave({ name, isActive })} disabled={!name || saving}>
            {saving ? "Saving..." : "Save"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
