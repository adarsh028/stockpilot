import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orgApi } from "@/api/users";
import { Button, Card, Input } from "@/components/ui";
import { ErrorState, LoadingSpinner } from "@/components/states";
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
    <div className="mx-auto max-w-2xl space-y-6">
      <h1 className="text-2xl font-bold text-slate-800">Settings</h1>

      <Card className="space-y-4">
        <h2 className="font-semibold text-slate-700">Organization profile</h2>
        <Input label="Organization name" value={name} onChange={(e) => setName(e.target.value)} disabled={!isOwner} />
        <div className="grid grid-cols-2 gap-4 text-sm text-slate-500">
          <div>
            <span className="block text-xs uppercase text-slate-400">Slug</span>
            {org.data?.slug}
          </div>
          <div>
            <span className="block text-xs uppercase text-slate-400">Plan</span>
            {org.data?.plan}
          </div>
        </div>
        {isOwner && (
          <div className="flex items-center gap-3">
            <Button onClick={() => save.mutate()} disabled={save.isPending}>
              {save.isPending ? "Saving..." : "Save changes"}
            </Button>
            {saved && <span className="text-sm text-emerald-600">Saved ✓</span>}
          </div>
        )}
        {save.isError && <ErrorState message={apiErrorMessage(save.error)} />}
      </Card>

      <Card className="space-y-3">
        <h2 className="font-semibold text-slate-700">Email (Brevo) configuration</h2>
        <p className="text-sm text-slate-500">
          Transactional emails (OTP, welcome, invites, low-stock alerts) are sent via Brevo. The API
          key and sender are configured on the server via environment variables{" "}
          <code className="rounded bg-slate-100 px-1 text-xs">BREVO_API_KEY</code>,{" "}
          <code className="rounded bg-slate-100 px-1 text-xs">BREVO_SENDER_EMAIL</code>,{" "}
          <code className="rounded bg-slate-100 px-1 text-xs">BREVO_SENDER_NAME</code>.
        </p>
        <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-700">
          If no Brevo key is set, the backend runs in dev mode and logs OTP codes / email bodies to
          its console instead of sending — so the app is fully usable without an email account.
        </p>
      </Card>
    </div>
  );
}
