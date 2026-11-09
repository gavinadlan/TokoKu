import { formatRupiah, productNumber, formatDate } from './format';

describe('formatRupiah', () => {
  it('memakai pemisah ribuan titik dan awalan Rp', () => {
    expect(formatRupiah(85000).replace(/\s/g, ' ')).toBe('Rp 85.000');
    expect(formatRupiah(1450000).replace(/\s/g, ' ')).toBe('Rp 1.450.000');
    expect(formatRupiah(0).replace(/\s/g, ' ')).toBe('Rp 0');
  });
  it('tidak menampilkan sen', () => {
    expect(formatRupiah(99.9).replace(/\s/g, ' ')).toBe('Rp 100');
  });
});

describe('productNumber', () => {
  it('menambah nol di depan sampai tiga digit', () => {
    expect(productNumber(1)).toBe('001');
    expect(productNumber(30)).toBe('030');
    expect(productNumber(1234)).toBe('1234');
  });
});

describe('formatDate', () => {
  it('memakai nama bulan bahasa Indonesia', () => {
    expect(formatDate('2026-11-10T03:00:00Z')).toContain('November');
  });
});
