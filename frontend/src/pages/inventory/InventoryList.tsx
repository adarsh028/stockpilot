import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inventoryApi } from "@/api/inventory";
import { Button, Card, Input, Modal, formatNumber } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { apiErrorMessage } from "@/api/client";
import { InventoryItem } from "@/types/api";
import { useAuth } from "@/context/AuthContext";

export default function InventoryList() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const canEdit = user?.role === "OWNER" || user?.role === "ADMIN";
  const [page, setPage] = useState(0);
  const [showLowOnly, setShowLowOnly] = useState(false);
  const [adjust, setAdjust] = useState<InventoryItem | null>(null);

  const list = useQuery({
    queryKey: ["inventory", page],
    queryFn: () => inventoryApi.list(page, 15),
    enabled: !showLowOnly,
  });
  const low = useQuery({
    queryKey: ["low-stock"],
    queryFn: () => inventoryApi.lowStock(),
    enabled: showLowOnly,
  });

  const items = showLowOnly ? low.data ?? [] : list.data?.content ?? [];
  const loading = showLowOnly ? low.isLoading : list.isLoading;
  const error = showLowOnly ? low.error : list.error;
  const isError = showLowOnly ? low.isError : list.isError;

  const adjustMut = useMutation({
    mutationFn: (body: { skuId: string; newQuantity: number; reason: string }) => inventoryApi.adjust(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["inventory"] });
      qc.invalidateQueries({ queryKey: ["low-stock"] });
      setAdjust(null);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-800">Inventory</h1>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={showLowOnly} onChange={(e) => setShowLowOnly(e.target.checked)} />
          Show low-stock only
        </label>
      </div>

      <Card>
        {loading ? (
          <LoadingSpinner />
        ) : isError ? (
          <ErrorState message={apiErrorMessage(error)} />
        ) : items.length === 0 ? (
          <EmptyState title={showLowOnly ? "No low-stock items" : "No inventory yet"} />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Product</th>
                  <th className="py-2">SKU</th>
                  <th className="py-2 text-right">On hand</th>
                  <th className="py-2 text-right">Allocated</th>
                  <th className="py-2 text-right">Reorder at</th>
                  <th className="py-2">Status</th>
                  {canEdit && <th className="py-2 text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {items.map((it) => (
                  <tr key={it.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-2 font-medium text-slate-700">{it.productName}</td>
                    <td className="py-2 text-slate-500">{it.sku}</td>
                    <td className="py-2 text-right">{formatNumber(it.quantityOnHand)}</td>
                    <td className="py-2 text-right">{formatNumber(it.totalAllocated)}</td>
                    <td className="py-2 text-right">{it.reorderLevel}</td>
                    <td className="py-2">
                      {it.lowStock ? <Badge color="red">Low stock</Badge> : <Badge color="green">OK</Badge>}
                    </td>
                    {canEdit && (
                      <td className="py-2 text-right">
                        <button onClick={() => setAdjust(it)} className="text-brand-600 hover:underline">
                          Adjust
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {!showLowOnly && list.data && list.data.totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-slate-500">
              Page {list.data.page + 1} of {list.data.totalPages}
            </span>
            <div className="flex gap-2">
              <Button variant="secondary" disabled={list.data.first} onClick={() => setPage((p) => p - 1)}>
                Previous
              </Button>
              <Button variant="secondary" disabled={list.data.last} onClick={() => setPage((p) => p + 1)}>
                Next
              </Button>
            </div>
          </div>
        )}
      </Card>

      <AdjustModal key={adjust?.id ?? "none"} item={adjust} onClose={() => setAdjust(null)} onSave={(qty, reason) =>
        adjust && adjustMut.mutate({ skuId: adjust.skuId, newQuantity: qty, reason })
      } error={adjustMut.isError ? apiErrorMessage(adjustMut.error) : ""} saving={adjustMut.isPending} />
    </div>
  );
}

function AdjustModal({
  item,
  onClose,
  onSave,
  error,
  saving,
}: {
  item: InventoryItem | null;
  onClose: () => void;
  onSave: (qty: number, reason: string) => void;
  error: string;
  saving: boolean;
}) {
  const [qty, setQty] = useState(item?.quantityOnHand ?? 0);
  const [reason, setReason] = useState("");
  return (
    <Modal open={!!item} title={`Adjust stock — ${item?.sku ?? ""}`} onClose={onClose}>
      <div className="space-y-4">
        {error && <ErrorState message={error} />}
        <p className="text-sm text-slate-500">
          Current on-hand: <b>{item?.quantityOnHand}</b>
        </p>
        <Input
          label="New quantity on hand"
          type="number"
          defaultValue={item?.quantityOnHand}
          onChange={(e) => setQty(Number(e.target.value))}
        />
        <Input label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Stock count, restock, damage..." />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => onSave(qty, reason)} disabled={saving}>
            {saving ? "Saving..." : "Save"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
