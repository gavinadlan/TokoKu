/** Midtrans memanggil order_id = providerRef berbentuk "TKU-<orderId>-<hex>". */
export function orderIdFromProviderRef(ref: string | null): number | null {
  if (!ref) return null;
  const m = ref.match(/^TKU-(\d+)-[0-9a-f]+$/i);
  return m ? Number(m[1]) : null;
}

/** Provider "fake" (hanya dev/test) mengembalikan URL ini; tidak bisa dibuka, jadi dialihkan ke halaman simulasi. */
export function isFakePayment(redirectUrl: string): boolean {
  return redirectUrl.startsWith('https://fake-payment.invalid/');
}

export function providerRefFromFake(redirectUrl: string): string {
  return redirectUrl.split('/').pop() ?? '';
}
