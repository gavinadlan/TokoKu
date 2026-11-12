import type { Order } from '../../api/types';
import { Badge } from '../../components/Badge';
import { InvoiceButtons } from '../../components/InvoiceButtons';
import { StatusLabel } from '../../components/StatusLabel';
import { formatDateTimeCompact, formatRupiah } from '../../lib/format';
import { FULFILLMENT_LABEL, type AdminAction, type Fulfillment, type OrderStatus } from '../../lib/status';

interface Props {
  order: Order;
  isNew: boolean;
  action: AdminAction | null;
  busy: boolean;
  onAction(order: Order, action: AdminAction): void;
}

/** Order sebagai kartu (ponsel). Isi sama dengan baris tabel: nomor, waktu, penerima, status, jenis, total, tindakan utama. */
export function OrderCard({ order: o, isNew, action, busy, onAction }: Props) {
  const no = String(o.id).padStart(4, '0');
  return (
    <li className={`ocard${isNew ? ' is-new' : ''}`}>
      <div className="ocard-top">
        <span className="ocard-no tnum">
          Order {no}
          {isNew && <span className="new-dot" role="img" aria-label="Baru dibayar" title="Baru dibayar" />}
        </span>
        <time className="muted tnum" dateTime={o.createdAt}>{formatDateTimeCompact(o.createdAt)}</time>
      </div>
      <div className="ocard-main">
        <details>
          <summary>{o.shippingName}</summary>
          <div className="muted rowdetail">
            <div className="tnum">{o.shippingPhone}</div>
            {o.shippingAddress && <div style={{ whiteSpace: 'pre-line' }}>{o.shippingAddress}</div>}
            {o.trackingNumber && <div>Resi {o.courier} <span className="tnum">{o.trackingNumber}</span></div>}
            <ul>{o.items.map((i, idx) => <li key={idx}>{i.quantity} x {i.productName}</li>)}</ul>
          </div>
        </details>
        <span className="ocard-total tnum">{formatRupiah(o.totalAmount)}</span>
      </div>
      <div className="ocard-badges">
        <StatusLabel status={o.status as OrderStatus} />
        <Badge tone={o.fulfillmentType === 'PICKUP' ? 'accent' : 'neutral'}>{FULFILLMENT_LABEL[o.fulfillmentType as Fulfillment]}</Badge>
      </div>
      {action && (
        <button type="button" className="btn block" disabled={busy} aria-label={`${action.label}, order ${no}`} onClick={() => onAction(o, action)}>{action.label}</button>
      )}
      {o.invoiceNumber && <InvoiceButtons orderId={o.id} number={o.invoiceNumber} realm="admin" compact />}
    </li>
  );
}
