import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import type { Order } from '../../api/types';
import { adminNextAction } from '../../lib/status';
import { OrderCard } from './OrderCard';

const order = (over: Partial<Order> = {}): Order => ({
  id: 63, status: 'PAID', fulfillmentType: 'PICKUP', totalAmount: 176000, createdAt: '2026-11-12T15:50:00Z', invoicedAt: '2026-11-12T15:55:00Z',
  shippingName: 'Andi Pratama', shippingPhone: '0812-5550-0101', shippingAddress: null, courier: null, trackingNumber: null, invoiceNumber: 'INV-1',
  items: [{ productName: 'Kopi Gayo', quantity: 2 }],
  ...over,
} as unknown as Order);

const renderCard = (o: Order, extra: Partial<Parameters<typeof OrderCard>[0]> = {}) => {
  const onAction = vi.fn();
  render(<ul><OrderCard order={o} isNew={false} action={adminNextAction(o.status as never, o.fulfillmentType as never)} busy={false} onAction={onAction} {...extra} /></ul>);
  return onAction;
};

describe('OrderCard', () => {
  it('menampilkan nomor, waktu ringkas, penerima, total, status dan jenis berdampingan', () => {
    renderCard(order());
    const card = document.querySelector('li.ocard') as HTMLElement;
    expect(within(card).getByText('Order 0063')).toBeInTheDocument();
    expect(within(card).getByText(/^12 Nov, \d\d\.\d\d$/)).toBeInTheDocument();
    expect(within(card).getByText('Andi Pratama')).toBeInTheDocument();
    expect(within(card).getByText(/Rp\s176\.000/)).toBeInTheDocument();
    expect(within(card).getByText('Dibayar')).toHaveClass('badge');
    expect(within(card).getByText('Ambil di toko')).toHaveClass('badge-accent');
  });
  it('tombol utama bernama aksesibel dengan nomor order dan memanggil tindakan', () => {
    const onAction = renderCard(order());
    fireEvent.click(screen.getByRole('button', { name: 'Proses pesanan, order 0063' }));
    expect(onAction).toHaveBeenCalledTimes(1);
    expect(onAction.mock.calls[0]?.[1]).toMatchObject({ to: 'PROCESSING' });
  });
  it('tindakan siap diambil untuk order ambil di toko yang diproses', () => {
    renderCard(order({ status: 'PROCESSING' }));
    expect(screen.getByRole('button', { name: 'Siap diambil, order 0063' })).toBeInTheDocument();
  });
  it('tanpa tindakan: tidak ada tombol utama', () => {
    renderCard(order({ status: 'COMPLETED' }));
    expect(screen.queryByRole('button', { name: /order 0063$/ })).toBeNull();
  });
  it('order baru ditandai titik dan sorotan', () => {
    renderCard(order(), { isNew: true });
    expect(screen.getByRole('img', { name: 'Baru dibayar' })).toBeInTheDocument();
    expect(document.querySelector('li.ocard')).toHaveClass('is-new');
  });
  it('tombol dinonaktifkan saat sibuk', () => {
    renderCard(order(), { busy: true });
    expect(screen.getByRole('button', { name: 'Proses pesanan, order 0063' })).toBeDisabled();
  });
});
