import { ReactNode } from "react";

export function AuthShell({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-brand-50 to-slate-100 p-4">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          <div className="text-3xl">🚀</div>
          <h1 className="mt-2 text-2xl font-bold text-brand-700">StockPilot</h1>
          <p className="text-sm text-slate-500">Multi-channel inventory management</p>
        </div>
        <div className="rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-800">{title}</h2>
          {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          <div className="mt-6">{children}</div>
        </div>
      </div>
    </div>
  );
}
