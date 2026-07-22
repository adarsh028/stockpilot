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
import { ChannelComparison, SalesByChannel, SalesTrendPoint } from "@/types/api";
import { formatCurrency } from "./ui";

// Brand-neutral categorical palette (accessible in light mode).
export const PALETTE = [
  "#4f46e5",
  "#0ea5e9",
  "#10b981",
  "#f59e0b",
  "#ef4444",
  "#8b5cf6",
  "#ec4899",
];

function formatBucket(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString("en-IN", { month: "short", day: "numeric" });
}

export function SalesTrendChart({ data }: { data: SalesTrendPoint[] }) {
  const rows = data.map((d) => ({ ...d, label: formatBucket(d.bucket) }));
  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={rows} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
        <defs>
          <linearGradient id="rev" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#4f46e5" stopOpacity={0.35} />
            <stop offset="95%" stopColor="#4f46e5" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
        <XAxis dataKey="label" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} width={70} tickFormatter={(v) => `₹${Math.round(v / 1000)}k`} />
        <Tooltip formatter={(v: number) => formatCurrency(v)} />
        <Area type="monotone" dataKey="revenue" stroke="#4f46e5" fill="url(#rev)" name="Revenue" />
      </AreaChart>
    </ResponsiveContainer>
  );
}

export function SalesByChannelChart({ data }: { data: SalesByChannel[] }) {
  const rows = data.filter((d) => d.revenue > 0);
  if (rows.length === 0) {
    return <p className="py-12 text-center text-sm text-slate-400">No sales in this period</p>;
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
          outerRadius={100}
          label={(e) => e.channelName}
        >
          {rows.map((_, i) => (
            <Cell key={i} fill={PALETTE[i % PALETTE.length]} />
          ))}
        </Pie>
        <Tooltip formatter={(v: number) => formatCurrency(v)} />
      </PieChart>
    </ResponsiveContainer>
  );
}

export function ChannelComparisonChart({ data }: { data: ChannelComparison[] }) {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
        <XAxis dataKey="channelName" tick={{ fontSize: 12 }} />
        <YAxis tick={{ fontSize: 12 }} width={70} tickFormatter={(v) => `₹${Math.round(v / 1000)}k`} />
        <Tooltip formatter={(v: number) => formatCurrency(v)} />
        <Legend />
        <Bar dataKey="revenuePrevious" name="Previous" fill="#cbd5e1" radius={[4, 4, 0, 0]} />
        <Bar dataKey="revenue" name="Current" fill="#4f46e5" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
