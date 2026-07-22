import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { channelsApi, ChannelInput } from "@/api/channels";
import { Button, Card, Input, Modal, Select } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
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
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">Channels</h1>
        {canEdit && <Button onClick={() => setCreating(true)}>+ New channel</Button>}
      </div>

      <Card>
        {query.isLoading ? (
          <LoadingSpinner />
        ) : query.isError ? (
          <ErrorState message={apiErrorMessage(query.error)} />
        ) : query.data && query.data.length === 0 ? (
          <EmptyState title="No channels" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Name</th>
                  <th className="py-2">Code</th>
                  <th className="py-2">Type</th>
                  <th className="py-2">Status</th>
                  <th className="py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {query.data?.map((c) => (
                  <tr key={c.id} className="border-b border-slate-50 hover:bg-slate-50">
                    <td className="py-2 font-medium text-slate-700">{c.name}</td>
                    <td className="py-2 text-slate-500">{c.code}</td>
                    <td className="py-2 text-slate-500">{c.type.replace("_", " ")}</td>
                    <td className="py-2">
                      <Badge color={c.isActive ? "green" : "slate"}>{c.isActive ? "Active" : "Inactive"}</Badge>
                    </td>
                    <td className="py-2 text-right">
                      <Link to={`/channels/${c.id}/listings`} className="mr-3 text-brand-600 hover:underline">
                        Allocations
                      </Link>
                      {canEdit && (
                        <>
                          <button onClick={() => setEditing(c)} className="mr-3 text-brand-600 hover:underline">
                            Edit
                          </button>
                          {c.isActive && (
                            <button onClick={() => deactivate.mutate(c.id)} className="text-red-600 hover:underline">
                              Deactivate
                            </button>
                          )}
                        </>
                      )}
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
          <input type="checkbox" checked={isActive} onChange={(e) => setIsActive(e.target.checked)} />
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
