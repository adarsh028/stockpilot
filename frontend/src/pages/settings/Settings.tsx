import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orgApi } from "@/api/users";
import { googleDriveApi } from "@/api/integrations";
import { Button, Card, Input, PageHeader } from "@/components/ui";
import { ErrorState, LoadingSpinner } from "@/components/states";
import { CheckCircleIcon, ChevronRightIcon, ProductsIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";

export default function Settings() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const isOwner = user?.role === "OWNER";
  const org = useQuery({ queryKey: ["org"], queryFn: () => orgApi.get() });
  const [name, setName] = useState("");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (org.data) setName(org.data.name);
  }, [org.data]);

  const save = useMutation({
    mutationFn: () => orgApi.update(name),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["org"] });
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    },
  });

  if (org.isLoading) return <LoadingSpinner />;
  if (org.isError) return <ErrorState message={apiErrorMessage(org.error)} />;

  return (
    <div className="space-y-6">
      <PageHeader title="Settings" subtitle="Manage your organization and integrations" />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-2 xl:items-start">
        <GoogleDriveCard canManage={isOwner || user?.role === "ADMIN"} isOwner={isOwner} />

        <Card className="space-y-4">
          <h2 className="text-sm font-semibold text-slate-800">Organization profile</h2>
          <Input label="Organization name" value={name} onChange={(e) => setName(e.target.value)} disabled={!isOwner} />
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="min-w-0 rounded-lg border border-slate-200 bg-slate-50/60 px-3 py-2.5">
              <span className="block text-[11px] font-semibold uppercase tracking-wider text-slate-400">Slug</span>
              <span className="block truncate text-sm text-slate-700">{org.data?.slug}</span>
            </div>
            <div className="min-w-0 rounded-lg border border-slate-200 bg-slate-50/60 px-3 py-2.5">
              <span className="block text-[11px] font-semibold uppercase tracking-wider text-slate-400">Plan</span>
              <span className="block truncate text-sm text-slate-700">{org.data?.plan}</span>
            </div>
          </div>
          {isOwner && (
            <div className="flex flex-wrap items-center gap-3">
              <Button onClick={() => save.mutate()} disabled={save.isPending}>
                {save.isPending ? "Saving..." : "Save changes"}
              </Button>
              {saved && (
                <span className="inline-flex items-center gap-1.5 text-sm font-medium text-emerald-600">
                  <CheckCircleIcon className="text-base" /> Saved
                </span>
              )}
            </div>
          )}
          {save.isError && <ErrorState message={apiErrorMessage(save.error)} />}
        </Card>

        {(isOwner || user?.role === "ADMIN") && (
          <Card>
            <Link
              to="/settings/categories"
              className="flex items-center justify-between gap-3 text-slate-800 transition hover:text-brand-600"
            >
              <span className="flex items-center gap-3">
                <ProductsIcon className="text-lg text-slate-400" />
                <span>
                  <span className="block text-sm font-semibold">Product categories</span>
                  <span className="block text-sm text-slate-500">
                    Manage the categories available when adding a product
                  </span>
                </span>
              </span>
              <ChevronRightIcon className="shrink-0 text-lg text-slate-400" />
            </Link>
          </Card>
        )}
      </div>
    </div>
  );
}

/**
 * Google Drive connection card. Product images are stored in the org's own Drive, so an
 * owner/admin connects it here via Google's OAuth consent screen. Connecting is a full
 * browser redirect; on return the backend appends ?drive=connected|error to this page.
 */
function GoogleDriveCard({ canManage, isOwner }: { canManage: boolean; isOwner: boolean }) {
  const qc = useQueryClient();
  const [banner, setBanner] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [error, setError] = useState("");

  const status = useQuery({ queryKey: ["drive-status"], queryFn: () => googleDriveApi.status() });

  // Surface the result of a just-completed connect attempt, then clean the URL.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const result = params.get("drive");
    if (!result) return;
    setBanner(
      result === "connected"
        ? { kind: "ok", text: "Google Drive connected successfully." }
        : { kind: "err", text: "Could not connect Google Drive. Please try again." },
    );
    qc.invalidateQueries({ queryKey: ["drive-status"] });
    params.delete("drive");
    const query = params.toString();
    window.history.replaceState({}, "", window.location.pathname + (query ? `?${query}` : ""));
  }, [qc]);

  const connect = useMutation({
    mutationFn: () => googleDriveApi.authorizeUrl(),
    onSuccess: (url) => {
      window.location.href = url;
    },
    onError: (e) => setError(apiErrorMessage(e)),
  });

  const disconnect = useMutation({
    mutationFn: () => googleDriveApi.disconnect(),
    onSuccess: () => {
      setBanner({ kind: "ok", text: "Google Drive disconnected." });
      qc.invalidateQueries({ queryKey: ["drive-status"] });
    },
    onError: (e) => setError(apiErrorMessage(e)),
  });

  const data = status.data;
  const connected = data?.connected ?? false;

  return (
    <Card className="space-y-3">
      <h2 className="text-sm font-semibold text-slate-800">Google Drive storage</h2>
      <p className="text-sm text-slate-500">
        Product images are stored in your organization's own Google Drive. Connect an account below;
        images uploaded to variants are saved there and streamed back when you open a product.
      </p>

      {banner && (
        <p
          className={`rounded-lg border px-3 py-2 text-sm ${
            banner.kind === "ok"
              ? "border-emerald-200 bg-emerald-50 text-emerald-700"
              : "border-red-200 bg-red-50 text-red-700"
          }`}
        >
          {banner.text}
        </p>
      )}

      {status.isLoading ? (
        <p className="text-sm text-slate-400">Checking connection…</p>
      ) : !data?.configured ? (
        <p className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">
          Google Drive is not configured on the server. Set{" "}
          <code className="rounded bg-white/70 px-1 font-mono text-xs">GOOGLE_CLIENT_ID</code> and{" "}
          <code className="rounded bg-white/70 px-1 font-mono text-xs">GOOGLE_CLIENT_SECRET</code>.
        </p>
      ) : connected ? (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 bg-slate-50/60 px-3 py-2.5">
          <div className="min-w-0 text-sm">
            <span className="inline-flex items-center gap-2 font-medium text-emerald-700">
              <span className="h-2 w-2 shrink-0 rounded-full bg-emerald-500" /> Connected
            </span>
            {data?.email && <span className="ml-2 break-all text-slate-500">{data.email}</span>}
          </div>
          {isOwner && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => disconnect.mutate()}
              disabled={disconnect.isPending}
            >
              {disconnect.isPending ? "Disconnecting…" : "Disconnect"}
            </Button>
          )}
        </div>
      ) : (
        <div className="flex flex-wrap items-center gap-3">
          {canManage ? (
            <Button onClick={() => connect.mutate()} disabled={connect.isPending}>
              {connect.isPending ? "Redirecting…" : "Connect Google Drive"}
            </Button>
          ) : (
            <p className="text-sm text-slate-500">
              Not connected. Ask an owner or admin to connect Google Drive.
            </p>
          )}
        </div>
      )}

      {error && <ErrorState message={error} />}
    </Card>
  );
}
