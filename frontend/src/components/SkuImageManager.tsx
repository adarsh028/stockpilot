import { ChangeEvent, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { skuImagesApi } from "@/api/products";
import { apiErrorMessage } from "@/api/client";
import { SkuImage } from "@/types/api";
import { CloseIcon, PlusIcon, StarIcon } from "@/components/icons";

/**
 * Manages the image gallery for a single saved SKU (variant): upload multiple
 * images, mark one primary, and delete. Images are persisted immediately via their
 * own endpoints — independent of the product save.
 */
export default function SkuImageManager({ skuId }: { skuId: string }) {
  const qc = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState("");

  const key = ["sku-images", skuId];
  const images = useQuery({
    queryKey: key,
    queryFn: () => skuImagesApi.list(skuId),
  });

  const invalidate = () => qc.invalidateQueries({ queryKey: key });

  const upload = useMutation({
    mutationFn: (file: File) => skuImagesApi.upload(skuId, file, (images.data?.length ?? 0) === 0),
    onSuccess: invalidate,
    onError: (e) => setError(apiErrorMessage(e)),
  });

  const setPrimary = useMutation({
    mutationFn: (imageId: string) => skuImagesApi.setPrimary(skuId, imageId),
    onSuccess: invalidate,
    onError: (e) => setError(apiErrorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (imageId: string) => skuImagesApi.remove(skuId, imageId),
    onSuccess: invalidate,
    onError: (e) => setError(apiErrorMessage(e)),
  });

  function onFiles(e: ChangeEvent<HTMLInputElement>) {
    setError("");
    const files = Array.from(e.target.files ?? []);
    files.forEach((f) => upload.mutate(f));
    if (fileRef.current) fileRef.current.value = "";
  }

  const list: SkuImage[] = images.data ?? [];

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Images</span>
        <button
          type="button"
          onClick={() => fileRef.current?.click()}
          disabled={upload.isPending}
          className="inline-flex items-center gap-1 text-xs font-medium text-brand-600 transition hover:text-brand-700 disabled:opacity-50"
        >
          <PlusIcon className="text-sm" /> {upload.isPending ? "Uploading…" : "Add images"}
        </button>
        <input
          ref={fileRef}
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          multiple
          className="hidden"
          onChange={onFiles}
        />
      </div>

      {error && <p className="text-xs text-red-600">{error}</p>}

      {list.length === 0 ? (
        <p className="text-xs text-slate-400">No images yet.</p>
      ) : (
        <div className="flex flex-wrap gap-3">
          {list.map((img) => (
            <div
              key={img.id}
              className={`group relative h-20 w-20 overflow-hidden rounded-lg border ${
                img.primary ? "border-brand-500 ring-2 ring-brand-200" : "border-slate-200"
              }`}
            >
              <img src={img.url} alt="variant" className="h-full w-full object-cover" />

              {img.primary && (
                <span className="absolute left-0 top-0 rounded-br-md bg-brand-600 px-1.5 py-0.5 text-[10px] font-semibold text-white">
                  Primary
                </span>
              )}

              <div className="absolute inset-x-0 bottom-0 flex items-center justify-between bg-gradient-to-t from-black/70 to-transparent px-1.5 py-1 opacity-0 transition-opacity group-hover:opacity-100">
                {!img.primary && (
                  <button
                    type="button"
                    title="Set as primary"
                    onClick={() => setPrimary.mutate(img.id)}
                    className="inline-flex items-center gap-0.5 text-[10px] text-white hover:text-brand-200"
                  >
                    <StarIcon className="text-xs" /> Primary
                  </button>
                )}
                <button
                  type="button"
                  title="Delete image"
                  onClick={() => remove.mutate(img.id)}
                  className="ml-auto text-white hover:text-red-300"
                >
                  <CloseIcon className="text-sm" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
