import { adminNextAction, canCancel, canPay, STATUS_LABEL, ORDER_STATUSES } from './status';

describe('status order', () => {
  it('admin hanya memajukan PAID -> PROCESSING -> SHIPPED -> COMPLETED', () => {
    expect(adminNextAction('PAID')?.to).toBe('PROCESSING');
    expect(adminNextAction('PROCESSING')?.to).toBe('SHIPPED');
    expect(adminNextAction('SHIPPED')?.to).toBe('COMPLETED');
    expect(adminNextAction('PENDING_PAYMENT')).toBeNull();
    expect(adminNextAction('COMPLETED')).toBeNull();
    expect(adminNextAction('CANCELLED')).toBeNull();
  });
  it('bayar dan batal hanya saat menunggu pembayaran', () => {
    for (const s of ORDER_STATUSES) {
      expect(canPay(s)).toBe(s === 'PENDING_PAYMENT');
      expect(canCancel(s)).toBe(s === 'PENDING_PAYMENT');
    }
  });
  it('setiap status punya label bahasa Indonesia', () => {
    for (const s of ORDER_STATUSES) expect(STATUS_LABEL[s].length).toBeGreaterThan(3);
  });
});
