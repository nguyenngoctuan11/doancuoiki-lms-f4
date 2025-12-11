import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
  login as loginRequest,
  fetchMe,
  loginWithGoogle as loginWithGoogleRequest,
  loginWithFacebook as loginWithFacebookRequest,
} from "../services/auth";
import { getProfile as fetchProfile } from "../services/profile";
import { setAuthToken } from "../api/httpClient";

const STORAGE_KEY = "auth.session";

const AuthContext = createContext(null);

const readStoredSession = () => {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const writeLegacyKeys = (session) => {
  if (typeof window === "undefined") return;
  if (!session) {
    window.localStorage.removeItem("token");
    window.localStorage.removeItem("email");
    window.localStorage.removeItem("roles");
    return;
  }
  window.localStorage.setItem("token", session.token || "");
  window.localStorage.setItem("email", session.user?.email || "");
  window.localStorage.setItem("roles", (session.user?.roles || []).join(","));
};

const writeStoredSession = (session) => {
  if (typeof window === "undefined") return;
  if (!session) {
    window.localStorage.removeItem(STORAGE_KEY);
    writeLegacyKeys(null);
    return;
  }
  const payload = {
    accessToken: session.token,
    user: session.user,
  };
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  writeLegacyKeys(session);
};

const extractRoles = (payload) => {
  if (Array.isArray(payload?.roles?.items)) return payload.roles.items;
  if (Array.isArray(payload?.roles)) return payload.roles;
  return [];
};

const toUserShape = (payload, prevUser = null) => {
  if (!payload && !prevUser) return null;
  const base = payload || {};
  return {
    id: base.id ?? base.userId ?? prevUser?.id ?? null,
    email: base.email ?? prevUser?.email ?? "",
    fullName: base.fullName ?? prevUser?.fullName ?? "",
    username: base.username ?? prevUser?.username ?? "",
    bio: base.bio ?? prevUser?.bio ?? "",
    avatarUrl: base.avatarUrl ?? prevUser?.avatarUrl ?? "",
    locale: base.locale ?? prevUser?.locale ?? "vi",
    twoFactorEnabled: base.twoFactorEnabled ?? prevUser?.twoFactorEnabled ?? false,
    hasPassword: base.hasPassword ?? prevUser?.hasPassword ?? false,
    roles: extractRoles(base) ?? prevUser?.roles ?? [],
  };
};

const toAuthState = (payload) => {
  if (!payload) return null;
  const token = payload.accessToken ?? payload.token ?? null;
  if (!token) return null;
  const userPayload = payload.user ?? payload;
  return {
    token,
    user: toUserShape(userPayload),
  };
};

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => toAuthState(readStoredSession()));
  const [initialised, setInitialised] = useState(() => session == null);

  useEffect(() => {
    if (session?.token) {
      writeStoredSession(session);
      setAuthToken(session.token);
    } else {
      writeStoredSession(null);
      setAuthToken(null);
    }
  }, [session]);

  useEffect(() => {
    if (initialised || !session?.token) return;
    let cancelled = false;
    const refresh = async () => {
      try {
        const { data } = await fetchProfile();
        if (cancelled) return;
        setSession((prev) =>
          prev
            ? {
                token: prev.token,
                user: toUserShape(data, prev.user),
              }
            : prev,
        );
      } catch {
        // fallback to /api/auth/me in case profile endpoint unavailable
        try {
          const { data } = await fetchMe();
          if (!cancelled) {
            setSession((prev) =>
              prev
                ? {
                    token: prev.token,
                    user: toUserShape(data, prev.user),
                  }
                : prev,
            );
          }
        } catch {
          if (!cancelled) {
            setSession(null);
          }
        }
      } finally {
        if (!cancelled) setInitialised(true);
      }
    };
    refresh();
    return () => {
      cancelled = true;
    };
  }, [session, initialised]);

  useEffect(() => {
    if (typeof window === "undefined") return undefined;
    const handleUnauthorized = () => {
      setSession(null);
      setInitialised(true);
    };
    window.addEventListener("auth:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("auth:unauthorized", handleUnauthorized);
  }, []);

  const setAuthFromResponse = useCallback((response) => {
    const state = toAuthState(response);
    if (!state) throw new Error("Không thể xác thực người dùng");
    setSession(state);
    setInitialised(true);
    return state;
  }, []);

  const login = useCallback(async (credentials) => {
    const { data } = await loginRequest(credentials);
    return setAuthFromResponse(data);
  }, [setAuthFromResponse]);

  const loginWithGoogle = useCallback(async (payload) => {
    const { data } = await loginWithGoogleRequest(payload);
    return setAuthFromResponse(data);
  }, [setAuthFromResponse]);

  const loginWithFacebook = useCallback(async (payload) => {
    const { data } = await loginWithFacebookRequest(payload);
    return setAuthFromResponse(data);
  }, [setAuthFromResponse]);

  const logout = useCallback(() => {
    setSession(null);
    setInitialised(true);
  }, []);

  const refreshProfile = useCallback(async () => {
    const { data } = await fetchProfile();
    setSession((prev) =>
      prev
        ? {
            token: prev.token,
            user: toUserShape(data, prev.user),
          }
        : prev,
    );
    return data;
  }, []);

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      isAuthenticated: Boolean(session?.token),
      initialised,
      login,
      loginWithGoogle,
      loginWithFacebook,
      logout,
      refreshProfile,
      setAuthFromResponse,
    }),
    [session, initialised, login, loginWithGoogle, loginWithFacebook, logout, refreshProfile, setAuthFromResponse],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
};
