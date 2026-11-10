import { useQueryClient } from '@tanstack/react-query';
import { Link, useParams, useSearchParams } from 'react-router';
import { useOrder } from '../api/queries';
import { ErrorState, ListSkeleton } from '../components/States';
import { formatRupiah } from '../lib/format';
import { orderIdFromProviderRef } from '../lib/payment';
import type { OrderStatus } from '../lib/status';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { NotFoundView } from './NotFound';

/**
 * Halaman tujuan setelah bayar. Status order berubah lewat webhook Midtrans, jadi diperiksa berkala.
 * Di lokal webhook tidak bisa menjangkau localhost, sehingga ada tombol "Periksa status".
 */
function StatusView({ orderId }: { orderId: number }) {
  const order = useOrder(orderId, { poll: true });
  const qc = useQueryClient();
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
              {o.fulfillmentType === 'PICKUP' && ' Kode ambil ada di halaman order.'}
            </p>
          </>
        ) : status === 'CANCELLED' ? (
          <>
            <h1>Pembayaran tidak berhasil</h1>
            <p className="muted">
              Pembayaran ditolak atau sudah kedaluwarsa, jadi pesanan dibatalkan dan barangnya dikembalikan ke stok. Anda bisa memesan lagi kapan saja.
            </p>
          </>
        ) : (
          <>
            <h1>Menunggu konfirmasi pembayaran</h1>
            <p className="muted">
              Kami memeriksa status setiap beberapa detik. Konfirmasi dari bank atau dompet digital kadang butuh waktu satu sampai beberapa menit.
              Di lingkungan lokal, notifikasi pembayaran tidak bisa sampai ke server, jadi status mungkin tidak berubah sendiri; tekan &ldquo;Periksa status&rdquo; untuk memeriksa ulang.
            </p>
          </>
        )}
        <div className="actions" style={{ display: 'flex', gap: 'var(--s-3)', flexWrap: 'wrap' }}>
          {status === 'PENDING_PAYMENT' && (
            <button className="btn" type="button" onClick={() => qc.invalidateQueries({ queryKey: ['order', orderId] })} disabled={order.isFetching}>
              {order.isFetching ? 'Memeriksa...' : 'Periksa status'}
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
