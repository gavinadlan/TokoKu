import type { TokenResponse } from '../api/types';

/**
 * Access token hanya di memori (hilang saat tab ditutup, tidak terbaca oleh skrip lain lewat storage).
 * Refresh token di localStorage agar sesi bertahan; trade-off XSS dijelaskan di frontend/README.md.
 */
const REFRESH_KEY = 'tokoku.refresh';
const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

let accessToken: string | null = null;
let refreshing: Promise<boolean> | null = null;
const listeners = new Set<() => void>();

function readRefresh(): string | null {
  try {
    return localStorage.getItem(REFRESH_KEY);
  } catch {
    return null;
  }
}

function writeRefresh(token: string | null): void {
  try {
    if (token) localStorage.setItem(REFRESH_KEY, token);
    else localStorage.removeItem(REFRESH_KEY);
  } catch {
    /* penyimpanan tidak tersedia (mode privat): sesi hanya hidup di memori */
  }
}

export const session = {
  getAccess: () => accessToken,
  getRefresh: readRefresh,
  set(tokens: Pick<TokenResponse, 'accessToken' | 'refreshToken'>) {
    accessToken = tokens.accessToken;
    writeRefresh(tokens.refreshToken);
  },
  clear() {
    accessToken = null;
    writeRefresh(null);
    listeners.forEach((fn) => fn());
  },
  /** Dipanggil saat sesi dihapus paksa (refresh gagal) supaya UI ikut keluar. */
  onCleared(fn: () => void) {
    listeners.add(fn);
    return () => {
      listeners.delete(fn);
    };
  },
};

/**
 * Menukar refresh token dengan pasangan token baru. Single-flight: banyak request yang 401 bersamaan
 * hanya memicu satu refresh (refresh token berotasi, memakainya dua kali dianggap pencurian oleh backend).
 */
export function refreshSession(): Promise<boolean> {
  if (refreshing) return refreshing;
  const token = readRefresh();
  if (!token) return Promise.resolve(false);
  refreshing = (async () => {
    try {
      const res = await fetch(`${API_URL}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: token }),
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) session.clear();
        return false;
      }
      const data = (await res.json()) as TokenResponse;
      session.set(data);
      return true;
    } catch {
      return false; // jaringan putus: jangan hapus sesi, coba lagi nanti
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}
