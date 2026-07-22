import client from "./client";
import { ImportBatch, Page, Sale } from "@/types/api";

export interface SaleInput {
  channelId: string;
  skuId: string;
  quantity: number;
  unitPrice: number;
  saleDate?: string;
  marketplaceOrderId?: string;
}

export interface SaleListParams {
  channelId?: string;
  skuId?: string;
  from?: string;
  to?: string;
  status?: string;
  page?: number;
  size?: number;
}

export const salesApi = {
  list: (params: SaleListParams) =>
    client.get<Page<Sale>>("/sales", { params }).then((r) => r.data),

  create: (body: SaleInput) => client.post<Sale>("/sales", body).then((r) => r.data),

  returnSale: (id: string) => client.post<Sale>(`/sales/${id}/return`).then((r) => r.data),

  remove: (id: string) => client.delete(`/sales/${id}`).then((r) => r.data),

  import: (file: File, channelId?: string) => {
    const form = new FormData();
    form.append("file", file);
    const params = channelId ? { channelId } : {};
    return client
      .post<ImportBatch>("/sales/import", form, {
        headers: { "Content-Type": "multipart/form-data" },
        params,
      })
      .then((r) => r.data);
  },

  batches: (kind?: "SALES" | "PRODUCTS", page = 0, size = 20) =>
    client
      .get<Page<ImportBatch>>("/import-batches", { params: { kind, page, size } })
      .then((r) => r.data),

  errorReportUrl: (batchId: string) => `/import-batches/${batchId}/error-report`,
};
