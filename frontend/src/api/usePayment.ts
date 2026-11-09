import { useNavigate } from 'react-router';
import { usePay } from './queries';
import { isFakePayment, providerRefFromFake } from '../lib/payment';

/**
 * Memulai pembayaran lalu mengalihkan pengguna. Midtrans: redirect penuh ke redirect_url (alur terpendek,
 * tanpa memuat Snap.js). Provider fake (dev/test): halaman simulasi internal.
 */
export function usePayAndRedirect() {
  const pay = usePay();
  const navigate = useNavigate();

  async function start(orderId: number) {
    const res = await pay.mutateAsync(orderId);
    if (!res.redirectUrl) throw new Error('Backend tidak mengembalikan redirectUrl');
    if (isFakePayment(res.redirectUrl)) {
      navigate(`/dev/bayar/${providerRefFromFake(res.redirectUrl)}?order=${orderId}`);
    } else {
      window.location.assign(res.redirectUrl);
    }
  }
  return { start, pending: pay.isPending, error: pay.error, reset: pay.reset };
}
