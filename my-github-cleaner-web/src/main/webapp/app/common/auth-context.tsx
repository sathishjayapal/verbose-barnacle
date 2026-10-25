import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import axios from 'axios';

/**
 * Centralized authentication state. The backend session is authoritative —
 * /api/auth/me returns {username, authorities} when a session cookie exists.
 * Roles come back as plain authorities ("ADMIN", "VIEWER") without a ROLE_ prefix.
 */
export interface AuthUser {
  username: string;
  authorities: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  /** true while the initial /api/auth/me probe is in flight */
  loading: boolean;
  isAdmin: boolean;
  isViewer: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const { data } = await axios.get<AuthUser>('/api/auth/me');
      setUser(data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const login = useCallback(async (username: string, password: string) => {
    const { data } = await axios.post<AuthUser>('/api/auth/login', { username, password });
    setUser({ username: data.username, authorities: data.authorities });
  }, []);

  const logout = useCallback(async () => {
    try {
      await axios.post('/api/auth/logout');
    } catch {
      // session may already be gone — clear local state regardless
    }
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    loading,
    isAdmin: user?.authorities.includes('ADMIN') ?? false,
    isViewer: user?.authorities.includes('VIEWER') ?? false,
    login,
    logout,
    refresh,
  }), [user, loading, login, logout, refresh]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
