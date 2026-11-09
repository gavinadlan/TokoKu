import { isFakePayment, orderIdFromProviderRef, providerRefFromFake } from './payment';

describe('orderIdFromProviderRef', () => {
  it('mengambil id order dari order_id Midtrans', () => {
    expect(orderIdFromProviderRef('TKU-12-ab12cd34')).toBe(12);
    expect(orderIdFromProviderRef('TKU-1-0f0f0f0f')).toBe(1);
  });
  it('menolak format lain', () => {
    expect(orderIdFromProviderRef(null)).toBeNull();
    expect(orderIdFromProviderRef('ORDER-9')).toBeNull();
    expect(orderIdFromProviderRef('TKU-x-abc')).toBeNull();
  });
});

describe('fake payment helpers', () => {
  it('mengenali URL fake dan mengambil referensinya', () => {
    const url = 'https://fake-payment.invalid/pay/TKU-3-aabbccdd';
    expect(isFakePayment(url)).toBe(true);
    expect(isFakePayment('https://app.sandbox.midtrans.com/snap/v4/redirection/x')).toBe(false);
    expect(providerRefFromFake(url)).toBe('TKU-3-aabbccdd');
  });
});
