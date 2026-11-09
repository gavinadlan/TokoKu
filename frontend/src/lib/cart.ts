export interface CartLine {
  id: number;
  productId: number;
  productName: string;
  imageUrl?: string | null;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  availableStock: number;
}

export interface CartTotals {
  items: CartLine[];
  totalQuantity: number;
  total: number;
}

/** Menghitung ulang subtotal, jumlah, dan total dari daftar baris (dipakai untuk optimistic update). */
export function recalc(lines: CartLine[]): CartTotals {
  const items = lines.map((l) => ({ ...l, subtotal: l.unitPrice * l.quantity }));
  return {
    items,
    totalQuantity: items.reduce((n, l) => n + l.quantity, 0),
    total: items.reduce((sum, l) => sum + l.subtotal, 0),
  };
}

/** Jumlah baru dibatasi 1 sampai stok tersedia. */
export function clampQuantity(quantity: number, availableStock: number): number {
  return Math.max(1, Math.min(quantity, Math.max(1, availableStock)));
}

export function withQuantity(cart: CartTotals, itemId: number, quantity: number): CartTotals {
  return recalc(cart.items.map((l) => (l.id === itemId ? { ...l, quantity } : l)));
}

export function without(cart: CartTotals, itemId: number): CartTotals {
  return recalc(cart.items.filter((l) => l.id !== itemId));
}
