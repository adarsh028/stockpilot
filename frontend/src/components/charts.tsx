import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ReactNode } from "react";
import { ChannelComparison, SalesByChannel, SalesTrendPoint } from "@/types/api";
import { formatCurrency } from "./ui";

// Restrained, professional categorical palette anchored on the brand blue.
export const PALETTE = [
  "#2450e0",
  "#0ea5e9",
  "#10b981",
  "#f59e0b",
  "#8b5cf6",
  "#ec4899",
  "#64748b",
];

const AXIS_TICK = { fontSize: 12, fill: "#94a3b8" };
const GRID_STROKE = "#eef2f6";

function formatBucket(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

/** Shared premium tooltip surface. */
function ChartTooltip({
  active,
  payload,
  label,
  rows,
}: {
  active?: boolean;
  payload?: any[];
  label?: string;
  rows?: (p: any[]) => ReactNode;
}) {
  if (!active || !payload || payload.length === 0) return null;
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-xs shadow-dropdown">
      {label && <p className="mb-1 font-medium text-slate-700">{label}</p>}
      {rows ? (
        rows(payload)
      ) : (
        <div className="space-y-1">
          {payload.map((p, i) => (
            <div key={i} className="flex items-center gap-2 text-slate-600">
              <span className="h-2 w-2 rounded-full" style={{ background: p.color || p.fill }} />
              <span className="text-slate-500">{p.name}</span>
              <span className="ml-auto font-semibold text-slate-800">{formatCurrency(p.value)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export function SalesTrendChart({ data }: { data: SalesTrendPoint[] }) {
  const rows = data.map((d) => ({ ...d, label: formatBucket(d.bucket) }));
  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={rows} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#2450e0" stopOpacity={0.28} />
            <stop offset="95%" stopColor="#2450e0" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="0" stroke={GRID_STROKE} vertical={false} />
        <XAxis dataKey="label" tick={AXIS_TICK} tickLine={false} axisLine={false} dy={6} />
        <YAxis
          tick={AXIS_TICK}
          tickLine={false}
          axisLine={false}
          width={64}
          tickFormatter={(v) => `₹${Math.round(v / 1000)}k`}
        />
        <Tooltip cursor={{ stroke: "#cbd5e1", strokeDasharray: "4 4" }} content={<ChartTooltip />} />
        <Area
          type="monotone"
          dataKey="revenue"
          stroke="#2450e0"
          strokeWidth={2}
          fill="url(#rev)"
          name="Revenue"
          dot={false}
          activeDot={{ r: 4, strokeWidth: 2, stroke: "#fff" }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}

export function SalesByChannelChart({ data }: { data: SalesByChannel[] }) {
  const rows = data.filter((d) => d.revenue > 0);
  if (rows.length === 0) {
    return <p className="py-16 text-center text-sm text-slate-400">No sales in this period</p>;
  }
  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie
          data={rows}
          dataKey="revenue"
          nameKey="channelName"
          cx="50%"
          cy="50%"
          innerRadius={58}
          outerRadius={100}
          paddingAngle={2}
          stroke="#fff"
          strokeWidth={2}
        >
          {rows.map((_, i) => (
            <Cell key={i} fill={PALETTE[i % PALETTE.length]} />
          ))}
        </Pie>
        <Legend
          iconType="circle"
          wrapperStyle={{ fontSize: 12, color: "#64748b" }}
        />
        <Tooltip content={<ChartTooltip />} />
      </PieChart>
    </ResponsiveContainer>
  );
}

export function ChannelComparisonChart({ data }: { data: ChannelComparison[] }) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 10, right: 8, left: 0, bottom: 0 }} barGap={4}>
        <CartesianGrid strokeDasharray="0" stroke={GRID_STROKE} vertical={false} />
        <XAxis dataKey="channelName" tick={AXIS_TICK} tickLine={false} axisLine={false} dy={6} />
        <YAxis
          tick={AXIS_TICK}
          tickLine={false}
          axisLine={false}
          width={64}
          tickFormatter={(v) => `₹${Math.round(v / 1000)}k`}
        />
        <Tooltip cursor={{ fill: "#f1f5f9" }} content={<ChartTooltip />} />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12, color: "#64748b" }} />
        <Bar dataKey="revenuePrevious" name="Previous" fill="#cbd5e1" radius={[4, 4, 0, 0]} maxBarSize={40} />
        <Bar dataKey="revenue" name="Current" fill="#2450e0" radius={[4, 4, 0, 0]} maxBarSize={40} />
      </BarChart>
    </ResponsiveContainer>
  );
}
