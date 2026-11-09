import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { adminCredentials, apiUser, loginUI } from './helpers';

async function expectNoSeriousViolations(page: Page, label: string) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  const bad = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
  expect(bad.map((v) => `${label}: ${v.id} (${v.nodes.length}x) ${v.help}`), `pelanggaran aksesibilitas di ${label}`).toEqual([]);
}

test.describe('aksesibilitas (axe: tanpa pelanggaran serious/critical)', () => {
  for (const width of [375, 1280]) {
    test(`halaman publik di lebar ${width}`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/');
      await expect(page.locator('.grid .card').first()).toBeVisible();
      await expectNoSeriousViolations(page, `katalog ${width}`);

      await page.locator('.grid .card').first().click();
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expectNoSeriousViolations(page, `detail produk ${width}`);

      await page.goto('/masuk');
      await expectNoSeriousViolations(page, `masuk ${width}`);
      await page.goto('/daftar');
      await expectNoSeriousViolations(page, `daftar ${width}`);
      await page.goto('/tidak-ada');
      await expectNoSeriousViolations(page, `404 ${width}`);
    });
  }

  test('halaman pelanggan (keranjang, checkout, order)', async ({ page, request }) => {
    const user = await apiUser(request);
    await loginUI(page, user.email, user.password);
    await page.goto('/keranjang');
    await expect(page.getByText('Keranjang Anda masih kosong')).toBeVisible();
    await expectNoSeriousViolations(page, 'keranjang kosong');

    await page.goto('/?q=gayo');
    await page.locator('.grid .card').first().click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();
    await expect(page.getByRole('region', { name: 'Ringkasan' })).toBeVisible();
    await expectNoSeriousViolations(page, 'keranjang');
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();
    await expect(page.getByLabel('Nama penerima')).toBeVisible();
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click(); // memunculkan error validasi
    await expectNoSeriousViolations(page, 'checkout dengan error');
    await page.goto('/order');
    await expectNoSeriousViolations(page, 'riwayat order');
  });

  test('halaman admin', async ({ page }) => {
    const admin = adminCredentials();
    await loginUI(page, admin.email, admin.password);
    for (const path of ['/admin/produk', '/admin/kategori', '/admin/order']) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('table, form').first()).toBeVisible();
      await expectNoSeriousViolations(page, path);
    }
  });
});
