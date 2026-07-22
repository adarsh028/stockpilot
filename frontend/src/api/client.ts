import axios, {
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";

/**
 * The ONE place a backend base URL is constructed. Every api/*.ts module imports
 * this configured instance and calls relative paths — change VITE_API_BASE_URL in
 * .env to repoint the whole app, with no code edits anywhere else.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

const ACCESS_KEY = "sp_access_token";
const REFRESH_KEY = "sp_refresh_token";

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  setAccess: (access: string) => localStorage.setItem(ACCESS_KEY, access),
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

client.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refresh = tokenStore.getRefresh();
  if (!refresh) return null;
  try {
    // Use a bare axios call so the interceptor doesn't loop.
    const resp = await axios.post(
      `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
      { refreshToken: refresh },
      { headers: { "Content-Type": "application/json" } }
    );
    const { accessToken, refreshToken } = resp.data;
    tokenStore.set(accessToken, refreshToken);
    return accessToken;
  } catch {
    tokenStore.clear();
    return null;
  }
}

client.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retried?: boolean };
    const status = error.response?.status;
    const isAuthCall = original?.url?.includes("/auth/");

    if (status === 401 && original && !original._retried && !isAuthCall) {
      original._retried = true;
      if (!refreshing) {
        refreshing = refreshAccessToken().finally(() => {
          refreshing = null;
        });
      }
      const newToken = await refreshing;
      if (newToken) {
        original.headers = original.headers ?? {};
        (original.headers as Record<string, string>).Authorization = `Bearer ${newToken}`;
        return client(original);
      }
      // Refresh failed — force re-login.
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

/** Download a file from an authenticated endpoint (attaches the bearer token). */
export async function downloadAuthed(path: string, filename: string): Promise<void> {
  const resp = await client.get(path, { responseType: "blob" });
  const url = window.URL.createObjectURL(resp.data as Blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined;
    return data?.message ?? error.message;
  }
  return "Something went wrong";
}

export default client;
