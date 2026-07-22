import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { salesApi, SaleInput } from "@/api/sales";
import { channelsApi } from "@/api/channels";
import { inventoryApi } from "@/api/inventory";
import { Button, Card, Input, Modal, Select, formatCurrency } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
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
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-800">Sales</h1>
        <div className="flex gap-2">
          <Link to="/sales/import">
            <Button variant="secondary">Import</Button>
          </Link>
          <Link to="/sales/batches">
            <Button variant="secondary">Import history</Button>
          </Link>
          <Button onClick={() => setRecording(true)}>+ Record sale</Button>
        </div>
      </div>

      <Card>
        <div className="mb-4 w-56">
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
          <EmptyState title="No sales recorded" hint="Record a manual sale or import a spreadsheet." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-left text-slate-500">
                    <th className="py-2">Date</th>
                    <th className="py-2">Product / SKU</th>
                    <th className="py-2">Channel</th>
                    <th className="py-2 text-right">Qty</th>
                    <th className="py-2 text-right">Total</th>
                    <th className="py-2">Status</th>
                    <th className="py-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sales.data?.content.map((s) => (
                    <tr key={s.id} className="border-b border-slate-50 hover:bg-slate-50">
                      <td className="py-2 text-slate-500">{new Date(s.saleDate).toLocaleDateString()}</td>
                      <td className="py-2">
                        <span className="font-medium text-slate-700">{s.productName}</span>
                        <span className="ml-1 text-slate-400">({s.sku})</span>
                      </td>
                      <td className="py-2 text-slate-500">{s.channelName}</td>
                      <td className="py-2 text-right">{s.quantity}</td>
                      <td className="py-2 text-right">{formatCurrency(s.totalAmount)}</td>
                      <td className="py-2">
                        <Badge color={s.status === "COMPLETED" ? "green" : "amber"}>{s.status}</Badge>
                      </td>
                      <td className="py-2 text-right">
                        {s.status === "COMPLETED" && (
                          <button onClick={() => returnMut.mutate(s.id)} className="mr-3 text-amber-600 hover:underline">
                            Return
                          </button>
                        )}
                        <button
                          onClick={() => confirm("Delete this sale?") && deleteMut.mutate(s.id)}
                          className="text-red-600 hover:underline"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {sales.data && sales.data.totalPages > 1 && (
              <div className="mt-4 flex items-center justify-between text-sm">
                <span className="text-slate-500">Page {sales.data.page + 1} of {sales.data.totalPages}</span>
                <div className="flex gap-2">
                  <Button variant="secondary" disabled={sales.data.first} onClick={() => setPage((p) => p - 1)}>
                    Previous
                  </Button>
                  <Button variant="secondary" disabled={sales.data.last} onClick={() => setPage((p) => p + 1)}>
                    Next
                  </Button>
                </div>
              </div>
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
        {warning && <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-700">{warning}</p>}
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
