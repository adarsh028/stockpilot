import client from "./client";
import {
  AnalyticsSummary,
  ChannelComparison,
  SalesByChannel,
  SalesTrendPoint,
  TopProduct,
} from "@/types/api";

export interface RangeParams {
  preset?: string;
  from?: string;
  to?: string;
}

export const analyticsApi = {
  summary: (params: RangeParams) =>
    client.get<AnalyticsSummary>("/analytics/summary", { params }).then((r) => r.data),

  salesByChannel: (params: RangeParams) =>
    client.get<SalesByChannel[]>("/analytics/sales-by-channel", { params }).then((r) => r.data),

  salesTrend: (params: RangeParams & { granularity?: string }) =>
    client.get<SalesTrendPoint[]>("/analytics/sales-trend", { params }).then((r) => r.data),

  topProducts: (params: RangeParams & { limit?: number }) =>
    client.get<TopProduct[]>("/analytics/top-products", { params }).then((r) => r.data),

  channelComparison: (params: RangeParams) =>
    client.get<ChannelComparison[]>("/analytics/channel-comparison", { params }).then((r) => r.data),
};
