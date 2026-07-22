import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inventoryApi } from "@/api/inventory";
import { channelsApi } from "@/api/channels";
import { Button, Card, Input, Modal, Select, formatNumber } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { apiErrorMessage } from "@/api/client";

export default function ChannelListings() {
  const { channelId } = useParams();
  const qc = useQueryClient();
  const [showAdd, setShowAdd] = useState(false);

  const channels = useQuery({ queryKey: ["channels"], queryFn: () => channelsApi.list() });
  const channel = channels.data?.find((c) => c.id === channelId);

  const listings = useQuery({
    queryKey: ["listings", channelId],
    queryFn: () => inventoryApi.listingsForChannel(channelId!),
  });
  const inventory = useQuery({ queryKey: ["inventory-all"], queryFn: () => inventoryApi.list(0, 500) });

  const upsert = useMutation({
    mutationFn: (body: { skuId: string; allocatedQuantity: number; channelPrice?: number }) =>
      inventoryApi.upsertListing(channelId!, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["listings", channelId] });
      setShowAdd(false);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <Link to="/channels" className="text-sm text-brand-600 hover:underline">
            ← Channels
          </Link>
          <h1 className="text-2xl font-bold text-slate-800">
            Allocations — {channel?.name ?? "Channel"}
          </h1>
        </div>
        <Button onClick={() => setShowAdd(true)}>+ Allocate SKU</Button>
      </div>

      <Card>
        {listings.isLoading ? (
          <LoadingSpinner />
        ) : listings.isError ? (
          <ErrorState message={apiErrorMessage(listings.error)} />
        ) : listings.data && listings.data.length === 0 ? (
          <EmptyState title="No allocations yet" hint="Allocate stock from your central inventory to this channel." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Product</th>
                  <th className="py-2">SKU</th>
                  <th className="py-2 text-right">Allocated</th>
                  <th className="py-2 text-right">Channel price</th>
                  <th className="py-2">Status</th>
                </tr>
              </thead>
              <tbody>
                {listings.data?.map((l) => (
                  <tr key={l.id} className="border-b border-slate-50">
                    <td className="py-2 font-medium text-slate-700">{l.productName}</td>
                    <td className="py-2 text-slate-500">{l.sku}</td>
                    <td className="py-2 text-right">{formatNumber(l.allocatedQuantity)}</td>
                    <td className="py-2 text-right">{l.channelPrice ?? "—"}</td>
                    <td className="py-2">
                      <Badge color={l.status === "ACTIVE" ? "green" : "slate"}>{l.status}</Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <Modal open={showAdd} title="Allocate SKU to channel" onClose={() => setShowAdd(false)}>
        <AllocateForm
          skus={inventory.data?.content ?? []}
          onSubmit={(body) => upsert.mutate(body)}
          error={upsert.isError ? apiErrorMessage(upsert.error) : ""}
          saving={upsert.isPending}
        />
      </Modal>
    </div>
  );
}

function AllocateForm({
  skus,
  onSubmit,
  error,
  saving,
}: {
  skus: { skuId: string; sku: string; productName: string; quantityOnHand: number }[];
  onSubmit: (body: { skuId: string; allocatedQuantity: number; channelPrice?: number }) => void;
  error: string;
  saving: boolean;
}) {
  const [skuId, setSkuId] = useState("");
  const [qty, setQty] = useState(0);
  const [price, setPrice] = useState<number | "">("");

  return (
    <div className="space-y-4">
      {error && <ErrorState message={error} />}
      <Select label="SKU" value={skuId} onChange={(e) => setSkuId(e.target.value)}>
        <option value="">Select a SKU...</option>
        {skus.map((s) => (
          <option key={s.skuId} value={s.skuId}>
            {s.productName} — {s.sku} (on hand: {s.quantityOnHand})
          </option>
        ))}
      </Select>
      <Input label="Quantity to allocate" type="number" value={qty} onChange={(e) => setQty(Number(e.target.value))} />
      <Input
        label="Channel price (optional)"
        type="number"
        value={price}
        onChange={(e) => setPrice(e.target.value === "" ? "" : Number(e.target.value))}
      />
      <div className="flex justify-end">
        <Button
          onClick={() => onSubmit({ skuId, allocatedQuantity: qty, channelPrice: price === "" ? undefined : price })}
          disabled={!skuId || saving}
        >
          {saving ? "Allocating..." : "Allocate"}
        </Button>
      </div>
    </div>
  );
}
