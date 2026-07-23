import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inventoryApi } from "@/api/inventory";
import { Button, Card, Input, Modal, PageHeader, Pagination, formatNumber } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { InventoryIcon } from "@/components/icons";
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
    <div className="space-y-6">
      <PageHeader title="Inventory" subtitle="Central stock across all variants">
        <label
          className={`flex cursor-pointer items-center gap-2 rounded-lg border px-3 py-2 text-sm transition ${
            showLowOnly
              ? "border-brand-200 bg-brand-50 text-brand-700"
              : "border-slate-300 bg-white text-slate-600 hover:bg-slate-50"
          }`}
        >
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/30"
            checked={showLowOnly}
            onChange={(e) => setShowLowOnly(e.target.checked)}
          />
          Show low-stock only
        </label>
      </PageHeader>

      <Card>
        {loading ? (
          <LoadingSpinner />
        ) : isError ? (
          <ErrorState message={apiErrorMessage(error)} />
        ) : items.length === 0 ? (
          <EmptyState icon={<InventoryIcon />} title={showLowOnly ? "No low-stock items" : "No inventory yet"} />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th className="text-right">On hand</th>
                  <th className="text-right">Allocated</th>
                  <th className="text-right">Reorder at</th>
                  <th>Status</th>
                  {canEdit && <th className="text-right">Actions</th>}
                </tr>
              </thead>
              <tbody>
                {items.map((it) => (
                  <tr key={it.id}>
                    <td className="font-medium text-slate-800">{it.productName}</td>
                    <td className="font-mono text-xs text-slate-500">{it.sku}</td>
                    <td className="text-right tabular-nums">{formatNumber(it.quantityOnHand)}</td>
                    <td className="text-right tabular-nums">{formatNumber(it.totalAllocated)}</td>
                    <td className="text-right tabular-nums">{it.reorderLevel}</td>
                    <td>
                      {it.lowStock ? <Badge color="red">Low stock</Badge> : <Badge color="green">OK</Badge>}
                    </td>
                    {canEdit && (
                      <td className="text-right">
                        <button
                          onClick={() => setAdjust(it)}
                          className="rounded-md px-2.5 py-1 text-sm font-medium text-brand-600 transition hover:bg-brand-50"
                        >
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
          <Pagination
            page={list.data.page}
            totalPages={list.data.totalPages}
            first={list.data.first}
            last={list.data.last}
            onPrev={() => setPage((p) => p - 1)}
            onNext={() => setPage((p) => p + 1)}
          />
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
        <div className="rounded-lg bg-slate-50 px-3 py-2 text-sm text-slate-600">
          Current on-hand: <b className="text-slate-900 tabular-nums">{item?.quantityOnHand}</b>
        </div>
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
