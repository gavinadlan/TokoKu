import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { POLL_LIMIT_MS, PaymentReturnPage } from './PaymentStatus';

const useOrder = vi.fn();
vi.mock('../api/queries', () => ({ useOrder: (...a: unknown[]) => useOrder(...a) }));

function order(status: string) {
  return { isPending: false, isError: false, isFetching: false, refetch: vi.fn(),
    data: { id: 7, status, totalAmount: 85000, fulfillmentType: 'DELIVERY' } };
}

function renderAt(url: string) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[url]}><PaymentReturnPage /></MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('PaymentReturnPage (finish redirect Midtrans)', () => {
  beforeEach(() => { vi.useFakeTimers(); useOrder.mockReset(); });
  afterEach(() => vi.useRealTimers());

  it('mengambil order dari order_id dan polling saat masih PENDING_PAYMENT', () => {
    useOrder.mockReturnValue(order('PENDING_PAYMENT'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234&status_code=200&transaction_status=settlement');
    expect(useOrder).toHaveBeenCalledWith(7, { poll: true });
    expect(screen.getByText('Pembayaran sedang diverifikasi')).toBeInTheDocument();
  });

  it('berhenti polling setelah 30 detik dan menawarkan periksa manual', () => {
    useOrder.mockReturnValue(order('PENDING_PAYMENT'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234');
    act(() => { vi.advanceTimersByTime(POLL_LIMIT_MS); });
    expect(useOrder).toHaveBeenLastCalledWith(7, { poll: false });
    expect(screen.getByText('Pembayaran belum terkonfirmasi')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Periksa status' })).toBeInTheDocument();
  });

  it('status dari API menang atas query string: PAID tampil diterima', () => {
    useOrder.mockReturnValue(order('PAID'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234&transaction_status=pending');
    expect(screen.getByText('Pembayaran diterima')).toBeInTheDocument();
  });

  it('order_id tidak valid tidak memanggil API', () => {
    renderAt('/pembayaran/selesai?order_id=ngawur');
    expect(useOrder).not.toHaveBeenCalled();
    expect(screen.getByText('Pembayaran tidak dikenali')).toBeInTheDocument();
  });
});
