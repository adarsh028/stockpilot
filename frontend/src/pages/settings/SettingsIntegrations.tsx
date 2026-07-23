import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { googleDriveApi } from "@/api/integrations";
import { Button, Card } from "@/components/ui";
import { ErrorState } from "@/components/states";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";

/**
 * Google Drive connection. Product images are stored in the org's own Drive, so an
 * owner/admin connects it here via Google's OAuth consent screen. Connecting is a full
 * browser redirect; on return the backend appends ?drive=connected|error to this page.
 */
export default function SettingsIntegrations() {
  const { user } = useAuth();
  const isOwner = user?.role === "OWNER";
  const canManage = isOwner || user?.role === "ADMIN";

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
    <Card className="max-w-2xl space-y-3">
      <div>
        <h2 className="text-sm font-semibold text-slate-800">Google Drive storage</h2>
        <p className="mt-0.5 text-sm text-slate-500">
          Product images are stored in your organization's own Google Drive. Connect an account below;
          images uploaded to variants are saved there and streamed back when you open a product.
        </p>
      </div>

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
