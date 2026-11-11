import { adminNextAction, canCancel, canPay, ORDER_STATUSES, STATUS_LABEL, timelineSteps } from './status';

describe('status order', () => {
  it('jalur kirim: PAID -> PROCESSING -> SHIPPED (butuh resi) -> COMPLETED', () => {
    expect(adminNextAction('PAID', 'DELIVERY')?.to).toBe('PROCESSING');
    expect(adminNextAction('PROCESSING', 'DELIVERY')).toMatchObject({ to: 'SHIPPED', input: 'tracking' });
    expect(adminNextAction('SHIPPED', 'DELIVERY')).toMatchObject({ to: 'COMPLETED', input: null });
    expect(adminNextAction('READY_FOR_PICKUP', 'DELIVERY')).toMatchObject({ to: 'COMPLETED' }); // tidak dicapai jalur kirim
  });
  it('jalur ambil: PROCESSING -> READY_FOR_PICKUP -> COMPLETED (butuh kode)', () => {
    expect(adminNextAction('PROCESSING', 'PICKUP')).toMatchObject({ to: 'READY_FOR_PICKUP', input: null });
    expect(adminNextAction('READY_FOR_PICKUP', 'PICKUP')).toMatchObject({ to: 'COMPLETED', input: 'code' });
  });
  it('tidak ada tindakan untuk menunggu pembayaran, selesai, dan dibatalkan', () => {
    for (const t of ['DELIVERY', 'PICKUP'] as const) {
      expect(adminNextAction('PENDING_PAYMENT', t)).toBeNull();
      expect(adminNextAction('COMPLETED', t)).toBeNull();
      expect(adminNextAction('CANCELLED', t)).toBeNull();
    }
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

describe('timelineSteps', () => {
  it('jalur kirim memuat Dikirim, jalur ambil memuat Siap diambil', () => {
    expect(timelineSteps('DELIVERY', 'PAID')?.map((s) => s.label)).toEqual(['Pesanan dibuat', 'Dibayar', 'Diproses', 'Dikirim', 'Selesai']);
    expect(timelineSteps('PICKUP', 'PAID')?.map((s) => s.label)).toEqual(['Pesanan dibuat', 'Dibayar', 'Diproses', 'Siap diambil', 'Selesai']);
  });
  it('menandai langkah selesai, sekarang, dan berikutnya', () => {
    expect(timelineSteps('DELIVERY', 'SHIPPED')?.map((s) => s.state)).toEqual(['done', 'done', 'done', 'now', 'todo']);
    expect(timelineSteps('PICKUP', 'READY_FOR_PICKUP')?.map((s) => s.state)).toEqual(['done', 'done', 'done', 'now', 'todo']);
    expect(timelineSteps('DELIVERY', 'PENDING_PAYMENT')?.map((s) => s.state)).toEqual(['now', 'todo', 'todo', 'todo', 'todo']);
  });
  it('order selesai: semua langkah selesai', () => {
    expect(timelineSteps('PICKUP', 'COMPLETED')?.every((s) => s.state === 'done')).toBe(true);
  });
  it('order dibatalkan tidak punya linimasa', () => {
    expect(timelineSteps('DELIVERY', 'CANCELLED')).toBeNull();
  });
});
