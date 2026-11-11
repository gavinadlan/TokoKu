import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { POLL_LIMIT_MS, PaymentReturnPage, REFRESH_EVERY_MS } from './PaymentStatus';

const useOrder = vi.fn();
const check = vi.fn();
const refreshState = { isPending: false, isError: false };
vi.mock('../api/queries', () => ({
  useOrder: (...a: unknown[]) => useOrder(...a),
  useRefreshPayment: () => ({ mutate: check, ...refreshState }),
}));

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
  beforeEach(() => { vi.useFakeTimers(); useOrder.mockReset(); check.mockReset(); refreshState.isError = false; });
  afterEach(() => vi.useRealTimers());

  it('menanyakan status ke backend segera dan tiap 5 detik selama PENDING_PAYMENT', () => {
    useOrder.mockReturnValue(order('PENDING_PAYMENT'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234&status_code=200&transaction_status=settlement');
    expect(useOrder).toHaveBeenCalledWith(7);
    expect(check).toHaveBeenCalledTimes(1);
    expect(check).toHaveBeenCalledWith(7);
    act(() => { vi.advanceTimersByTime(REFRESH_EVERY_MS * 2); });
    expect(check).toHaveBeenCalledTimes(3);
    expect(screen.getByText('Pembayaran sedang diverifikasi')).toBeInTheDocument();
  });

  it('berhenti setelah 30 detik; "Periksa status" tetap menanyakan ke backend', () => {
    useOrder.mockReturnValue(order('PENDING_PAYMENT'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234');
    act(() => { vi.advanceTimersByTime(POLL_LIMIT_MS); });
    const calls = check.mock.calls.length;
    act(() => { vi.advanceTimersByTime(REFRESH_EVERY_MS * 3); });
    expect(check).toHaveBeenCalledTimes(calls);
    expect(screen.getByText('Pembayaran belum terkonfirmasi')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Periksa status' }));
    expect(check).toHaveBeenCalledTimes(calls + 1);
  });

  it('status final tidak memicu pemeriksaan; status dari API menang atas query string', () => {
    useOrder.mockReturnValue(order('PAID'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234&transaction_status=pending');
    expect(screen.getByText('Pembayaran diterima')).toBeInTheDocument();
    expect(check).not.toHaveBeenCalled();
  });

  it('gagal memeriksa ditampilkan dengan ramah', () => {
    refreshState.isError = true;
    useOrder.mockReturnValue(order('PENDING_PAYMENT'));
    renderAt('/pembayaran/selesai?order_id=TKU-7-abcd1234');
    expect(screen.getByText(/belum bisa diperiksa saat ini/)).toBeInTheDocument();
  });

  it('order_id tidak valid tidak memanggil API', () => {
    renderAt('/pembayaran/selesai?order_id=ngawur');
    expect(useOrder).not.toHaveBeenCalled();
    expect(check).not.toHaveBeenCalled();
    expect(screen.getByText('Pembayaran tidak dikenali')).toBeInTheDocument();
  });
});
