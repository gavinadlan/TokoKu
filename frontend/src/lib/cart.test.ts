import { clampQuantity, recalc, without, withQuantity, type CartLine } from './cart';

const line = (over: Partial<CartLine>): CartLine => ({
  id: 1, productId: 1, productName: 'Kopi', unitPrice: 85000, quantity: 1, subtotal: 85000, availableStock: 10, ...over,
});

describe('recalc', () => {
  it('menghitung subtotal, jumlah, dan total', () => {
    const t = recalc([line({ id: 1, quantity: 2 }), line({ id: 2, unitPrice: 48000, quantity: 3 })]);
    expect(t.items.map((i) => i.subtotal)).toEqual([170000, 144000]);
    expect(t.totalQuantity).toBe(5);
    expect(t.total).toBe(314000);
  });
  it('keranjang kosong bernilai nol', () => {
    expect(recalc([])).toEqual({ items: [], totalQuantity: 0, total: 0 });
  });
});

describe('withQuantity / without', () => {
  const cart = recalc([line({ id: 1, quantity: 1 }), line({ id: 2, unitPrice: 10000, quantity: 1 })]);
  it('mengubah jumlah satu baris dan menghitung ulang total', () => {
    const next = withQuantity(cart, 2, 4);
    expect(next.total).toBe(85000 + 40000);
    expect(next.totalQuantity).toBe(5);
  });
  it('menghapus satu baris', () => {
    const next = without(cart, 1);
    expect(next.items).toHaveLength(1);
    expect(next.total).toBe(10000);
  });
});

describe('clampQuantity', () => {
  it('dibatasi 1 sampai stok', () => {
    expect(clampQuantity(0, 5)).toBe(1);
    expect(clampQuantity(9, 5)).toBe(5);
    expect(clampQuantity(3, 5)).toBe(3);
  });
  it('stok 0 tetap minimal 1 (tombol sendiri yang dinonaktifkan)', () => {
    expect(clampQuantity(3, 0)).toBe(1);
  });
});
