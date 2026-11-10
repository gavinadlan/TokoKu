import type { ReactNode } from 'react';
import { formatRupiah } from '../lib/format';

export interface ReceiptRow {
  label: string;
  value: ReactNode;
}

/** Ringkasan bergaya struk (motif label kemasan): baris dengan garis titik, total di bawah garis tegas. */
export function Receipt({ title, items, rows, total, totalLabel = 'Total', note, actions }: {
  title: string;
  items?: { name: string; qty: number; subtotal: number }[];
  rows?: ReceiptRow[];
  total: number;
  totalLabel?: string;
  note?: string;
  actions?: ReactNode;
}) {
  return (
    <section className="receipt" aria-label={title}>
      <h2>{title}</h2>
      {items && (
        <ul className="items">
          {items.map((i, idx) => (
            <li key={idx}>
              <span>{i.name}</span>
              <span className="tnum">{formatRupiah(i.subtotal)}</span>
              <span className="q tnum">{i.qty} x</span>
            </li>
          ))}
        </ul>
      )}
      {rows && (
        <dl>
          {rows.map((r) => (
            <div className="r" key={r.label}>
              <dt>{r.label}</dt>
              <dd>{r.value}</dd>
            </div>
          ))}
        </dl>
      )}
      <div className="total">
        <span>{totalLabel}</span>
        <b>{formatRupiah(total)}</b>
      </div>
      {note && <p className="note">{note}</p>}
      {actions && <div className="actions">{actions}</div>}
    </section>
  );
}
