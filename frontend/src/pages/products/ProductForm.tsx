import { FormEvent, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { productsApi } from "@/api/products";
import { Button, Card, Input, PageHeader } from "@/components/ui";
import { ErrorState, LoadingSpinner } from "@/components/states";
import { PlusIcon, TrashIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { SkuInput } from "@/types/api";
import SkuImageManager from "@/components/SkuImageManager";

const emptySku: SkuInput = { sku: "", costPrice: 0, sellingPrice: 0, quantityOnHand: 0, reorderLevel: 0 };

export default function ProductForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const editing = !!id;

  const [name, setName] = useState("");
  const [category, setCategory] = useState("");
  const [brandName, setBrandName] = useState("");
  const [description, setDescription] = useState("");
  const [skus, setSkus] = useState<SkuInput[]>([{ ...emptySku }]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const existing = useQuery({
    queryKey: ["product", id],
    queryFn: () => productsApi.get(id!),
    enabled: editing,
  });

  useEffect(() => {
    if (existing.data) {
      const p = existing.data;
      setName(p.name);
      setCategory(p.category ?? "");
      setBrandName(p.brandName ?? "");
      setDescription(p.description ?? "");
      setSkus(
        p.skus.map((s) => ({
          id: s.id,
          sku: s.sku,
          attributes: s.attributes,
          costPrice: s.costPrice,
          sellingPrice: s.sellingPrice,
          quantityOnHand: s.quantityOnHand,
          reorderLevel: s.reorderLevel,
        }))
      );
    }
  }, [existing.data]);

  function updateSku(i: number, key: keyof SkuInput, value: string) {
    const next = [...skus];
    const numericKeys: (keyof SkuInput)[] = ["costPrice", "sellingPrice", "quantityOnHand", "reorderLevel"];
    const parsed: string | number = numericKeys.includes(key) ? Number(value) : value;
    next[i] = { ...next[i], [key]: parsed };
    setSkus(next);
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSaving(true);
    try {
      const body = { name, category, brandName, description, skus };
      if (editing) await productsApi.update(id!, body);
      else await productsApi.create(body);
      navigate("/products");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  if (editing && existing.isLoading) return <LoadingSpinner />;

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title={editing ? "Edit product" : "New product"}
        subtitle="Product details and their sellable variants"
      />
      <form onSubmit={onSubmit} className="space-y-5">
        {error && <ErrorState message={error} />}
        <Card className="space-y-4">
          <h2 className="text-sm font-semibold text-slate-800">Details</h2>
          <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} required />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Input label="Category" value={category} onChange={(e) => setCategory(e.target.value)} />
            <Input label="Brand" value={brandName} onChange={(e) => setBrandName(e.target.value)} />
          </div>
          <Input label="Description" value={description} onChange={(e) => setDescription(e.target.value)} />
        </Card>

        <Card className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-slate-800">SKUs / Variants</h2>
            <Button type="button" variant="secondary" size="sm" onClick={() => setSkus([...skus, { ...emptySku }])}>
              <PlusIcon className="text-base" /> Add SKU
            </Button>
          </div>
          {skus.map((s, i) => (
            <div key={i} className="rounded-xl border border-slate-200 bg-slate-50/40 p-4">
              <div className="mb-3 flex items-center justify-between">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                  Variant {i + 1}
                </span>
                {skus.length > 1 && (
                  <button
                    type="button"
                    onClick={() => setSkus(skus.filter((_, idx) => idx !== i))}
                    title="Remove variant"
                    className="rounded-md p-1.5 text-slate-400 transition hover:bg-red-50 hover:text-red-600"
                  >
                    <TrashIcon className="text-base" />
                  </button>
                )}
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-6">
                <div className="sm:col-span-2">
                  <Input label="SKU code" value={s.sku} onChange={(e) => updateSku(i, "sku", e.target.value)} required />
                </div>
                <Input label="Cost" type="number" value={s.costPrice ?? 0} onChange={(e) => updateSku(i, "costPrice", e.target.value)} />
                <Input label="Price" type="number" value={s.sellingPrice ?? 0} onChange={(e) => updateSku(i, "sellingPrice", e.target.value)} />
                <Input label="Qty" type="number" value={s.quantityOnHand ?? 0} onChange={(e) => updateSku(i, "quantityOnHand", e.target.value)} />
                <Input label="Reorder" type="number" value={s.reorderLevel ?? 0} onChange={(e) => updateSku(i, "reorderLevel", e.target.value)} />
              </div>
              <div className="mt-4 border-t border-slate-200 pt-4">
                {s.id ? (
                  <SkuImageManager skuId={s.id} />
                ) : (
                  <p className="text-xs text-slate-400">
                    Save the product first, then reopen it to add images for this variant.
                  </p>
                )}
              </div>
            </div>
          ))}
        </Card>

        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={() => navigate("/products")}>
            Cancel
          </Button>
          <Button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Save product"}
          </Button>
        </div>
      </form>
    </div>
  );
}
