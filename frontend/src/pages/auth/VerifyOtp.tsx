import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { Button, Input } from "@/components/ui";
import { ErrorState } from "@/components/states";
import { authApi } from "@/api/auth";
import { apiErrorMessage } from "@/api/client";
import { useAuth } from "@/context/AuthContext";

export default function VerifyOtp() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setSession } = useAuth();
  const initialId = (location.state as { identifier?: string } | null)?.identifier ?? "";
  const [identifier, setIdentifier] = useState(initialId);
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await authApi.verifyOtp(identifier, code);
      setSession(res.user, res.accessToken, res.refreshToken);
      navigate("/");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function resend() {
    setError("");
    setInfo("");
    try {
      await authApi.resendOtp(identifier);
      setInfo("A new code has been sent. (In dev, check the backend console log.)");
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <AuthShell title="Verify your account" subtitle="Enter the 6-digit code sent to your email">
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <ErrorState message={error} />}
        {info && <p className="rounded-md bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{info}</p>}
        <Input label="Email or phone" value={identifier} onChange={(e) => setIdentifier(e.target.value)} required />
        <Input
          label="Verification code"
          value={code}
          onChange={(e) => setCode(e.target.value)}
          placeholder="000000"
          maxLength={6}
          required
        />
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? "Verifying..." : "Verify & continue"}
        </Button>
      </form>
      <div className="mt-4 flex justify-between text-sm">
        <button onClick={resend} className="text-brand-600 hover:underline">
          Resend code
        </button>
        <Link to="/login" className="text-slate-500 hover:underline">
          Back to login
        </Link>
      </div>
      <p className="mt-4 rounded-md bg-amber-50 p-3 text-xs text-amber-700">
        No email service configured? The OTP is printed in the backend console, labeled
        <span className="font-mono"> [DEV EMAIL FALLBACK]</span>.
      </p>
    </AuthShell>
  );
}
