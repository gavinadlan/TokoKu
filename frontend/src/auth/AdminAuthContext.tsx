import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { adminApi, callAdmin } from '../api/client';
import type { User } from '../api/types';
import type { AuthStatus } from './guard';
import { adminSession } from './session';

interface AdminAuthValue {
  status: AuthStatus;
  user: User | null;
  login(email: string, password: string): Promise<User>;
  logout(): Promise<void>;
}

const AdminAuthContext = createContext<AdminAuthValue | null>(null);

/** Sesi admin: terpisah total dari sesi toko (penyimpanan, token, dan instans konteks sendiri). */
export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<{ status: AuthStatus; user: User | null }>({
    status: adminSession.getRefresh() ? 'loading' : 'anonymous',
    user: null,
  });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!adminSession.getRefresh()) return;
      const ok = await adminSession.refresh();
      if (!ok) {
        if (!cancelled) setState({ status: 'anonymous', user: null });
        return;
      }
      try {
        const me = await callAdmin(() => adminApi.GET('/api/admin/me'));
        if (!cancelled) setState({ status: 'authenticated', user: me });
      } catch {
        if (!cancelled) setState({ status: 'anonymous', user: null });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => adminSession.onCleared(() => setState({ status: 'anonymous', user: null })), []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await callAdmin(() => adminApi.POST('/api/admin/auth/login', { body: { email, password } }));
    adminSession.set(res);
    setState({ status: 'authenticated', user: res.user });
    return res.user;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = adminSession.getRefresh();
    if (refreshToken) {
      try {
        await callAdmin(() => adminApi.POST('/api/admin/auth/logout', { body: { refreshToken } }));
      } catch {
        /* tetap keluar di sisi klien walau server tidak terjangkau */
      }
    }
    adminSession.clear();
    setState({ status: 'anonymous', user: null });
  }, []);

  const value = useMemo(() => ({ ...state, login, logout }), [state, login, logout]);
  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>;
}

export function useAdminAuth(): AdminAuthValue {
  const ctx = useContext(AdminAuthContext);
  if (!ctx) throw new Error('useAdminAuth harus dipakai di dalam AdminAuthProvider');
  return ctx;
}
