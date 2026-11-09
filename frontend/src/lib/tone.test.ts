import { toneFor } from './tone';

describe('toneFor', () => {
  it('selalu di rentang 1..6', () => {
    for (let c = 1; c <= 8; c++) for (let p = 1; p <= 40; p++) {
      const t = toneFor(c, p);
      expect(t).toBeGreaterThanOrEqual(1);
      expect(t).toBeLessThanOrEqual(6);
    }
  });
  it('satu kategori selalu dalam satu keluarga (terang 1-3 atau gelap 4-6), paling banyak 3 nada', () => {
    for (let c = 1; c <= 8; c++) {
      const tones = new Set(Array.from({ length: 12 }, (_, i) => toneFor(c, i + 1)));
      expect(tones.size).toBeLessThanOrEqual(3);
      const dark = [...tones].every((t) => t >= 4);
      const light = [...tones].every((t) => t <= 3);
      expect(dark || light).toBe(true);
    }
  });
  it('kategori bersebelahan berganti keluarga terang dan gelap', () => {
    expect(toneFor(1, 1)).toBeLessThanOrEqual(3);
    expect(toneFor(2, 1)).toBeGreaterThanOrEqual(4);
    expect(toneFor(3, 1)).toBeLessThanOrEqual(3);
  });
  it('produk berurutan dalam satu kategori bergantian nada', () => {
    expect(toneFor(1, 1)).not.toBe(toneFor(1, 2));
  });
});
