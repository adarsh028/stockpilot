import client from "./client";
import { ChannelListing, InventoryItem, Page } from "@/types/api";

export interface AdjustStockInput {
  skuId: string;
  delta?: number;
  newQuantity?: number;
  reason?: string;
}

export interface ChannelListingInput {
  skuId: string;
  channelSku?: string;
  allocatedQuantity: number;
  channelPrice?: number;
  status?: string;
}

export interface ChannelListingUpdate {
  channelSku?: string;
  allocatedQuantity?: number;
  channelPrice?: number;
  status?: string;
}

export const inventoryApi = {
  list: (page = 0, size = 20) =>
    client.get<Page<InventoryItem>>("/inventory", { params: { page, size } }).then((r) => r.data),

  lowStock: () => client.get<InventoryItem[]>("/inventory/low-stock").then((r) => r.data),

  adjust: (body: AdjustStockInput) =>
    client.post<InventoryItem>("/inventory/adjust", body).then((r) => r.data),

  listingsForChannel: (channelId: string) =>
    client.get<ChannelListing[]>(`/channels/${channelId}/listings`).then((r) => r.data),

  upsertListing: (channelId: string, body: ChannelListingInput) =>
    client.post<ChannelListing>(`/channels/${channelId}/listings`, body).then((r) => r.data),

  updateListing: (listingId: string, body: ChannelListingUpdate) =>
    client.patch<ChannelListing>(`/listings/${listingId}`, body).then((r) => r.data),
};
