import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { salesApi, SaleInput } from "@/api/sales";
import { channelsApi } from "@/api/channels";
import { inventoryApi } from "@/api/inventory";
import { Button, Card, Input, Modal, PageHeader, Pagination, Select, formatCurrency } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { PlusIcon, SalesIcon, UploadIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";

export default function SalesList() {
  const qc = useQueryClient();
  const [channelId, setChannelId] = useState("");
  const [page, setPage] = useState(0);
  const [recording, setRecording] = useState(false);

  const channels = useQuery({ queryKey: ["channels"], queryFn: () => channelsApi.list() });
  const sales = useQuery({
    queryKey: ["sales", channelId, page],
    queryFn: () => salesApi.list({ channelId: channelId || undefined, page, size: 15 }),
  });

  const returnMut = useMutation({
    mutationFn: (id: string) => salesApi.returnSale(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["sales"] }),
  });
  const deleteMut = useMutation({
    mutationFn: (id: string) => salesApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["sales"] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Sales" subtitle="Recorded orders across every channel">
        <Link to="/sales/import">
          <Button variant="secondary">
            <UploadIcon className="text-base" /> Import
          </Button>
        </Link>
        <Link to="/sales/batches">
          <Button variant="secondary">Import history</Button>
        </Link>
        <Button onClick={() => setRecording(true)}>
          <PlusIcon className="text-base" /> Record sale
        </Button>
      </PageHeader>

      <Card>
        <div className="mb-4 w-full sm:w-56">
          <Select label="Filter by channel" value={channelId} onChange={(e) => { setChannelId(e.target.value); setPage(0); }}>
            <option value="">All channels</option>
            {channels.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </div>

        {sales.isLoading ? (
          <LoadingSpinner />
        ) : sales.isError ? (
          <ErrorState message={apiErrorMessage(sales.error)} />
        ) : sales.data && sales.data.content.length === 0 ? (
          <EmptyState icon={<SalesIcon />} title="No sales recorded" hint="Record a manual sale or import a spreadsheet." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Product / SKU</th>
                    <th>Channel</th>
                    <th className="text-right">Qty</th>
                    <th className="text-right">Total</th>
                    <th>Status</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sales.data?.content.map((s) => (
                    <tr key={s.id}>
                      <td className="whitespace-nowrap tabular-nums">{new Date(s.saleDate).toLocaleDateString()}</td>
                      <td>
                        <span className="font-medium text-slate-800">{s.productName}</span>
                        <span className="ml-1 font-mono text-xs text-slate-400">({s.sku})</span>
                      </td>
                      <td>{s.channelName}</td>
                      <td className="text-right tabular-nums">{s.quantity}</td>
                      <td className="text-right font-medium tabular-nums text-slate-800">{formatCurrency(s.totalAmount)}</td>
                      <td>
                        <Badge color={s.status === "COMPLETED" ? "green" : "amber"}>{s.status}</Badge>
                      </td>
                      <td className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          {s.status === "COMPLETED" && (
                            <button
                              onClick={() => returnMut.mutate(s.id)}
                              className="rounded-md px-2.5 py-1 text-sm font-medium text-amber-600 transition hover:bg-amber-50"
                            >
                              Return
                            </button>
                          )}
                          <button
                            onClick={() => confirm("Delete this sale?") && deleteMut.mutate(s.id)}
                            className="rounded-md px-2.5 py-1 text-sm font-medium text-red-600 transition hover:bg-red-50"
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {sales.data && sales.data.totalPages > 1 && (
              <Pagination
                page={sales.data.page}
                totalPages={sales.data.totalPages}
                first={sales.data.first}
                last={sales.data.last}
                onPrev={() => setPage((p) => p - 1)}
                onNext={() => setPage((p) => p + 1)}
              />
            )}
          </>
        )}
      </Card>

      {recording && <RecordSaleModal onClose={() => setRecording(false)} onDone={() => { setRecording(false); qc.invalidateQueries({ queryKey: ["sales"] }); }} />}
    </div>
  );
}

function RecordSaleModal({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const channels = useQuery({ queryKey: ["channels"], queryFn: () => channelsApi.list() });
  const inventory = useQuery({ queryKey: ["inventory-all"], queryFn: () => inventoryApi.list(0, 500) });
  const [form, setForm] = useState<SaleInput>({ channelId: "", skuId: "", quantity: 1, unitPrice: 0 });
  const [warning, setWarning] = useState("");

  const create = useMutation({
    mutationFn: (body: SaleInput) => salesApi.create(body),
    onSuccess: (sale) => {
      if (sale.warnings.includes("NEGATIVE_STOCK")) {
        setWarning("Sale recorded, but this drove stock negative for the SKU.");
        setTimeout(onDone, 1200);
      } else {
        onDone();
      }
    },
  });

  return (
    <Modal open title="Record a sale" onClose={onClose}>
      <div className="space-y-4">
        {create.isError && <ErrorState message={apiErrorMessage(create.error)} />}
        {warning && <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">{warning}</p>}
        <Select label="Channel" value={form.channelId} onChange={(e) => setForm({ ...form, channelId: e.target.value })}>
          <option value="">Select channel...</option>
          {channels.data?.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </Select>
        <Select
          label="SKU"
          value={form.skuId}
          onChange={(e) => {
            const item = inventory.data?.content.find((i) => i.skuId === e.target.value);
            setForm({ ...form, skuId: e.target.value });
            void item;
          }}
        >
          <option value="">Select SKU...</option>
          {inventory.data?.content.map((i) => (
            <option key={i.skuId} value={i.skuId}>
              {i.productName} — {i.sku} (on hand: {i.quantityOnHand})
            </option>
          ))}
        </Select>
        <div className="grid grid-cols-2 gap-4">
          <Input label="Quantity" type="number" min={1} value={form.quantity} onChange={(e) => setForm({ ...form, quantity: Number(e.target.value) })} />
          <Input label="Unit price" type="number" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: Number(e.target.value) })} />
        </div>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button
            onClick={() => create.mutate(form)}
            disabled={!form.channelId || !form.skuId || create.isPending}
          >
            {create.isPending ? "Recording..." : "Record sale"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
