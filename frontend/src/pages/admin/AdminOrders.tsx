import { useState } from 'react';
import { useAdminOrders, useAdminOrderStatus } from '../../api/queries';
import { Field } from '../../components/Field';
import { Pager } from '../../components/Pager';
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States';
import { StatusLabel } from '../../components/StatusLabel';
import { messageOf } from '../../lib/errors';
import { formatDateTime, formatRupiah } from '../../lib/format';
import { adminNextAction, ORDER_STATUSES, STATUS_LABEL, type OrderStatus } from '../../lib/status';
import { useDocumentTitle } from '../../lib/useDocumentTitle';

export function AdminOrdersPage() {
  useDocumentTitle('Kelola order');
  const [status, setStatus] = useState<OrderStatus | ''>('');
  const [page, setPage] = useState(1);
  const orders = useAdminOrders({ status: status || null, page });
  const change = useAdminOrderStatus();

  return (
    <>
      <h1>Order</h1>
      <div className="inline-form">
        <Field label="Status">
          <select className="input" value={status} onChange={(e) => { setStatus(e.target.value as OrderStatus | ''); setPage(1); }}>
            <option value="">Semua status</option>
            {ORDER_STATUSES.map((s) => (
              <option key={s} value={s}>{STATUS_LABEL[s]}</option>
            ))}
          </select>
        </Field>
      </div>

      {change.error && <div className="alert err" role="alert">{messageOf(change.error)}</div>}

      {orders.isPending ? (
        <ListSkeleton />
      ) : orders.isError ? (
        <ErrorState error={orders.error} onRetry={() => orders.refetch()} />
      ) : orders.data.content.length === 0 ? (
        <EmptyState title="Tidak ada order">Belum ada order dengan status ini.</EmptyState>
      ) : (
        <>
          <div className="table-wrap" tabIndex={0} role="region" aria-label="Daftar order">
            <table className="data">
              <thead>
                <tr><th>No.</th><th>Penerima</th><th>Dibuat</th><th className="num">Total</th><th>Status</th><th><span className="sr-only">Tindakan</span></th></tr>
              </thead>
              <tbody>
                {orders.data.content.map((o) => {
                  const next = adminNextAction(o.status as OrderStatus);
                  return (
                    <tr key={o.id}>
                      <td className="tnum">{String(o.id).padStart(4, '0')}</td>
                      <td>
                        <details>
                          <summary style={{ cursor: 'pointer', minHeight: 32 }}>{o.shippingName}</summary>
                          <div className="muted" style={{ fontSize: 'var(--text-xs)', paddingBlock: 'var(--s-2)' }}>
                            <div className="tnum">{o.shippingPhone}</div>
                            <div style={{ whiteSpace: 'pre-line' }}>{o.shippingAddress}</div>
                            <ul style={{ margin: 'var(--s-2) 0 0', paddingLeft: 'var(--s-4)' }}>
                              {o.items.map((i, idx) => (
                                <li key={idx}>{i.quantity} x {i.productName}</li>
                              ))}
                            </ul>
                          </div>
                        </details>
                      </td>
                      <td className="tnum muted">{formatDateTime(o.createdAt)}</td>
                      <td className="num">{formatRupiah(o.totalAmount)}</td>
                      <td><StatusLabel status={o.status as OrderStatus} /></td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        {next ? (
                          <button type="button" className="btn small" disabled={change.isPending} onClick={() => change.mutate({ id: o.id, status: next.to as 'PROCESSING' | 'SHIPPED' | 'COMPLETED' })}>
                            {next.label}
                          </button>
                        ) : (
                          <span className="muted" style={{ fontSize: 'var(--text-xs)' }}>{o.status === 'PENDING_PAYMENT' ? 'Menunggu pelanggan' : 'Tidak ada tindakan'}</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
          <Pager page={page} totalPages={orders.data.page.totalPages} onChange={setPage} />
        </>
      )}
    </>
  );
}
