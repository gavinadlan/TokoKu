import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { adminLoginUI, apiPaidOrder, apiUser, loginUI } from './helpers';

type Flagged = { __noReload?: boolean };

test.describe('pembaruan otomatis tanpa refresh', () => {
  test('pesanan baru muncul di admin, status "siap diambil" muncul di layar pembeli', async ({ page, context, request }) => {
    test.setTimeout(120_000);
    // admin membuka daftar order lebih dulu
    const admin = await context.newPage();
    await adminLoginUI(admin, undefined, undefined, '/admin/order');
    await expect(admin.getByRole('heading', { name: 'Order', level: 1 })).toBeVisible();
    await admin.evaluate(() => { (window as unknown as Flagged).__noReload = true; });

    // pembeli checkout dan membayar (order ambil di toko)
    const buyer = await apiUser(request);
    const orderId = await apiPaidOrder(request, buyer.token, 'PICKUP');
    const no = String(orderId).padStart(4, '0');
    await loginUI(page, buyer.email, buyer.password);
    await page.goto(`/order/${orderId}`);
    await expect(page.getByRole('heading', { name: `Order No. ${no}` })).toBeVisible();
    await page.evaluate(() => { (window as unknown as Flagged).__noReload = true; });

    // admin: baris muncul sendiri (polling ~10 dtk), disorot lembut, dengan penanda baru
    const row = admin.getByRole('row').filter({ hasText: no }).first();
    await expect(row).toBeVisible({ timeout: 25_000 });
    await expect(row).toHaveClass(/row-new/);
    await expect(row.getByRole('img', { name: 'Baru dibayar' })).toBeVisible();
    await expect(admin.getByText(/Diperbarui (baru saja|\d+ detik lalu)/)).toBeVisible();
    expect(await admin.evaluate(() => (window as unknown as Flagged).__noReload)).toBe(true); // tidak ada reload

    const results = await new AxeBuilder({ page: admin }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
    expect(results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical').map((v) => v.id)).toEqual([]);

    // admin ubah status ke siap diambil
    await row.getByRole('button', { name: 'Proses pesanan' }).click();
    await expect(row).toContainText('Diproses');
    await row.getByRole('button', { name: 'Siap diambil' }).click();
    await expect(row).toContainText('Siap diambil');

    // pembeli melihat status baru tanpa refresh (polling 15 dtk)
    await expect(page.getByRole('list', { name: 'Linimasa pesanan' })).toContainText('Siap diambil', { timeout: 30_000 });
    expect(await page.evaluate(() => (window as unknown as Flagged).__noReload)).toBe(true);
  });

  test('tombol Muat ulang tersedia dan hitungan butuh-tindakan ditampilkan', async ({ page }) => {
    await adminLoginUI(page, undefined, undefined, '/admin/order');
    await expect(page.getByRole('button', { name: 'Muat ulang' })).toBeVisible();
    await expect(page.getByRole('status').filter({ hasText: /order perlu diproses|Tidak ada order menunggu/ })).toBeVisible();
  });
});
