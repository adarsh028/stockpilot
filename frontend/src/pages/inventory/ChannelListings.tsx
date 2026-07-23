import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inventoryApi } from "@/api/inventory";
import { channelsApi } from "@/api/channels";
import { Button, Card, Input, Modal, PageHeader, Select, formatNumber } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { ArrowLeftIcon, InventoryIcon, PlusIcon } from "@/components/icons";
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
    <div className="space-y-6">
      <div className="space-y-3">
        <Link
          to="/channels"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-slate-500 transition hover:text-slate-700"
        >
          <ArrowLeftIcon className="text-base" /> Channels
        </Link>
        <PageHeader title={`Allocations — ${channel?.name ?? "Channel"}`} subtitle="Stock committed to this channel from central inventory">
          <Button onClick={() => setShowAdd(true)}>
            <PlusIcon className="text-base" /> Allocate SKU
          </Button>
        </PageHeader>
      </div>

      <Card>
        {listings.isLoading ? (
          <LoadingSpinner />
        ) : listings.isError ? (
          <ErrorState message={apiErrorMessage(listings.error)} />
        ) : listings.data && listings.data.length === 0 ? (
          <EmptyState icon={<InventoryIcon />} title="No allocations yet" hint="Allocate stock from your central inventory to this channel." />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th className="text-right">Allocated</th>
                  <th className="text-right">Channel price</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {listings.data?.map((l) => (
                  <tr key={l.id}>
                    <td className="font-medium text-slate-800">{l.productName}</td>
                    <td className="font-mono text-xs text-slate-500">{l.sku}</td>
                    <td className="text-right tabular-nums">{formatNumber(l.allocatedQuantity)}</td>
                    <td className="text-right tabular-nums">{l.channelPrice ?? "—"}</td>
                    <td>
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
