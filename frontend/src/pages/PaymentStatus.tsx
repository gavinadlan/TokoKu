import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router';
import { useOrder, useRefreshPayment } from '../api/queries';
import { ErrorState, ListSkeleton } from '../components/States';
import { formatRupiah } from '../lib/format';
import { orderIdFromProviderRef } from '../lib/payment';
import type { OrderStatus } from '../lib/status';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { NotFoundView } from './NotFound';

/**
 * Halaman tujuan setelah bayar. Status tidak menunggu webhook (yang tidak bisa menjangkau localhost dan bisa
 * terlambat): halaman ini meminta backend menanyakan status ke Midtrans tiap 5 detik, maksimal 30 detik.
 */
export const POLL_LIMIT_MS = 30_000;
export const REFRESH_EVERY_MS = 5_000;

export function StatusView({ orderId }: { orderId: number }) {
  const [gaveUp, setGaveUp] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => setGaveUp(true), POLL_LIMIT_MS);
    return () => clearTimeout(t);
  }, []);
  const order = useOrder(orderId);
  const { mutate: check, isPending: checking, isError: checkFailed } = useRefreshPayment();
  const pending = order.data?.status === 'PENDING_PAYMENT';
  useEffect(() => {
    if (!pending || gaveUp) return;
    check(orderId);
    const t = setInterval(() => check(orderId), REFRESH_EVERY_MS);
    return () => clearInterval(t);
  }, [pending, gaveUp, orderId, check]);
  useDocumentTitle('Status pembayaran');

  if (order.isPending) return <div className="wrap narrow page-pad"><ListSkeleton rows={2} /></div>;
  if (order.isError) return <div className="wrap"><ErrorState error={order.error} onRetry={() => order.refetch()} title="Status belum bisa diperiksa" /></div>;

  const o = order.data;
  const status = o.status as OrderStatus;
  const paid = status !== 'PENDING_PAYMENT' && status !== 'CANCELLED';

  return (
    <div className="wrap narrow page-pad">
      <div className="pay-result" role="status" aria-live="polite">
        {paid ? (
          <>
            <h1>Pembayaran diterima</h1>
            <p className="muted">
              Terima kasih. Order No. {String(o.id).padStart(4, '0')} sebesar <b className="tnum">{formatRupiah(o.totalAmount)}</b> sudah dibayar dan sedang kami siapkan.{o.fulfillmentType === 'PICKUP' && ' Kode ambil ada di halaman order.'}
            </p>
          </>
        ) : status === 'CANCELLED' ? (
          <>
            <h1>Pembayaran tidak berhasil</h1>
            <p className="muted">
              Pembayaran ditolak atau sudah kedaluwarsa, jadi pesanan dibatalkan dan barangnya dikembalikan ke stok. Anda bisa memesan lagi kapan saja.
            </p>
          </>
        ) : gaveUp ? (
          <>
            <h1>Pembayaran belum terkonfirmasi</h1>
            <p className="muted">
              Konfirmasi dari bank atau dompet digital kadang butuh waktu satu sampai beberapa menit. Jika Anda sudah membayar, tekan &ldquo;Periksa status&rdquo;
              sebentar lagi. Jika belum, buka halaman order untuk membayar.
            </p>
          </>
        ) : (
          <>
            <h1>Pembayaran sedang diverifikasi</h1>
            <p className="muted">Kami menanyakan status pembayaran setiap beberapa detik. Halaman ini akan diperbarui sendiri.</p>
          </>
        )}
        {status === 'PENDING_PAYMENT' && checkFailed && (
          <p className="muted small-note">Status belum bisa diperiksa saat ini. Coba lagi dalam beberapa saat.</p>
        )}
        <div className="actions" style={{ display: 'flex', gap: 'var(--s-3)', flexWrap: 'wrap' }}>
          {status === 'PENDING_PAYMENT' && (
            <button className="btn" type="button" onClick={() => check(orderId)} disabled={checking}>
              {checking ? 'Memeriksa...' : 'Periksa status'}
            </button>
          )}
          <Link className={status === 'PENDING_PAYMENT' ? 'btn ghost' : 'btn'} to={`/order/${o.id}`}>
            Lihat order
          </Link>
          {status === 'CANCELLED' && (
            <Link className="btn" to="/">
              Belanja lagi
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}

export function OrderStatusPage() {
  const id = Number(useParams().id);
  return Number.isFinite(id) ? <StatusView orderId={id} /> : <NotFoundView />;
}

/** Tujuan "finish redirect" Midtrans: order_id ada di query string. */
export function PaymentReturnPage() {
  const [sp] = useSearchParams();
  const id = orderIdFromProviderRef(sp.get('order_id'));
  if (id === null) {
    return <NotFoundView title="Pembayaran tidak dikenali" message="Alamat ini tidak memuat nomor pesanan. Buka riwayat order untuk melihat statusnya." />;
  }
  return <StatusView orderId={id} />;
}
