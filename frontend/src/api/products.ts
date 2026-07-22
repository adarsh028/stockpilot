import client from "./client";
import { ImportBatch, Page, Product, ProductInput } from "@/types/api";

export interface ProductListParams {
  search?: string;
  category?: string;
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
