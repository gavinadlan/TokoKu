import { useState } from 'react';
import type { Order } from '../api/types';
import { formatDateTimeShort } from '../lib/format';
import { timelineSteps, type Fulfillment, type OrderStatus } from '../lib/status';

/** Linimasa status per jalur (kirim / ambil), dengan resi dan kode ambil di langkah yang relevan. */
export function OrderTimeline({ order, store }: { order: Order; store?: { name: string; address: string; openingHours: string } | null }) {
  const type = order.fulfillmentType as Fulfillment;
  const status = order.status as OrderStatus;
  const steps = timelineSteps(type, status);
  const [copied, setCopied] = useState(false);

  if (!steps) {
    return (
      <div className="alert err" role="status">
        <b>Dibatalkan.</b> Pembayaran tidak berhasil atau pesanan dibatalkan, dan stok sudah dikembalikan. Tidak ada langkah berikutnya.
      </div>
    );
  }

  const times: Partial<Record<OrderStatus, string | null | undefined>> = {
    PENDING_PAYMENT: order.createdAt,
    PAID: order.invoicedAt,
    READY_FOR_PICKUP: order.pickupReadyAt,
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(order.trackingNumber ?? '');
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      /* izin papan klip ditolak: nomor tetap terlihat untuk disalin manual */
    }
  };

  return (
    <ol className="tl" aria-label="Linimasa pesanan">
      {steps.map((s) => (
        <li key={s.key} className={s.state} aria-current={s.state === 'now' ? 'step' : undefined}>
          <div className="row1">
            <span className="st">{s.label}</span>
            {times[s.key] && s.state !== 'todo' && <time dateTime={times[s.key]!}>{formatDateTimeShort(times[s.key]!)}</time>}
          </div>
          {s.key === 'PENDING_PAYMENT' && s.state === 'now' && <p className="tl-detail">Menunggu pembayaran.</p>}
          {s.key === 'SHIPPED' && s.state !== 'todo' && order.trackingNumber && (
            <div className="lab">
              <small>Kurir dan nomor resi</small>
              <div className="resi">
                <span>{order.courier}</span>
                <b className="tnum">{order.trackingNumber}</b>
                <button type="button" className="btn ghost small" onClick={copy}>
                  {copied ? 'Tersalin' : 'Salin nomor resi'}
                </button>
              </div>
            </div>
          )}
          {s.key === 'READY_FOR_PICKUP' && s.state !== 'todo' && (
            <div className="lab">
              <small>Kode ambil, tunjukkan ke kasir</small>
              <span className="code" aria-label={`Kode ambil ${order.pickupCode?.split('').join(' ')}`}>{order.pickupCode}</span>
              {store && (
                <small>
                  {store.name}, {store.address}
                  <br />
                  {store.openingHours}
                </small>
              )}
            </div>
          )}
        </li>
      ))}
    </ol>
  );
}
