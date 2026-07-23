import { ReactNode } from "react";
import { Logo } from "@/components/icons";

export function AuthShell({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-slate-50 p-4">
      {/* Subtle branded ambience */}
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-brand-50/60 to-slate-50" />
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.4]"
        style={{
          backgroundImage:
            "radial-gradient(circle at 1px 1px, rgb(148 163 184 / 0.18) 1px, transparent 0)",
          backgroundSize: "22px 22px",
        }}
      />

      <div className="relative w-full max-w-md">
        <div className="mb-6 flex flex-col items-center text-center">
          <Logo className="h-12 w-12" />
          <h1 className="mt-3 text-2xl font-bold tracking-tight text-slate-900">StockPilot</h1>
          <p className="text-sm text-slate-500">Multi-channel inventory management</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-card">
          <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
          {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
        <p className="mt-6 text-center text-xs text-slate-400">© StockPilot · Secure inventory platform</p>
      </div>
    </div>
  );
}
