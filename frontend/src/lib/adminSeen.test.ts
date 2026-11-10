import { describe, expect, it } from 'vitest';
import { agoLabel, getSeenAt, isNewSince, markSeen } from './adminSeen';

describe('adminSeen', () => {
  it('menandai order dibayar setelah kunjungan terakhir sebagai baru', () => {
    expect(isNewSince('2026-11-10T10:00:05Z', '2026-11-10T10:00:00Z')).toBe(true);
    expect(isNewSince('2026-11-10T09:59:00Z', '2026-11-10T10:00:00Z')).toBe(false);
    expect(isNewSince(null, '2026-11-10T10:00:00Z')).toBe(false);
    expect(isNewSince('2026-11-10T10:00:05Z', null)).toBe(false); // kunjungan pertama: tidak ada sorotan
  });
  it('menyimpan dan membaca waktu kunjungan', () => {
    markSeen(new Date('2026-11-10T10:00:00Z'));
    expect(getSeenAt()).toBe('2026-11-10T10:00:00.000Z');
  });
  it('label umur data', () => {
    expect(agoLabel(1000, 2000)).toBe('baru saja');
    expect(agoLabel(0, 12_000)).toBe('12 detik lalu');
    expect(agoLabel(0, 190_000)).toBe('3 menit lalu');
  });
});
