import { estimateShipping, remainingForFreeShipping } from './shipping';

const rules = { flatShippingFee: 15000, freeShippingThreshold: 500000 };

describe('estimateShipping', () => {
  it('tarif flat di bawah ambang, gratis di ambang atau lebih', () => {
    expect(estimateShipping('DELIVERY', 377000, rules)).toBe(15000);
    expect(estimateShipping('DELIVERY', 500000, rules)).toBe(0);
    expect(estimateShipping('DELIVERY', 820000, rules)).toBe(0);
  });
  it('tanpa ambang selalu flat', () => {
    expect(estimateShipping('DELIVERY', 9_999_999, { flatShippingFee: 15000, freeShippingThreshold: null })).toBe(15000);
  });
  it('ambil di toko selalu gratis', () => {
    expect(estimateShipping('PICKUP', 1000, rules)).toBe(0);
  });
});

describe('remainingForFreeShipping', () => {
  it('menghitung sisa belanja', () => {
    expect(remainingForFreeShipping(377000, rules)).toBe(123000);
  });
  it('null bila sudah tercapai, tanpa ambang, atau ongkir 0', () => {
    expect(remainingForFreeShipping(500000, rules)).toBeNull();
    expect(remainingForFreeShipping(1000, { flatShippingFee: 15000, freeShippingThreshold: null })).toBeNull();
    expect(remainingForFreeShipping(1000, { flatShippingFee: 0, freeShippingThreshold: 500000 })).toBeNull();
  });
});
