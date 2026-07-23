import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orgApi } from "@/api/users";
import { Button, Card, Input } from "@/components/ui";
import { ErrorState, LoadingSpinner } from "@/components/states";
import { CheckCircleIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";

export default function SettingsGeneral() {
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
    <Card className="max-w-2xl space-y-4">
      <div>
        <h2 className="text-sm font-semibold text-slate-800">Organization profile</h2>
        <p className="mt-0.5 text-sm text-slate-500">Your workspace name and plan details.</p>
      </div>
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
  );
}
