import { test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { adminCredentials, API, apiUser, loginUI } from './helpers';

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

  // admin
  const admin = adminCredentials();
  await page.evaluate(() => localStorage.clear());
  await loginUI(page, admin.email, admin.password);
  await page.goto('/admin/produk');
  await page.getByRole('table').waitFor();
  for (const w of WIDTHS) await shoot(page, 'admin-produk', w);
});
