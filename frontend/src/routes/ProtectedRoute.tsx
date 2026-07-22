import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { LoadingSpinner } from "@/components/states";
import { UserRole } from "@/types/api";

export function ProtectedRoute({
  children,
  roles,
}: {
  children: ReactNode;
  roles?: UserRole[];
}) {
  const { user, loading } = useAuth();
  if (loading) return <LoadingSpinner label="Checking session..." />;
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}
