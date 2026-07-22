import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { Button, Input } from "@/components/ui";
import { ErrorState } from "@/components/states";
import { authApi } from "@/api/auth";
import { apiErrorMessage } from "@/api/client";

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [identifier, setIdentifier] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authApi.forgotPassword(identifier);
      navigate("/reset-password", { state: { identifier } });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell title="Reset your password" subtitle="We'll send a reset code to your email">
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Email or phone" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? "Sending..." : "Send reset code"}
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
