import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { authApi } from "@/api/auth";
import { tokenStore } from "@/api/client";
import { AuthUser } from "@/types/api";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (identifier: string, password: string) => Promise<void>;
  setSession: (user: AuthUser, accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

const USER_KEY = "sp_user";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  });
  const [loading, setLoading] = useState<boolean>(!!tokenStore.getAccess());

  useEffect(() => {
    // Validate the stored session on load.
    if (tokenStore.getAccess() && !user) {
      authApi
        .me()
        .then((me) => {
          const u: AuthUser = {
            id: me.id,
            organizationId: me.organizationId,
            organizationName: "",
            fullName: me.fullName,
            email: me.email,
            phone: me.phone,
            role: me.role,
          };
          setUser(u);
          localStorage.setItem(USER_KEY, JSON.stringify(u));
        })
        .catch(() => {
          tokenStore.clear();
          localStorage.removeItem(USER_KEY);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const setSession = useCallback((u: AuthUser, accessToken: string, refreshToken: string) => {
    tokenStore.set(accessToken, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(u));
    setUser(u);
  }, []);

  const login = useCallback(
    async (identifier: string, password: string) => {
      const res = await authApi.login(identifier, password);
      setSession(res.user, res.accessToken, res.refreshToken);
    },
    [setSession]
  );

  const logout = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
    window.location.href = "/login";
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, setSession, logout }),
    [user, loading, login, setSession, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
