import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { z } from 'zod';
import { useCart, useCheckout } from '../api/queries';
import { usePayAndRedirect } from '../api/usePayment';
import { Field } from '../components/Field';
import { Receipt } from '../components/Receipt';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { messageOf } from '../lib/errors';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const schema = z.object({
  shippingName: z.string().trim().min(2, 'Isi nama penerima.').max(100, 'Nama terlalu panjang.'),
  shippingPhone: z.string().trim().regex(/^[0-9+\-\s]{8,20}$/, 'Isi nomor telepon yang bisa dihubungi, 8 sampai 20 digit.'),
  shippingAddress: z.string().trim().min(10, 'Tulis alamat selengkap mungkin (jalan, nomor, kota).').max(500, 'Alamat terlalu panjang.'),
});
type Values = z.infer<typeof schema>;

export function CheckoutPage() {
  useDocumentTitle('Checkout');
  const cart = useCart();
  const checkout = useCheckout();
  const payment = usePayAndRedirect();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { shippingName: '', shippingPhone: '', shippingAddress: '' } });
  const { errors } = form.formState;
  const busy = checkout.isPending || payment.pending;

  if (cart.isPending) return <div className="wrap page-pad"><ListSkeleton /></div>;
  if (cart.isError) return <div className="wrap"><ErrorState error={cart.error} onRetry={() => cart.refetch()} title="Keranjang belum bisa dimuat" /></div>;
  if (cart.data.items.length === 0 && !checkout.isSuccess) {
    return (
      <div className="wrap page-pad">
        <EmptyState title="Belum ada yang dipesan" actions={<Link className="btn" to="/">Lihat katalog</Link>}>
          Keranjang Anda kosong, jadi belum ada yang bisa di-checkout.
        </EmptyState>
      </div>
    );
  }

  const onSubmit = async (v: Values) => {
    setError(null);
    let orderId: number;
    try {
      const order = await checkout.mutateAsync(v);
      orderId = order.id;
    } catch (e) {
      setError(messageOf(e));
      return;
    }
    try {
      await payment.start(orderId);
    } catch {
      // Order sudah dibuat; pembayaran bisa diulang dari halaman order
      navigate(`/order/${orderId}`, { state: { notice: 'Pesanan dibuat, tetapi pembayaran belum bisa dimulai. Tekan "Bayar sekarang" untuk mencoba lagi.' } });
    }
  };

  return (
    <div className="wrap page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-6)' }}>
        Checkout
      </h1>
      <div className="layout-2">
        <form id="checkout-form" className="form" noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 440, fontSize: 'var(--text-xl)' }}>Pengiriman</h2>
          {error && (
            <div className="alert err" role="alert">
              {error} <Link to="/keranjang">Periksa keranjang</Link>
            </div>
          )}
          <Field label="Nama penerima" error={errors.shippingName?.message}>
            <input className="input" autoComplete="name" {...form.register('shippingName')} />
          </Field>
          <Field label="Nomor telepon" hint="Dipakai kurir untuk menghubungi Anda." error={errors.shippingPhone?.message}>
            <input className="input" type="tel" inputMode="tel" autoComplete="tel" {...form.register('shippingPhone')} />
          </Field>
          <Field label="Alamat lengkap" error={errors.shippingAddress?.message}>
            <textarea className="input" autoComplete="street-address" rows={4} {...form.register('shippingAddress')} />
          </Field>
        </form>
        <aside>
          <Receipt
            title="Pesanan Anda"
            items={cart.data.items.map((i) => ({ name: i.productName, qty: i.quantity, subtotal: i.subtotal }))}
            total={cart.data.total}
            actions={
              <>
                <button className="btn block" type="submit" form="checkout-form" disabled={busy}>
                  {busy ? 'Memproses...' : 'Buat pesanan dan bayar'}
                </button>
                <p className="muted" style={{ fontSize: 'var(--text-xs)' }}>
                  Anda akan diarahkan ke halaman pembayaran Midtrans.
                </p>
              </>
            }
          />
        </aside>
      </div>
    </div>
  );
}
