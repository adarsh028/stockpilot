import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { channelsApi, ChannelInput } from "@/api/channels";
import { Button, Card, Input, Modal, PageHeader, Select } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { ChannelsIcon, PlusIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { Channel } from "@/types/api";
import { useAuth } from "@/context/AuthContext";

const TYPES = ["MARKETPLACE", "OWN_WEBSITE", "OFFLINE", "OTHER"];

export default function ChannelList() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const canEdit = user?.role === "OWNER" || user?.role === "ADMIN";
  const [editing, setEditing] = useState<Channel | null>(null);
  const [creating, setCreating] = useState(false);

  const query = useQuery({ queryKey: ["channels"], queryFn: () => channelsApi.list() });

  const save = useMutation({
    mutationFn: (v: { id?: string; body: ChannelInput }) =>
      v.id ? channelsApi.update(v.id, v.body) : channelsApi.create(v.body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["channels"] });
      setEditing(null);
      setCreating(false);
    },
  });

  const deactivate = useMutation({
    mutationFn: (id: string) => channelsApi.deactivate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["channels"] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Channels" subtitle="Marketplaces and storefronts you sell through">
        {canEdit && (
          <Button onClick={() => setCreating(true)}>
            <PlusIcon className="text-base" /> New channel
          </Button>
        )}
      </PageHeader>

      <Card>
        {query.isLoading ? (
          <LoadingSpinner />
        ) : query.isError ? (
          <ErrorState message={apiErrorMessage(query.error)} />
        ) : query.data && query.data.length === 0 ? (
          <EmptyState icon={<ChannelsIcon />} title="No channels" />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Code</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {query.data?.map((c) => (
                  <tr key={c.id}>
                    <td className="font-medium text-slate-800">{c.name}</td>
                    <td className="font-mono text-xs text-slate-500">{c.code}</td>
                    <td>{c.type.replace("_", " ")}</td>
                    <td>
                      <Badge color={c.isActive ? "green" : "slate"}>{c.isActive ? "Active" : "Inactive"}</Badge>
                    </td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Link
                          to={`/channels/${c.id}/listings`}
                          className="rounded-md px-2.5 py-1 text-sm font-medium text-brand-600 transition hover:bg-brand-50"
                        >
                          Allocations
                        </Link>
                        {canEdit && (
                          <>
                            <button
                              onClick={() => setEditing(c)}
                              className="rounded-md px-2.5 py-1 text-sm font-medium text-slate-600 transition hover:bg-slate-100"
                            >
                              Edit
                            </button>
                            {c.isActive && (
                              <button
                                onClick={() => deactivate.mutate(c.id)}
                                className="rounded-md px-2.5 py-1 text-sm font-medium text-red-600 transition hover:bg-red-50"
                              >
                                Deactivate
                              </button>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {(creating || editing) && (
        <ChannelModal
          key={editing?.id ?? "new"}
          channel={editing}
          onClose={() => {
            setEditing(null);
            setCreating(false);
          }}
          onSave={(body) => save.mutate({ id: editing?.id, body })}
          error={save.isError ? apiErrorMessage(save.error) : ""}
          saving={save.isPending}
        />
      )}
    </div>
  );
}

function ChannelModal({
  channel,
  onClose,
  onSave,
  error,
  saving,
}: {
  channel: Channel | null;
  onClose: () => void;
  onSave: (body: ChannelInput) => void;
  error: string;
  saving: boolean;
}) {
  const [name, setName] = useState(channel?.name ?? "");
  const [code, setCode] = useState(channel?.code ?? "");
  const [type, setType] = useState(channel?.type ?? "MARKETPLACE");
  const [isActive, setIsActive] = useState(channel?.isActive ?? true);

  return (
    <Modal open title={channel ? "Edit channel" : "New channel"} onClose={onClose}>
      <div className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Name" value={name} onChange={(e) => setName(e.target.value)} />
        <Input label="Code" value={code} onChange={(e) => setCode(e.target.value)} placeholder="e.g. AMAZON" />
        <Select label="Type" value={type} onChange={(e) => setType(e.target.value as Channel["type"])}>
          {TYPES.map((t) => (
            <option key={t} value={t}>
              {t.replace("_", " ")}
            </option>
          ))}
        </Select>
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500/30"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
          />
          Active
        </label>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => onSave({ name, code, type, isActive })} disabled={!name || !code || saving}>
            {saving ? "Saving..." : "Save"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
