import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { usersApi, InviteUserInput } from "@/api/users";
import { Button, Card, Input, Modal, PageHeader, Select } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
import { PlusIcon, TeamIcon } from "@/components/icons";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";

export default function TeamList() {
  const qc = useQueryClient();
  const { user } = useAuth();
  const [inviting, setInviting] = useState(false);

  const team = useQuery({ queryKey: ["team"], queryFn: () => usersApi.list(0, 50) });

  const invite = useMutation({
    mutationFn: (body: InviteUserInput) => usersApi.invite(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["team"] });
      setInviting(false);
    },
  });
  const update = useMutation({
    mutationFn: (v: { id: string; status?: string; role?: string }) =>
      usersApi.update(v.id, { status: v.status, role: v.role }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["team"] }),
  });
  const remove = useMutation({
    mutationFn: (id: string) => usersApi.remove(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["team"] }),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Team" subtitle="People with access to this workspace">
        <Button onClick={() => setInviting(true)}>
          <PlusIcon className="text-base" /> Invite user
        </Button>
      </PageHeader>

      <Card>
        {team.isLoading ? (
          <LoadingSpinner />
        ) : team.isError ? (
          <ErrorState message={apiErrorMessage(team.error)} />
        ) : team.data && team.data.content.length === 0 ? (
          <EmptyState icon={<TeamIcon />} title="No team members" />
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {team.data?.content.map((u) => {
                  const isSelf = u.id === user?.id;
                  const isOwner = u.role === "OWNER";
                  return (
                    <tr key={u.id}>
                      <td className="font-medium text-slate-800">{u.fullName}{isSelf && <span className="ml-1 text-xs font-normal text-slate-400">(you)</span>}</td>
                      <td>{u.email}</td>
                      <td>
                        <Badge color={isOwner ? "brand" : "slate"}>{u.role}</Badge>
                      </td>
                      <td>
                        <Badge color={u.status === "ACTIVE" ? "green" : u.status === "DISABLED" ? "red" : "amber"}>
                          {u.status}
                        </Badge>
                      </td>
                      <td className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          {!isOwner && !isSelf && (
                            <>
                              {u.status === "ACTIVE" ? (
                                <button onClick={() => update.mutate({ id: u.id, status: "DISABLED" })} className="rounded-md px-2.5 py-1 text-sm font-medium text-amber-600 transition hover:bg-amber-50">
                                  Disable
                                </button>
                              ) : (
                                <button onClick={() => update.mutate({ id: u.id, status: "ACTIVE" })} className="rounded-md px-2.5 py-1 text-sm font-medium text-emerald-600 transition hover:bg-emerald-50">
                                  Enable
                                </button>
                              )}
                              {user?.role === "OWNER" && (
                                <button onClick={() => confirm("Remove this user?") && remove.mutate(u.id)} className="rounded-md px-2.5 py-1 text-sm font-medium text-red-600 transition hover:bg-red-50">
                                  Remove
                                </button>
                              )}
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {inviting && (
        <InviteModal
          onClose={() => setInviting(false)}
          onSave={(body) => invite.mutate(body)}
          error={invite.isError ? apiErrorMessage(invite.error) : ""}
          saving={invite.isPending}
        />
      )}
    </div>
  );
}

function InviteModal({
  onClose,
  onSave,
  error,
  saving,
}: {
  onClose: () => void;
  onSave: (body: InviteUserInput) => void;
  error: string;
  saving: boolean;
}) {
  const [form, setForm] = useState<InviteUserInput>({ fullName: "", email: "", phone: "", role: "STAFF", password: "" });
  return (
    <Modal open title="Invite team member" onClose={onClose}>
      <div className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        <Input label="Email" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <Input label="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="+91 90000 00000" />
        <Select label="Role" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as "ADMIN" | "STAFF" })}>
          <option value="STAFF">Staff</option>
          <option value="ADMIN">Admin</option>
        </Select>
        <Input
          label="Temporary password"
          hint="Optional — auto-generated and emailed if left blank."
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button onClick={() => onSave(form)} disabled={!form.fullName || !form.email || saving}>
            {saving ? "Inviting..." : "Send invite"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}
