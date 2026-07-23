import { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { ChevronDownIcon, ChevronLeftIcon, ChevronRightIcon, CloseIcon } from "./icons";

export function Button({
  children,
  variant = "primary",
  size = "md",
  className = "",
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "danger" | "ghost";
  size?: "sm" | "md";
}) {
  const base =
    "inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40 focus-visible:ring-offset-1 disabled:cursor-not-allowed disabled:opacity-50 active:scale-[0.98]";
  const sizes: Record<string, string> = {
    sm: "px-3 py-1.5 text-xs",
    md: "px-4 py-2.5 text-sm",
  };
  const variants: Record<string, string> = {
    primary: "bg-brand-600 text-white shadow-xs hover:bg-brand-700",
    secondary:
      "border border-slate-300 bg-white text-slate-700 shadow-xs hover:border-slate-400 hover:bg-slate-50",
    danger: "bg-red-600 text-white shadow-xs hover:bg-red-700",
    ghost: "text-slate-600 hover:bg-slate-100",
  };
  return (
    <button className={`${base} ${sizes[size]} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Input({
  label,
  error,
  hint,
  className = "",
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label?: string; error?: string; hint?: string }) {
  return (
    <label className="block">
      {label && <span className="mb-1.5 block text-sm font-medium text-slate-700">{label}</span>}
      <input
        className={`w-full rounded-lg border bg-white px-3 py-2.5 text-sm text-slate-900 shadow-xs outline-none transition placeholder:text-slate-400 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500 ${
          error
            ? "border-red-400 focus:border-red-500 focus:ring-2 focus:ring-red-500/20"
            : "border-slate-300 focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20"
        } ${className}`}
        {...props}
      />
      {error ? (
        <span className="mt-1.5 block text-xs text-red-600">{error}</span>
      ) : hint ? (
        <span className="mt-1.5 block text-xs text-slate-400">{hint}</span>
      ) : null}
    </label>
  );
}

export function Select({
  label,
  children,
  className = "",
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & { label?: string }) {
  return (
    <label className="block">
      {label && <span className="mb-1.5 block text-sm font-medium text-slate-700">{label}</span>}
      <div className="relative">
        <select
          className={`w-full appearance-none rounded-lg border border-slate-300 bg-white px-3 py-2.5 pr-9 text-sm text-slate-900 shadow-xs outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/20 disabled:cursor-not-allowed disabled:bg-slate-50 ${className}`}
          {...props}
        >
          {children}
        </select>
        <ChevronDownIcon className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-base text-slate-400" />
      </div>
    </label>
  );
}

export function Card({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-slate-200 bg-white p-5 shadow-card ${className}`}>
      {children}
    </div>
  );
}

export function PageHeader({
  title,
  subtitle,
  children,
}: {
  title: ReactNode;
  subtitle?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-xl font-semibold text-slate-900 sm:text-[1.6rem] sm:leading-8">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  );
}

export function Modal({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm animate-fade-in"
      onClick={onClose}
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-modal animate-scale-in"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-slate-100 px-6 py-4">
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <button
            onClick={onClose}
            aria-label="Close"
            className="-mr-1.5 rounded-md p-1.5 text-lg text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          >
            <CloseIcon />
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
      </div>
    </div>
  );
}

export function Pagination({
  page,
  totalPages,
  first,
  last,
  onPrev,
  onNext,
}: {
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  onPrev: () => void;
  onNext: () => void;
}) {
  return (
    <div className="mt-4 flex items-center justify-between border-t border-slate-100 pt-4 text-sm">
      <span className="text-slate-500">
        Page <span className="font-medium text-slate-700">{page + 1}</span> of {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variant="secondary" size="sm" disabled={first} onClick={onPrev}>
          <ChevronLeftIcon className="text-base" /> Previous
        </Button>
        <Button variant="secondary" size="sm" disabled={last} onClick={onNext}>
          Next <ChevronRightIcon className="text-base" />
        </Button>
      </div>
    </div>
  );
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0,
  }).format(value);
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat("en-IN").format(value);
}
