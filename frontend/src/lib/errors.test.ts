import { messageOf, toApiError } from './errors';

describe('toApiError', () => {
  it('menerjemahkan pesan stok dari backend', () => {
    const e = toApiError(409, { detail: 'Insufficient stock for Kopi Gayo (available 3)' });
    expect(e.message).toBe('Stok Kopi Gayo tidak cukup (tersedia 3).');
    expect(e.status).toBe(409);
  });
  it('pesan tidak dikenal memakai pesan standar per status', () => {
    expect(toApiError(500, {}).message).toContain('Server');
    expect(toApiError(404, { detail: 'Product 9 not found' }).message).toContain('tidak ditemukan');
  });
  it('membaca Retry-After dan error per kolom', () => {
    const e = toApiError(429, { detail: 'Too many login attempts, try again later', errors: { email: 'x' } }, '42');
    expect(e.retryAfter).toBe(42);
    expect(e.fieldErrors).toEqual({ email: 'x' });
  });
  it('body bukan objek tidak membuat crash', () => {
    expect(toApiError(502, undefined).message).toContain('Server');
    expect(toApiError(401, 'oops').status).toBe(401);
  });
});

describe('messageOf', () => {
  it('error biasa menjadi pesan umum', () => {
    expect(messageOf(new Error('boom'))).toContain('Terjadi kesalahan');
  });
});
