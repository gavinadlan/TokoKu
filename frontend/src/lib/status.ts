export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'READY_FOR_PICKUP' | 'COMPLETED' | 'CANCELLED';
export type Fulfillment = 'DELIVERY' | 'PICKUP';

export const ORDER_STATUSES: OrderStatus[] = ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED'];

export const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Menunggu pembayaran',
  PAID: 'Dibayar',
  PROCESSING: 'Diproses',
  SHIPPED: 'Dikirim',
  READY_FOR_PICKUP: 'Siap diambil',
  COMPLETED: 'Selesai',
  CANCELLED: 'Dibatalkan',
};

export type StatusTone = 'pending' | 'active' | 'done' | 'cancelled';

export const STATUS_TONE: Record<OrderStatus, StatusTone> = {
  PENDING_PAYMENT: 'pending',
  PAID: 'active',
  PROCESSING: 'active',
  SHIPPED: 'active',
  READY_FOR_PICKUP: 'active',
  COMPLETED: 'done',
  CANCELLED: 'cancelled',
};

export const FULFILLMENT_LABEL: Record<Fulfillment, string> = { DELIVERY: 'Dikirim', PICKUP: 'Ambil di toko' };

/** Apa yang admin perlu isi sebelum mengubah status (dialog). */
export type AdminInput = 'tracking' | 'code' | null;

export interface AdminAction {
  to: 'PROCESSING' | 'SHIPPED' | 'READY_FOR_PICKUP' | 'COMPLETED';
  label: string;
  input: AdminInput;
}

/** Tindakan admin yang sah per jalur (sama dengan state machine backend). PAID hanya lewat pembayaran. */
export function adminNextAction(status: OrderStatus, type: Fulfillment): AdminAction | null {
  switch (status) {
    case 'PAID':
      return { to: 'PROCESSING', label: 'Proses pesanan', input: null };
    case 'PROCESSING':
      return type === 'DELIVERY'
        ? { to: 'SHIPPED', label: 'Tandai dikirim', input: 'tracking' }
        : { to: 'READY_FOR_PICKUP', label: 'Siap diambil', input: null };
    case 'SHIPPED':
      return { to: 'COMPLETED', label: 'Tandai selesai', input: null };
    case 'READY_FOR_PICKUP':
      return { to: 'COMPLETED', label: 'Selesaikan (kode)', input: 'code' };
    default:
      return null;
  }
}

export const canPay = (s: OrderStatus) => s === 'PENDING_PAYMENT';
export const canCancel = (s: OrderStatus) => s === 'PENDING_PAYMENT';
export const hasInvoice = (invoiceNumber: string | null | undefined) => Boolean(invoiceNumber);

export interface TimelineStep {
  key: OrderStatus;
  label: string;
  state: 'done' | 'now' | 'todo';
}

const PATH: Record<Fulfillment, { key: OrderStatus; label: string }[]> = {
  DELIVERY: [
    { key: 'PENDING_PAYMENT', label: 'Pesanan dibuat' },
    { key: 'PAID', label: 'Dibayar' },
    { key: 'PROCESSING', label: 'Diproses' },
    { key: 'SHIPPED', label: 'Dikirim' },
    { key: 'COMPLETED', label: 'Selesai' },
  ],
  PICKUP: [
    { key: 'PENDING_PAYMENT', label: 'Pesanan dibuat' },
    { key: 'PAID', label: 'Dibayar' },
    { key: 'PROCESSING', label: 'Diproses' },
    { key: 'READY_FOR_PICKUP', label: 'Siap diambil' },
    { key: 'COMPLETED', label: 'Selesai' },
  ],
};

/** Linimasa per jalur. Order dibatalkan tidak punya linimasa (null): ditampilkan sebagai pesan tersendiri. */
export function timelineSteps(type: Fulfillment, status: OrderStatus): TimelineStep[] | null {
  if (status === 'CANCELLED') return null;
  const path = PATH[type];
  const current = path.findIndex((s) => s.key === status);
  return path.map((s, i) => ({
    key: s.key,
    label: s.label,
    state: i < current ? 'done' : i === current ? (status === 'COMPLETED' ? 'done' : 'now') : 'todo',
  }));
}
