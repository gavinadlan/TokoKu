import { useState } from 'react';
import { Link, useLocation, useParams } from 'react-router';
import { useCancelOrder, useOrder, useOrders, useStore } from '../api/queries';
import { usePayAndRedirect } from '../api/usePayment';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Pager } from '../components/Pager';
import { InvoiceButtons } from '../components/InvoiceButtons';
import { OrderTimeline } from '../components/OrderTimeline';
import { Receipt } from '../components/Receipt';
import { EmptyState, ErrorState, ListSkeleton } from '../components/States';
import { StatusLabel } from '../components/StatusLabel';
import { messageOf } from '../lib/errors';
import { formatDate, formatDateTime, formatRupiah } from '../lib/format';
import { canCancel, canPay, FULFILLMENT_LABEL, type Fulfillment, type OrderStatus } from '../lib/status';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { NotFoundView } from './NotFound';
import { ApiError } from '../lib/errors';

export function OrdersPage() {
  useDocumentTitle('Riwayat order');
  const [page, setPage] = useState(1);
  const orders = useOrders(page);

  return (
    <div className="wrap page-pad">
      <h1 className="page-title" style={{ marginBottom: 'var(--s-6)' }}>
        Riwayat order
      </h1>
      {orders.isPending ? (
        <ListSkeleton />
      ) : orders.isError ? (
        <ErrorState error={orders.error} onRetry={() => orders.refetch()} title="Riwayat order belum bisa dimuat" />
      ) : orders.data.content.length === 0 ? (
        <EmptyState title="Belum ada pesanan" actions={<Link className="btn" to="/">Mulai belanja</Link>}>
          Pesanan yang Anda buat akan muncul di sini, lengkap dengan statusnya.
        </EmptyState>
      ) : (
        <>
          <ul className="orders">
            {orders.data.content.map((o) => (
              <li key={o.id}>
                <Link to={`/order/${o.id}`}>
                  <div>
                    <div className="oid">Order No. {String(o.id).padStart(4, '0')}</div>
                    <div className="muted tnum" style={{ fontSize: 'var(--text-sm)' }}>
                      {formatDate(o.createdAt)} &middot; {o.items.reduce((n, i) => n + i.quantity, 0)} barang &middot; {FULFILLMENT_LABEL[o.fulfillmentType as Fulfillment]}
                    </div>
                  </div>
                  <div className="right">
                    <b className="tnum">{formatRupiah(o.totalAmount)}</b>
                    <StatusLabel status={o.status as OrderStatus} />
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          <Pager page={page} totalPages={orders.data.page.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}

export function OrderDetailPage() {
  const id = Number(useParams().id);
  const order = useOrder(id);
  const store = useStore();
  const payment = usePayAndRedirect();
  const cancel = useCancelOrder();
  const [confirming, setConfirming] = useState(false);
  const notice = (useLocation().state as { notice?: string } | null)?.notice;
  useDocumentTitle(`Order No. ${String(id).padStart(4, '0')}`);

  if (order.isPending) return <div className="wrap page-pad"><ListSkeleton /></div>;
  if (order.isError) {
    if (order.error instanceof ApiError && order.error.status === 404) return <NotFoundView title="Order tidak ditemukan" message="Order ini tidak ada di akun Anda." />;
    return <div className="wrap"><ErrorState error={order.error} onRetry={() => order.refetch()} title="Order belum bisa dimuat" /></div>;
  }
  const o = order.data;
  const status = o.status as OrderStatus;
  const actionError = payment.error ?? cancel.error;

  return (
    <div className="wrap page-pad">
      <nav className="crumbs" aria-label="Jejak halaman" style={{ paddingTop: 0 }}>
        <Link to="/order">Riwayat order</Link> / <span aria-current="page">No. {String(o.id).padStart(4, '0')}</span>
      </nav>
      <div className="row-between" style={{ alignItems: 'end', flexWrap: 'wrap', marginBottom: 'var(--s-6)' }}>
        <h1 className="page-title">Order No. {String(o.id).padStart(4, '0')}</h1>
        <StatusLabel status={status} />
      </div>
      {notice && <div className="alert warn" style={{ marginBottom: 'var(--s-5)' }}>{notice}</div>}
      {actionError && (
        <div className="alert err" role="alert" style={{ marginBottom: 'var(--s-5)' }}>
          {messageOf(actionError)}
        </div>
      )}
      <div className="layout-2">
        <div className="stack">
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 440, fontSize: 'var(--text-xl)' }}>Status pesanan</h2>
          <OrderTimeline order={o} store={store.data} />
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 440, fontSize: 'var(--text-xl)', marginTop: 'var(--s-6)' }}>
            {o.fulfillmentType === 'PICKUP' ? 'Pengambil' : 'Pengiriman'}
          </h2>
          <p>
            {o.shippingName}
            <br />
            <span className="muted tnum">{o.shippingPhone}</span>
            {o.shippingAddress && (
              <>
                <br />
                <span className="muted" style={{ whiteSpace: 'pre-line' }}>{o.shippingAddress}</span>
              </>
            )}
          </p>
          <p className="muted tnum" style={{ fontSize: 'var(--text-sm)' }}>Dibuat {formatDateTime(o.createdAt)}</p>
          {status === 'PENDING_PAYMENT' && (
            <p className="muted" style={{ fontSize: 'var(--text-sm)' }}>
              Pesanan yang belum dibayar akan dibatalkan otomatis dan stoknya dikembalikan.
            </p>
          )}
        </div>
        <aside>
          <Receipt
            title="Rincian"
            items={o.items.map((i) => ({ name: i.productName, qty: i.quantity, subtotal: i.subtotal }))}
            rows={[
              { label: 'Subtotal', value: formatRupiah(o.subtotal) },
              o.fulfillmentType === 'PICKUP'
                ? { label: 'Ambil di toko', value: <span className="free">Gratis</span> }
                : { label: 'Biaya kirim', value: o.shippingFee === 0 ? <span className="free">Gratis</span> : formatRupiah(o.shippingFee) },
            ]}
            total={o.totalAmount}
            actions={
              <>
                {o.invoiceNumber && <InvoiceButtons orderId={o.id} number={o.invoiceNumber} realm="store" />}
                {canPay(status) && (
                  <button className="btn block" type="button" disabled={payment.pending} onClick={() => payment.start(o.id).catch(() => undefined)}>
                    {payment.pending ? 'Mengalihkan...' : 'Bayar sekarang'}
                  </button>
                )}
                {status !== 'PENDING_PAYMENT' && status !== 'CANCELLED' && (
                  <Link className="btn ghost block" to={`/order/${o.id}/status`}>
                    Lihat status pembayaran
                  </Link>
                )}
                {canCancel(status) && (
                  <button className="btn ghost block" type="button" onClick={() => setConfirming(true)}>
                    Batalkan pesanan
                  </button>
                )}
              </>
            }
          />
        </aside>
      </div>
      <ConfirmDialog
        open={confirming}
        title="Batalkan pesanan ini?"
        confirmLabel="Ya, batalkan"
        cancelLabel="Tidak jadi"
        danger
        busy={cancel.isPending}
        onClose={() => setConfirming(false)}
        onConfirm={() => cancel.mutate(o.id, { onSettled: () => setConfirming(false) })}
      >
        Barang akan dikembalikan ke stok dan pesanan tidak bisa dibayar lagi.
      </ConfirmDialog>
    </div>
  );
}
