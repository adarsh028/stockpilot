import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { analyticsApi } from "@/api/analytics";
import { DateRangePicker, RangeValue } from "@/components/DateRangePicker";
import { Card, PageHeader, formatCurrency, formatNumber } from "@/components/ui";
import { LoadingSpinner, ErrorState, Badge } from "@/components/states";
import {
  ChannelComparisonChart,
  SalesByChannelChart,
  SalesTrendChart,
} from "@/components/charts";
import { apiErrorMessage } from "@/api/client";

function Kpi({ label, value, sub }: { label: string; value: string; sub?: React.ReactNode }) {
  return (
    <Card className="transition-shadow hover:shadow-card-hover">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-[1.75rem] font-semibold leading-none tracking-tight text-slate-900 tabular-nums">
        {value}
      </p>
      {sub && <p className="mt-3 text-xs">{sub}</p>}
    </Card>
  );
}

function SectionCard({ title, children, className = "" }: { title: string; children: React.ReactNode; className?: string }) {
  return (
    <Card className={className}>
      <h2 className="mb-4 text-sm font-semibold text-slate-800">{title}</h2>
      {children}
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
      <PageHeader title="Dashboard" subtitle="Performance across all your sales channels">
        <DateRangePicker value={range} onChange={setRange} />
      </PageHeader>

      {summary.isLoading ? (
        <LoadingSpinner />
      ) : summary.isError ? (
        <ErrorState message={apiErrorMessage(summary.error)} />
      ) : s ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Kpi
            label="Revenue"
            value={formatCurrency(s.revenue)}
            sub={
              <Badge color={s.revenueChangePct >= 0 ? "green" : "red"}>
                {s.revenueChangePct >= 0 ? "▲" : "▼"} {Math.abs(s.revenueChangePct)}% vs prev
              </Badge>
            }
          />
          <Kpi label="Units sold" value={formatNumber(s.unitsSold)} />
          <Kpi label="Orders" value={formatNumber(s.orderCount)} />
          <Kpi
            label="Low-stock SKUs"
            value={formatNumber(s.lowStockCount)}
            sub={
              topChannel ? (
                <span className="text-slate-400">Top channel: {topChannel.channelName}</span>
              ) : undefined
            }
          />
        </div>
      ) : null}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <SectionCard title="Sales trend" className="lg:col-span-2">
          {trend.isLoading ? (
            <LoadingSpinner />
          ) : trend.data && trend.data.length > 0 ? (
            <SalesTrendChart data={trend.data} />
          ) : (
            <p className="py-16 text-center text-sm text-slate-400">No sales in this period</p>
          )}
        </SectionCard>
        <SectionCard title="Revenue by channel">
          {byChannel.isLoading ? (
            <LoadingSpinner />
          ) : byChannel.data ? (
            <SalesByChannelChart data={byChannel.data} />
          ) : null}
        </SectionCard>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <SectionCard title="Channel comparison (vs previous period)">
          {comparison.isLoading ? (
            <LoadingSpinner />
          ) : comparison.data ? (
            <ChannelComparisonChart data={comparison.data} />
          ) : null}
        </SectionCard>
        <SectionCard title="Top products">
          {top.isLoading ? (
            <LoadingSpinner />
          ) : top.data && top.data.length > 0 ? (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th className="text-right">Units</th>
                  <th className="text-right">Revenue</th>
                </tr>
              </thead>
              <tbody>
                {top.data.map((p) => (
                  <tr key={p.sku}>
                    <td className="font-medium text-slate-800">{p.productName}</td>
                    <td className="font-mono text-xs text-slate-500">{p.sku}</td>
                    <td className="text-right tabular-nums">{formatNumber(p.units)}</td>
                    <td className="text-right font-medium tabular-nums text-slate-800">
                      {formatCurrency(p.revenue)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="py-16 text-center text-sm text-slate-400">No sales in this period</p>
          )}
        </SectionCard>
      </div>
    </div>
  );
}
