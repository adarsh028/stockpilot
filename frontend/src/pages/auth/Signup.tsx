import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthShell } from "./AuthShell";
import { Button, Input } from "@/components/ui";
import { ErrorState } from "@/components/states";
import { authApi } from "@/api/auth";
import { apiErrorMessage } from "@/api/client";

export default function Signup() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    organizationName: "",
    fullName: "",
    email: "",
    phone: "",
    password: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function set(key: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [key]: e.target.value });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await authApi.signup(form);
      navigate("/verify-otp", { state: { identifier: form.email } });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell title="Create your brand account" subtitle="Start managing inventory across channels">
      <form onSubmit={onSubmit} className="space-y-4">
        {error && <ErrorState message={error} />}
        <Input label="Organization / brand name" value={form.organizationName} onChange={set("organizationName")} required />
        <Input label="Your full name" value={form.fullName} onChange={set("fullName")} required />
        <Input label="Email" type="email" value={form.email} onChange={set("email")} required />
        <Input label="Phone" value={form.phone} onChange={set("phone")} placeholder="+91 90000 00000" required />
        <Input label="Password" type="password" value={form.password} onChange={set("password")} required minLength={8} />
        <Button type="submit" disabled={loading} className="w-full">
          {loading ? "Creating..." : "Create account"}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-500">
        Already have an account?{" "}
        <Link to="/login" className="font-medium text-brand-600 hover:underline">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
