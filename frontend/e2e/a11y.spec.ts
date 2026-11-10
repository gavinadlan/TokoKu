import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { adminLoginUI, apiAdminToken, apiPaidOrder, apiUser, fixture, loginUI, API } from './helpers';

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
    await expect(page.getByRole('heading', { name: 'Belum ada pesanan' })).toBeVisible();
    await expectNoSeriousViolations(page, 'riwayat order');
  });

  test('checkout (kirim dan ambil), detail order dengan linimasa', async ({ page, request }) => {
    const user = await apiUser(request);
    await loginUI(page, user.email, user.password);
    const paid = await apiPaidOrder(request, user.token, 'PICKUP');
    await page.goto('/?q=gayo');
    await page.locator('.grid .card').first().click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();
    await expect(page.getByLabel('Alamat lengkap')).toBeVisible();
    await expectNoSeriousViolations(page, 'checkout kirim');
    await page.getByRole('radio', { name: /Ambil di toko/ }).check();
    await expect(page.getByRole('region', { name: 'Lokasi pengambilan' })).toBeVisible();
    await expectNoSeriousViolations(page, 'checkout ambil');
    await page.goto(`/order/${paid}`);
    await expect(page.getByRole('list', { name: 'Linimasa pesanan' })).toBeVisible();
    await expectNoSeriousViolations(page, 'detail order');
  });

  test('galeri produk dan halaman admin (login, produk, ubah + foto, order, pengaturan)', async ({ page, request }) => {
    const token = await apiAdminToken(request);
    const auth = { Authorization: `Bearer ${token}` };
    const cats = await (await request.get(`${API}/api/categories`)).json();
    const created = await (await request.post(`${API}/api/products`, { headers: auth, data: { categoryId: cats[0].id, name: `Kopi Axe ${Date.now()}`, price: 1000, stock: 2 } })).json();

    await page.goto('/admin/masuk');
    await expect(page.getByRole('heading', { name: 'Masuk pengelola' })).toBeVisible();
    await expectNoSeriousViolations(page, 'login admin');
    await adminLoginUI(page);

    for (const path of ['/admin/produk', '/admin/kategori', '/admin/order', '/admin/pengaturan', `/admin/produk/${created.id}`, '/admin/produk/baru']) {
      await page.goto(path);
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
      await expect(page.locator('table, form').first()).toBeVisible();
      if (path.endsWith(String(created.id))) {
        await page.getByLabel('Pilih foto produk').setInputFiles([fixture('foto-1.png'), fixture('foto-2.png')]);
        await expect(page.locator('.imgs .tile img')).toHaveCount(2);
      }
      await expectNoSeriousViolations(page, path);
    }

    await page.goto(`/produk/${created.id}`);
    await expect(page.getByRole('group', { name: /Foto Kopi Axe/ })).toBeVisible();
    await expectNoSeriousViolations(page, 'galeri produk');
  });
});
