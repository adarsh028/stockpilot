import client from "./client";
import { ImportBatch, Page, Product, ProductInput, SkuImage } from "@/types/api";

export interface ProductListParams {
  search?: string;
  categoryId?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const productsApi = {
  list: (params: ProductListParams) =>
    client.get<Page<Product>>("/products", { params }).then((r) => r.data),

  get: (id: string) => client.get<Product>(`/products/${id}`).then((r) => r.data),

  create: (body: ProductInput) => client.post<Product>("/products", body).then((r) => r.data),

  update: (id: string, body: ProductInput) =>
    client.patch<Product>(`/products/${id}`, body).then((r) => r.data),

  remove: (id: string) => client.delete(`/products/${id}`).then((r) => r.data),

  import: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return client
      .post<ImportBatch>("/products/import", form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },
};

export const skuImagesApi = {
  list: (skuId: string) =>
    client.get<SkuImage[]>(`/skus/${skuId}/images`).then((r) => r.data),

  upload: (skuId: string, file: File, primary = false) => {
    const form = new FormData();
    form.append("file", file);
    form.append("primary", String(primary));
    return client
      .post<SkuImage>(`/skus/${skuId}/images`, form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  setPrimary: (skuId: string, imageId: string) =>
    client.patch<SkuImage>(`/skus/${skuId}/images/${imageId}/primary`).then((r) => r.data),

  remove: (skuId: string, imageId: string) =>
    client.delete(`/skus/${skuId}/images/${imageId}`).then((r) => r.data),
};
