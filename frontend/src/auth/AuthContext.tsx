import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, call } from '../api/client';
import type { User } from '../api/types';
import type { AuthStatus } from './guard';
import { refreshSession, session } from './session';

interface AuthValue {
  status: AuthStatus;
  user: User | null;
  login(email: string, password: string): Promise<User>;
  register(input: { name: string; email: string; password: string }): Promise<User>;
  logout(): Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [state, setState] = useState<{ status: AuthStatus; user: User | null }>({
    status: session.getRefresh() ? 'loading' : 'anonymous',
    user: null,
  });

  // Pulihkan sesi saat aplikasi dibuka: refresh token -> access token baru -> profil
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!session.getRefresh()) return;
      const ok = await refreshSession();
      if (!ok) {
        if (!cancelled) setState({ status: 'anonymous', user: null });
        return;
      }
      try {
        const me = await call(() => api.GET('/api/me'));
        if (!cancelled) setState({ status: 'authenticated', user: me });
      } catch {
        if (!cancelled) setState({ status: 'anonymous', user: null });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Sesi dihapus paksa (refresh gagal): keluar dari UI
  useEffect(
    () =>
      session.onCleared(() => {
        setState({ status: 'anonymous', user: null });
        queryClient.clear();
      }),
    [queryClient],
  );

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await call(() => api.POST('/api/auth/login', { body: { email, password } }));
      session.set(res);
      queryClient.clear();
      setState({ status: 'authenticated', user: res.user });
      return res.user;
    },
    [queryClient],
  );

  const register = useCallback(
    async (input: { name: string; email: string; password: string }) => {
      await call(() => api.POST('/api/auth/register', { body: input }));
      return login(input.email, input.password);
    },
    [login],
  );

  const logout = useCallback(async () => {
    const refreshToken = session.getRefresh();
    if (refreshToken) {
      try {
        await call(() => api.POST('/api/auth/logout', { body: { refreshToken } }));
      } catch {
        /* tetap keluar di sisi klien walau server tidak terjangkau */
      }
    }
    session.clear();
    queryClient.clear();
    setState({ status: 'anonymous', user: null });
  }, [queryClient]);

  const value = useMemo(() => ({ ...state, login, register, logout }), [state, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth harus dipakai di dalam AuthProvider');
  return ctx;
}
