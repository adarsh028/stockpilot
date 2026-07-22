import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { usersApi, InviteUserInput } from "@/api/users";
import { Button, Card, Input, Modal, Select } from "@/components/ui";
import { Badge, EmptyState, ErrorState, LoadingSpinner } from "@/components/states";
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
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-800">Team</h1>
        <Button onClick={() => setInviting(true)}>+ Invite user</Button>
      </div>

      <Card>
        {team.isLoading ? (
          <LoadingSpinner />
        ) : team.isError ? (
          <ErrorState message={apiErrorMessage(team.error)} />
        ) : team.data && team.data.content.length === 0 ? (
          <EmptyState title="No team members" />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-slate-500">
                  <th className="py-2">Name</th>
                  <th className="py-2">Email</th>
                  <th className="py-2">Role</th>
                  <th className="py-2">Status</th>
                  <th className="py-2 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {team.data?.content.map((u) => {
                  const isSelf = u.id === user?.id;
                  const isOwner = u.role === "OWNER";
                  return (
                    <tr key={u.id} className="border-b border-slate-50">
                      <td className="py-2 font-medium text-slate-700">{u.fullName}{isSelf && " (you)"}</td>
                      <td className="py-2 text-slate-500">{u.email}</td>
                      <td className="py-2">
                        <Badge color={isOwner ? "brand" : "slate"}>{u.role}</Badge>
                      </td>
                      <td className="py-2">
                        <Badge color={u.status === "ACTIVE" ? "green" : u.status === "DISABLED" ? "red" : "amber"}>
                          {u.status}
                        </Badge>
                      </td>
                      <td className="py-2 text-right">
                        {!isOwner && !isSelf && (
                          <>
                            {u.status === "ACTIVE" ? (
                              <button onClick={() => update.mutate({ id: u.id, status: "DISABLED" })} className="mr-3 text-amber-600 hover:underline">
                                Disable
                              </button>
                            ) : (
                              <button onClick={() => update.mutate({ id: u.id, status: "ACTIVE" })} className="mr-3 text-emerald-600 hover:underline">
                                Enable
                              </button>
                            )}
                            {user?.role === "OWNER" && (
                              <button onClick={() => confirm("Remove this user?") && remove.mutate(u.id)} className="text-red-600 hover:underline">
                                Remove
                              </button>
                            )}
                          </>
                        )}
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
          label="Temporary password (optional — auto-generated & emailed if blank)"
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
