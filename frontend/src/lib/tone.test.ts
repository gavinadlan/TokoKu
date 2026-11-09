import { toneFor } from './tone';

describe('toneFor', () => {
  it('selalu di rentang 1..6', () => {
    for (let c = 1; c <= 8; c++) for (let p = 1; p <= 40; p++) {
      const t = toneFor(c, p);
      expect(t).toBeGreaterThanOrEqual(1);
      expect(t).toBeLessThanOrEqual(6);
    }
  });
  it('produk dalam satu kategori memakai paling banyak 3 nada berdekatan', () => {
    const tones = new Set([1, 2, 3, 4, 5, 6, 7, 8, 9].map((p) => toneFor(2, p)));
    expect(tones.size).toBeLessThanOrEqual(3);
  });
  it('kategori berbeda bergeser nada', () => {
    expect(toneFor(1, 3)).not.toBe(toneFor(2, 3));
  });
});
