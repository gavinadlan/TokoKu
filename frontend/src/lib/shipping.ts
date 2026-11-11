import type { Fulfillment } from './status';

export interface ShippingRules {
  flatShippingFee: number;
  freeShippingThreshold?: number | null;
}

/**
 * Perkiraan ongkir untuk tampilan. Server menghitung sendiri saat checkout (angka ini tidak dikirim ke server),
 * dengan aturan yang sama: ambil di toko gratis; kirim = tarif flat, gratis bila subtotal mencapai ambang.
 */
export function estimateShipping(type: Fulfillment, subtotal: number, rules: ShippingRules): number {
  if (type === 'PICKUP') return 0;
  const threshold = rules.freeShippingThreshold;
  if (threshold !== null && threshold !== undefined && subtotal >= threshold) return 0;
  return rules.flatShippingFee;
}

/** Sisa belanja agar gratis ongkir, atau null bila tidak ada ambang / sudah tercapai. */
export function remainingForFreeShipping(subtotal: number, rules: ShippingRules): number | null {
  const threshold = rules.freeShippingThreshold;
  if (threshold === null || threshold === undefined || rules.flatShippingFee <= 0 || subtotal >= threshold) return null;
  return threshold - subtotal;
}
