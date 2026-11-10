import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';
import { z } from 'zod';
import { useCart, useCheckout, useStore } from '../api/queries';
import { usePayAndRedirect } from '../api/usePayment';
import { Field } from '../components/Field';
import { Receipt } from '../components/Receipt';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { messageOf } from '../lib/errors';
import { formatRupiah } from '../lib/format';
import { estimateShipping, remainingForFreeShipping } from '../lib/shipping';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const person = {
  shippingName: z.string().trim().min(2, 'Isi nama.').max(100, 'Nama terlalu panjang.'),
  shippingPhone: z.string().trim().regex(/^[0-9+\-\s]{8,20}$/, 'Isi nomor telepon yang bisa dihubungi, 8 sampai 20 digit.'),
};

// Alamat wajib hanya untuk pengiriman; ambil di toko cukup nama dan telepon
const schema = z.discriminatedUnion('fulfillmentType', [
  z.object({ fulfillmentType: z.literal('DELIVERY'), ...person, shippingAddress: z.string().trim().min(10, 'Tulis alamat selengkap mungkin (jalan, nomor, kota).').max(500, 'Alamat terlalu panjang.') }),
  z.object({ fulfillmentType: z.literal('PICKUP'), ...person, shippingAddress: z.string().optional() }),
]);
type Values = z.infer<typeof schema>;

export function CheckoutPage() {
  useDocumentTitle('Checkout');
  const cart = useCart();
  const store = useStore();
  const checkout = useCheckout();
  const payment = usePayAndRedirect();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { fulfillmentType: 'DELIVERY', shippingName: '', shippingPhone: '', shippingAddress: '' } });
  const { errors } = form.formState;
  const type = useWatch({ control: form.control, name: 'fulfillmentType' });
  const busy = checkout.isPending || payment.pending;

  if (cart.isPending || store.isPending) return <div className="wrap page-pad"><ListSkeleton /></div>;
  if (cart.isError) return <div className="wrap"><ErrorState error={cart.error} onRetry={() => cart.refetch()} title="Keranjang belum bisa dimuat" /></div>;
  if (store.isError) return <div className="wrap"><ErrorState error={store.error} onRetry={() => store.refetch()} title="Info toko belum bisa dimuat" /></div>;
  if (cart.data.items.length === 0 && !checkout.isSuccess) {
    return (
      <div className="wrap page-pad">
        <EmptyState title="Belum ada yang dipesan" actions={<Link className="btn" to="/">Lihat katalog</Link>}>
          Keranjang Anda kosong, jadi belum ada yang bisa di-checkout.
        </EmptyState>
      </div>
    );
  }

  const s = store.data;
  const subtotal = cart.data.total;
  const fee = estimateShipping(type, subtotal, s);
  const remaining = type === 'DELIVERY' ? remainingForFreeShipping(subtotal, s) : null;

  const onSubmit = async (v: Values) => {
    setError(null);
    let orderId: number;
    try {
      const order = await checkout.mutateAsync({
        fulfillmentType: v.fulfillmentType,
        shippingName: v.shippingName,
        shippingPhone: v.shippingPhone,
        shippingAddress: v.fulfillmentType === 'DELIVERY' ? v.shippingAddress : undefined,
      });
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

  const deliveryErr = errors as { shippingAddress?: { message?: string } };

  return (
    <div className="wrap page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-6)' }}>
        Checkout
      </h1>
      <div className="layout-2">
        <form id="checkout-form" className="form" noValidate onSubmit={form.handleSubmit(onSubmit)}>
          <fieldset className="seg">
            <legend>Cara menerima pesanan</legend>
            <label>
              <input type="radio" value="DELIVERY" disabled={!s.deliveryEnabled} {...form.register('fulfillmentType')} />
              <span className="t">Kirim ke alamat</span>
              <span className="s">
                {!s.deliveryEnabled
                  ? 'Pengiriman sedang tidak tersedia.'
                  : s.flatShippingFee > 0
                    ? `Biaya kirim ${formatRupiah(s.flatShippingFee)}.${s.freeShippingThreshold ? ` Gratis untuk belanja ${formatRupiah(s.freeShippingThreshold)} atau lebih.` : ''}`
                    : 'Gratis ongkir.'}
              </span>
            </label>
            <label>
              <input type="radio" value="PICKUP" disabled={!s.pickupEnabled} {...form.register('fulfillmentType')} />
              <span className="t">Ambil di toko</span>
              <span className="s">{s.pickupEnabled ? 'Gratis. Siap diambil setelah pembayaran dikonfirmasi dan pesanan diproses.' : 'Pengambilan di toko belum tersedia.'}</span>
            </label>
          </fieldset>

          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 440, fontSize: 'var(--text-xl)' }}>{type === 'PICKUP' ? 'Pengambil' : 'Pengiriman'}</h2>
          {error && (
            <div className="alert err" role="alert">
              {error} <Link to="/keranjang">Periksa keranjang</Link>
            </div>
          )}
          <Field label={type === 'PICKUP' ? 'Nama pengambil' : 'Nama penerima'} error={errors.shippingName?.message}>
            <input className="input" autoComplete="name" {...form.register('shippingName')} />
          </Field>
          <Field label="Nomor telepon" hint={type === 'PICKUP' ? 'Dihubungi bila pesanan sudah siap diambil.' : 'Dipakai kurir untuk menghubungi Anda.'} error={errors.shippingPhone?.message}>
            <input className="input" type="tel" inputMode="tel" autoComplete="tel" {...form.register('shippingPhone')} />
          </Field>
          {type === 'DELIVERY' ? (
            <Field label="Alamat lengkap" error={deliveryErr.shippingAddress?.message}>
              <textarea className="input" autoComplete="street-address" rows={4} {...form.register('shippingAddress')} />
            </Field>
          ) : (
            <section className="storebox" aria-label="Lokasi pengambilan">
              <h3>Ambil di {s.name}</h3>
              <dl>
                <div><dt>Alamat</dt><dd>{s.address}</dd></div>
                {s.openingHours && <div><dt>Jam buka</dt><dd>{s.openingHours}</dd></div>}
                <div><dt>Telepon</dt><dd>{s.phone}</dd></div>
                {s.pickupInstructions && <div><dt>Petunjuk</dt><dd>{s.pickupInstructions}</dd></div>}
              </dl>
            </section>
          )}
        </form>
        <aside>
          <Receipt
            title="Pesanan Anda"
            items={cart.data.items.map((i) => ({ name: i.productName, qty: i.quantity, subtotal: i.subtotal }))}
            rows={[
              { label: `Subtotal (${cart.data.totalQuantity} barang)`, value: formatRupiah(subtotal) },
              type === 'PICKUP'
                ? { label: 'Ambil di toko', value: <span className="free">Gratis</span> }
                : { label: 'Biaya kirim', value: fee === 0 ? <span className="free">Gratis</span> : formatRupiah(fee) },
            ]}
            total={subtotal + fee}
            note={remaining !== null ? `Tambah ${formatRupiah(remaining)} lagi untuk gratis ongkir.` : type === 'PICKUP' ? 'Kode ambil muncul di halaman order setelah pembayaran diterima.' : undefined}
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
