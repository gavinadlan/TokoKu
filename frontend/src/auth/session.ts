import type { TokenResponse } from '../api/types';

/**
 * Dua sesi terpisah (toko dan admin): penyimpanan, token, dan refresh sendiri-sendiri, sehingga satu browser bisa
 * masuk sebagai pembeli dan admin sekaligus tanpa saling menimpa. Access token hanya di memori. Refresh toko di
 * localStorage (bertahan antar kunjungan); refresh admin di sessionStorage (hilang saat tab ditutup, karena lebih
 * sensitif). Trade-off XSS dijelaskan di frontend/README.md.
 */
export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export interface Session {
  getAccess(): string | null;
  getRefresh(): string | null;
  set(tokens: Pick<TokenResponse, 'accessToken' | 'refreshToken'>): void;
  clear(): void;
  /** Dipanggil saat sesi dihapus paksa (refresh gagal) supaya UI ikut keluar. */
  onCleared(fn: () => void): () => void;
  /** Tukar refresh token (single-flight: refresh token berotasi, memakainya dua kali dianggap pencurian). */
  refresh(): Promise<boolean>;
}

interface Options {
  storageKey: string;
  storage: 'local' | 'session';
  refreshPath: string;
}

export function createSession({ storageKey, storage, refreshPath }: Options): Session {
  let accessToken: string | null = null;
  let refreshing: Promise<boolean> | null = null;
  const listeners = new Set<() => void>();

  const store = (): Storage | null => {
    try {
      return storage === 'local' ? localStorage : sessionStorage;
    } catch {
      return null;
    }
  };
  const readRefresh = () => {
    try {
      return store()?.getItem(storageKey) ?? null;
    } catch {
      return null;
    }
  };
  const writeRefresh = (token: string | null) => {
    try {
      if (token) store()?.setItem(storageKey, token);
      else store()?.removeItem(storageKey);
    } catch {
      /* penyimpanan tidak tersedia (mode privat): sesi hanya hidup di memori */
    }
  };

  const self: Session = {
    getAccess: () => accessToken,
    getRefresh: readRefresh,
    set(tokens) {
      accessToken = tokens.accessToken;
      writeRefresh(tokens.refreshToken);
    },
    clear() {
      accessToken = null;
      writeRefresh(null);
      listeners.forEach((fn) => fn());
    },
    onCleared(fn) {
      listeners.add(fn);
      return () => {
        listeners.delete(fn);
      };
    },
    refresh() {
      if (refreshing) return refreshing;
      const token = readRefresh();
      if (!token) return Promise.resolve(false);
      refreshing = (async () => {
        try {
          const res = await fetch(`${API_URL}${refreshPath}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: token }),
          });
          if (!res.ok) {
            if (res.status === 401 || res.status === 403) self.clear();
            return false;
          }
          self.set((await res.json()) as TokenResponse);
          return true;
        } catch {
          return false; // jaringan putus: jangan hapus sesi, coba lagi nanti
        } finally {
          refreshing = null;
        }
      })();
      return refreshing;
    },
  };
  return self;
}

export const storeSession = createSession({ storageKey: 'tokoku.refresh', storage: 'local', refreshPath: '/api/auth/refresh' });
export const adminSession = createSession({ storageKey: 'tokoku.admin.refresh', storage: 'session', refreshPath: '/api/admin/auth/refresh' });
