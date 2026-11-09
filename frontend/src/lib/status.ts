export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'PROCESSING' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED';

export const ORDER_STATUSES: OrderStatus[] = ['PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED'];

export const STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING_PAYMENT: 'Menunggu pembayaran',
  PAID: 'Dibayar',
  PROCESSING: 'Diproses',
  SHIPPED: 'Dikirim',
  COMPLETED: 'Selesai',
  CANCELLED: 'Dibatalkan',
};

export type StatusTone = 'pending' | 'active' | 'done' | 'cancelled';

export const STATUS_TONE: Record<OrderStatus, StatusTone> = {
  PENDING_PAYMENT: 'pending',
  PAID: 'active',
  PROCESSING: 'active',
  SHIPPED: 'active',
  COMPLETED: 'done',
  CANCELLED: 'cancelled',
};

/** Transisi yang boleh dilakukan admin (sama dengan aturan backend). PAID hanya lewat pembayaran. */
const ADMIN_NEXT: Partial<Record<OrderStatus, { to: OrderStatus; label: string }>> = {
  PAID: { to: 'PROCESSING', label: 'Proses pesanan' },
  PROCESSING: { to: 'SHIPPED', label: 'Tandai dikirim' },
  SHIPPED: { to: 'COMPLETED', label: 'Tandai selesai' },
};

export function adminNextAction(status: OrderStatus) {
  return ADMIN_NEXT[status] ?? null;
}

export const canPay = (s: OrderStatus) => s === 'PENDING_PAYMENT';
export const canCancel = (s: OrderStatus) => s === 'PENDING_PAYMENT';
