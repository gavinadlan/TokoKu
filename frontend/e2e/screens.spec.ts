import { test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { adminLoginUI, API, apiAdminToken, apiPaidOrder, apiUser, fixture, loginUI } from './helpers';

const OUT = process.env.SHOTS_DIR ?? fileURLToPath(new URL('../../docs/design/screens/', import.meta.url));
const WIDTHS = [375, 768, 1280];

async function shoot(page: Page, name: string, width: number) {
  await page.setViewportSize({ width, height: width === 375 ? 812 : 900 });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(250);
  await page.screenshot({ path: `${OUT}${name}-${width}.png`, fullPage: true });
}

test('screenshot halaman utama di 375, 768, 1280', async ({ page, request }) => {
  test.setTimeout(180_000);
  mkdirSync(OUT, { recursive: true });

  // publik
  await page.goto('/');
  await page.locator('.grid .card').first().waitFor();
  for (const w of WIDTHS) { await page.setViewportSize({ width: w, height: 900 }); await shoot(page, 'katalog', w); }

  await page.goto('/produk/1');
  await page.getByRole('heading', { level: 1 }).waitFor();
  for (const w of WIDTHS) await shoot(page, 'produk', w);

  await page.goto('/masuk');
  for (const w of WIDTHS) await shoot(page, 'masuk', w);

  // pelanggan dengan isi keranjang
  const user = await apiUser(request);
  const auth = { Authorization: `Bearer ${user.token}` };
  for (const [id, qty] of [[1, 2], [11, 1]]) await request.post(`${API}/api/cart/items`, { headers: auth, data: { productId: id, quantity: qty } });
  await loginUI(page, user.email, user.password);
  await page.goto('/keranjang');
  await page.getByRole('region', { name: 'Ringkasan' }).waitFor();
  for (const w of WIDTHS) await shoot(page, 'keranjang', w);

  await page.goto('/checkout');
  await page.getByLabel('Nama penerima').waitFor();
  for (const w of WIDTHS) await shoot(page, 'checkout', w);

  // checkout dengan pilihan pengambilan
  await page.goto('/checkout');
  await page.getByLabel('Nama penerima').waitFor();
  await page.getByRole('radio', { name: /Ambil di toko/ }).check();
  await page.getByLabel('Nama pengambil').fill('Siti Rahmawati');
  await page.getByLabel('Nomor telepon').fill('0812 3456 7890');
  for (const w of WIDTHS) await shoot(page, 'checkout-ambil', w);

  // order dengan linimasa dan invoice
  const paid = await apiPaidOrder(request, user.token, 'DELIVERY');
  await page.goto(`/order/${paid}`);
  await page.getByRole('list', { name: 'Linimasa pesanan' }).waitFor();
  for (const w of WIDTHS) await shoot(page, 'order-detail', w);

  // produk dengan galeri foto
  const adminToken = await apiAdminToken(request);
  const cats = await (await request.get(`${API}/api/categories`)).json();
  const prod = await (await request.post(`${API}/api/products`, { headers: { Authorization: `Bearer ${adminToken}` }, data: { categoryId: cats[0].id, name: 'Kopi Arabika Foto Contoh 250g', price: 85000, stock: 12, description: 'Medium roast, aroma rempah dan cokelat.' } })).json();

  // admin
  await page.evaluate(() => localStorage.clear());
  const adminPage = await page.context().newPage();
  await adminPage.goto('/admin/masuk');
  for (const w of WIDTHS) await shoot(adminPage, 'admin-login', w);
  await adminLoginUI(adminPage);
  await adminPage.getByRole('table').waitFor();
  for (const w of WIDTHS) await shoot(adminPage, 'admin-produk', w);
  await adminPage.goto('/admin/order');
  await adminPage.getByRole('table').waitFor();
  for (const w of WIDTHS) await shoot(adminPage, 'admin-order', w);
  await adminPage.goto(`/admin/produk/${prod.id}`);
  await adminPage.getByLabel('Pilih foto produk').setInputFiles([fixture('foto-1.png'), fixture('foto-2.png'), fixture('foto-3.png')]);
  await adminPage.locator('.imgs .tile img').nth(2).waitFor();
  for (const w of WIDTHS) await shoot(adminPage, 'admin-foto', w);
  await adminPage.goto('/admin/pengaturan');
  await adminPage.getByLabel('Nama toko').waitFor();
  for (const w of WIDTHS) await shoot(adminPage, 'admin-pengaturan', w);
  await page.goto(`/produk/${prod.id}`);
  await page.getByRole('group', { name: /Foto Kopi/ }).waitFor();
  for (const w of WIDTHS) await shoot(page, 'produk-galeri', w);
});
