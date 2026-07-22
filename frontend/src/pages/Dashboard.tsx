import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { analyticsApi } from "@/api/analytics";
import { DateRangePicker, RangeValue } from "@/components/DateRangePicker";
import { Card, formatCurrency, formatNumber } from "@/components/ui";
import { LoadingSpinner, ErrorState, Badge } from "@/components/states";
import {
  ChannelComparisonChart,
  SalesByChannelChart,
  SalesTrendChart,
} from "@/components/charts";
import { apiErrorMessage } from "@/api/client";

function Kpi({ label, value, sub }: { label: string; value: string; sub?: React.ReactNode }) {
  return (
    <Card>
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-800">{value}</p>
      {sub && <p className="mt-1 text-xs">{sub}</p>}
    </Card>
  );
}

export default function Dashboard() {
  const [range, setRange] = useState<RangeValue>({ preset: "LAST_30D" });
  const params = { preset: range.preset, from: range.from, to: range.to };

  const summary = useQuery({
    queryKey: ["summary", params],
    queryFn: () => analyticsApi.summary(params),
  });
  const trend = useQuery({
    queryKey: ["trend", params],
    queryFn: () => analyticsApi.salesTrend({ ...params, granularity: "day" }),
  });
  const byChannel = useQuery({
    queryKey: ["byChannel", params],
    queryFn: () => analyticsApi.salesByChannel(params),
  });
  const comparison = useQuery({
    queryKey: ["comparison", params],
    queryFn: () => analyticsApi.channelComparison(params),
  });
  const top = useQuery({
    queryKey: ["top", params],
    queryFn: () => analyticsApi.topProducts({ ...params, limit: 8 }),
  });

  const s = summary.data;
  const topChannel = byChannel.data && byChannel.data.length > 0 ? byChannel.data[0] : null;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
        <DateRangePicker value={range} onChange={setRange} />
      </div>

      {summary.isLoading ? (
        <LoadingSpinner />
      ) : summary.isError ? (
        <ErrorState message={apiErrorMessage(summary.error)} />
      ) : s ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi label="Revenue" value={formatCurrency(s.revenue)} sub={
            <Badge color={s.revenueChangePct >= 0 ? "green" : "red"}>
              {s.revenueChangePct >= 0 ? "▲" : "▼"} {Math.abs(s.revenueChangePct)}% vs prev
            </Badge>
          } />
          <Kpi label="Units sold" value={formatNumber(s.unitsSold)} />
          <Kpi label="Orders" value={formatNumber(s.orderCount)} />
          <Kpi label="Low-stock SKUs" value={formatNumber(s.lowStockCount)} sub={
            topChannel ? <span className="text-slate-400">Top channel: {topChannel.channelName}</span> : undefined
          } />
        </div>
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <h2 className="mb-4 font-semibold text-slate-700">Sales trend</h2>
          {trend.isLoading ? <LoadingSpinner /> : trend.data && trend.data.length > 0 ? (
            <SalesTrendChart data={trend.data} />
          ) : (
            <p className="py-12 text-center text-sm text-slate-400">No sales in this period</p>
          )}
        </Card>
        <Card>
          <h2 className="mb-4 font-semibold text-slate-700">Revenue by channel</h2>
          {byChannel.isLoading ? <LoadingSpinner /> : byChannel.data ? (
            <SalesByChannelChart data={byChannel.data} />
          ) : null}
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <h2 className="mb-4 font-semibold text-slate-700">Channel comparison (vs previous period)</h2>
          {comparison.isLoading ? <LoadingSpinner /> : comparison.data ? (
            <ChannelComparisonChart data={comparison.data} />
          ) : null}
        </Card>
        <Card>
          <h2 className="mb-4 font-semibold text-slate-700">Top products</h2>
          {top.isLoading ? (
            <LoadingSpinner />
          ) : top.data && top.data.length > 0 ? (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Product</th>
                  <th className="py-2">SKU</th>
                  <th className="py-2 text-right">Units</th>
                  <th className="py-2 text-right">Revenue</th>
                </tr>
              </thead>
              <tbody>
                {top.data.map((p) => (
                  <tr key={p.sku} className="border-b border-slate-50">
                    <td className="py-2 font-medium text-slate-700">{p.productName}</td>
                    <td className="py-2 text-slate-500">{p.sku}</td>
                    <td className="py-2 text-right">{formatNumber(p.units)}</td>
                    <td className="py-2 text-right">{formatCurrency(p.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="py-12 text-center text-sm text-slate-400">No sales in this period</p>
          )}
        </Card>
      </div>
    </div>
  );
}
