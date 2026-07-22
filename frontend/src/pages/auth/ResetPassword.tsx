import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { Button, Input } from "@/components/ui";
import { ErrorState } from "@/components/states";
import { authApi } from "@/api/auth";
import { apiErrorMessage } from "@/api/client";

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();
  const initialId = (location.state as { identifier?: string } | null)?.identifier ?? "";
  const [identifier, setIdentifier] = useState(initialId);
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authApi.resetPassword(identifier, code, newPassword);
      navigate("/login");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell title="Set a new password" subtitle="Enter the code we sent and your new password">
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Email or phone" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
        <Input label="Reset code" value={code} onChange={(e) => setCode(e.target.value)} maxLength={6} required />
        <Input
          label="New password"
          type="password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          minLength={8}
          required
        />
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? "Resetting..." : "Reset password"}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          Back to login
        </Link>
      </p>
    </AuthShell>
  );
}
