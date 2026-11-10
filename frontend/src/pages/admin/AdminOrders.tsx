import { useState } from 'react';
import { useAdminOrders, useAdminOrderStatus } from '../../api/queries';
import type { Order } from '../../api/types';
import { FormDialog } from '../../components/FormDialog';
import { InvoiceButtons } from '../../components/InvoiceButtons';
import { Pager } from '../../components/Pager';
import { Select } from '../../components/Select';
import { SortHeader, nextSort, type SortDir } from '../../components/SortHeader';
import { EmptyState, ErrorState, ListSkeleton } from '../../components/States';
import { StatusLabel } from '../../components/StatusLabel';
import { useToast } from '../../components/Toast';
import { messageOf } from '../../lib/errors';
import { formatDateTimeShort, formatRupiah } from '../../lib/format';
import { adminNextAction, FULFILLMENT_LABEL, ORDER_STATUSES, STATUS_LABEL, type AdminAction, type Fulfillment, type OrderStatus } from '../../lib/status';
import { useDocumentTitle } from '../../lib/useDocumentTitle';
import { AdminPage } from './AdminShell';

type OrderSort = 'id,desc' | 'id,asc' | 'createdAt,desc' | 'createdAt,asc' | 'totalAmount,desc' | 'totalAmount,asc';

export function AdminOrdersPage() {
  useDocumentTitle('Order | TokoKu Admin');
  const [status, setStatus] = useState<OrderStatus | 'all'>('all');
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState<{ field: string; dir: SortDir }>({ field: 'createdAt', dir: 'desc' });
  const orders = useAdminOrders({ status: status === 'all' ? null : status, page, sort: `${sort.field},${sort.dir}` as OrderSort });
  const change = useAdminOrderStatus();
  const toast = useToast();
  const [dialog, setDialog] = useState<{ order: Order; action: AdminAction } | null>(null);
  const [courier, setCourier] = useState('');
  const [tracking, setTracking] = useState('');
  const [code, setCode] = useState('');
  const [dialogError, setDialogError] = useState<string | null>(null);

  const onSort = (field: string) => { setSort((s) => nextSort(s, field, field === 'createdAt' || field === 'id' ? 'desc' : 'asc')); setPage(1); };

  const apply = (order: Order, action: AdminAction, extra: { courier?: string; trackingNumber?: string; pickupCode?: string } = {}) => {
    change.mutate({ id: order.id, status: action.to, ...extra }, {
      onSuccess: () => { toast.notify(`Order ${String(order.id).padStart(4, '0')}: ${action.label.toLowerCase()} berhasil.`); setDialog(null); },
      onError: (e) => { if (dialog) setDialogError(messageOf(e)); },
    });
  };

  const start = (order: Order, action: AdminAction) => {
    if (action.input === null) return apply(order, action);
    setCourier(''); setTracking(''); setCode(''); setDialogError(null);
    setDialog({ order, action });
  };

  return (
    <AdminPage title="Order" sub="Kelola pengiriman dan pengambilan di toko">
      <div className="toolrow">
        <div className="fld">
          <label htmlFor="o-status">Status</label>
          <Select id="o-status" value={status} onChange={(v) => { setStatus(v as OrderStatus | 'all'); setPage(1); }} options={[{ value: 'all', label: 'Semua status' }, ...ORDER_STATUSES.map((s) => ({ value: s, label: STATUS_LABEL[s] }))]} />
        </div>
      </div>

      {change.error && !dialog && <div className="alert err" role="alert">{messageOf(change.error)}</div>}

      {orders.isPending ? (
        <ListSkeleton />
      ) : orders.isError ? (
        <ErrorState error={orders.error} onRetry={() => orders.refetch()} />
      ) : orders.data.content.length === 0 ? (
        <EmptyState title="Tidak ada order">Belum ada order dengan status ini.</EmptyState>
      ) : (
        <>
          <div className="tablebox" tabIndex={0} role="region" aria-label="Daftar order">
            <table className="tbl" style={{ minWidth: 880 }}>
              <thead>
                <tr>
                  <SortHeader label="No." field="id" sort={sort} onSort={onSort} className="sticky-col" />
                  <SortHeader label="Dibuat" field="createdAt" sort={sort} onSort={onSort} />
                  <th scope="col">Penerima</th>
                  <th scope="col">Pengambilan</th>
                  <SortHeader label="Total" field="totalAmount" sort={sort} onSort={onSort} numeric />
                  <th scope="col">Status</th>
                  <th scope="col"><span className="sr-only">Tindakan</span></th>
                </tr>
              </thead>
              <tbody>
                {orders.data.content.map((o) => {
                  const action = adminNextAction(o.status as OrderStatus, o.fulfillmentType as Fulfillment);
                  return (
                    <tr key={o.id}>
                      <td className="id sticky-col tnum">{String(o.id).padStart(4, '0')}</td>
                      <td className="tnum muted">{formatDateTimeShort(o.createdAt)}</td>
                      <td>
                        <details>
                          <summary>{o.shippingName}</summary>
                          <div className="muted rowdetail">
                            <div className="tnum">{o.shippingPhone}</div>
                            {o.shippingAddress && <div style={{ whiteSpace: 'pre-line' }}>{o.shippingAddress}</div>}
                            {o.trackingNumber && <div>Resi {o.courier} <span className="tnum">{o.trackingNumber}</span></div>}
                            <ul>{o.items.map((i, idx) => <li key={idx}>{i.quantity} x {i.productName}</li>)}</ul>
                          </div>
                        </details>
                      </td>
                      <td><span className={`chip-s ${o.fulfillmentType === 'PICKUP' ? 'pk' : ''}`}>{FULFILLMENT_LABEL[o.fulfillmentType as Fulfillment]}</span></td>
                      <td className="n">{formatRupiah(o.totalAmount)}</td>
                      <td><StatusLabel status={o.status as OrderStatus} /></td>
                      <td className="act-cell">
                        {action ? (
                          <button type="button" className="btn sm" disabled={change.isPending} onClick={() => start(o, action)}>{action.label}</button>
                        ) : (
                          <span className="muted small-note">{o.status === 'PENDING_PAYMENT' ? 'Menunggu pembayaran' : 'Tidak ada tindakan'}</span>
                        )}
                        {o.invoiceNumber && <InvoiceButtons orderId={o.id} number={o.invoiceNumber} realm="admin" compact />}
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

      <FormDialog
        open={dialog !== null}
        title={dialog ? (dialog.action.input === 'tracking' ? `Tandai dikirim: order ${String(dialog.order.id).padStart(4, '0')}` : `Selesaikan pengambilan: order ${String(dialog.order.id).padStart(4, '0')}`) : ''}
        submitLabel={dialog?.action.input === 'tracking' ? 'Simpan dan tandai dikirim' : 'Verifikasi dan selesaikan'}
        busy={change.isPending}
        error={dialogError}
        onClose={() => setDialog(null)}
        onSubmit={() => dialog && apply(dialog.order, dialog.action, dialog.action.input === 'tracking' ? { courier: courier.trim(), trackingNumber: tracking.trim() } : { pickupCode: code })}
      >
        {dialog?.action.input === 'tracking' ? (
          <>
            <div className="field"><label htmlFor="d-courier">Kurir</label><input id="d-courier" className="input" value={courier} onChange={(e) => setCourier(e.target.value)} required maxLength={50} placeholder="mis. JNE" /></div>
            <div className="field"><label htmlFor="d-resi">Nomor resi</label><input id="d-resi" className="input tnum" value={tracking} onChange={(e) => setTracking(e.target.value)} required maxLength={60} /></div>
          </>
        ) : (
          <div className="field">
            <label htmlFor="d-code">Kode ambil dari pembeli</label>
            <input id="d-code" className="input code-input" value={code} onChange={(e) => setCode(e.target.value)} required maxLength={20} autoComplete="off" autoCapitalize="characters" spellCheck={false} />
            <span className="hint">Huruf besar atau kecil sama saja. Pembeli bisa melihat kodenya di halaman order.</span>
          </div>
        )}
      </FormDialog>
    </AdminPage>
  );
}
