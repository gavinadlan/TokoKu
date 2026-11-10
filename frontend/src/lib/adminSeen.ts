const KEY = 'tokoku.admin.orders.seenAt';

/** Waktu (ISO) kunjungan terakhir ke halaman Order admin. Penyimpanan bisa tidak tersedia: aman gagal. */
export function getSeenAt(): string | null {
  try { return localStorage.getItem(KEY); } catch { return null; }
}

export function markSeen(now: Date = new Date()): void {
  try { localStorage.setItem(KEY, now.toISOString()); } catch { /* abaikan */ }
}

export function isNewSince(invoicedAt: string | null | undefined, seenAt: string | null): boolean {
  return !!invoicedAt && !!seenAt && Date.parse(invoicedAt) > Date.parse(seenAt);
}

/** "baru saja" / "12 detik lalu" / "3 menit lalu" */
export function agoLabel(updatedAt: number, now: number): string {
  const s = Math.max(0, Math.round((now - updatedAt) / 1000));
  if (s < 5) return 'baru saja';
  if (s < 60) return `${s} detik lalu`;
  return `${Math.floor(s / 60)} menit lalu`;
}
