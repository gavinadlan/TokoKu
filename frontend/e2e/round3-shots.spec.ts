import { test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { adminLoginUI, API, apiAdminToken } from './helpers';

// Screenshot Putaran 3 (galeri detail produk). SHOTS_TAG=sebelum|sesudah menentukan awalan nama berkas.
const OUT = fileURLToPath(new URL('../../docs/design/round3/', import.meta.url));
const TAG = process.env.SHOTS_TAG ?? 'sesudah';
const VIEWPORTS = [{ name: 'desktop-1440', width: 1440, height: 900 }, { name: 'ponsel-390', width: 390, height: 844 }];

/** Foto contoh dirender di tempat (bukan foto asli): kantong bernomor di atas nada palet. Foto ke-4 sengaja mendatar. */
export async function samplePhotos(page: Page, count: number) {
  const tones = [['#DACBB4', '#3A2D25'], ['#5B4636', '#F6F1E9'], ['#C8B293', '#1B1613'], ['#7A624E', '#F6F1E9'], ['#E9E0D2', '#5B4636']];
  const p = await page.context().newPage();
  const files = [];
  for (let i = 0; i < count; i++) {
    const [bg, fg] = tones[i % tones.length]!;
    const landscape = i === 3;
    await p.setViewportSize(landscape ? { width: 1500, height: 1000 } : { width: 1200, height: 1500 });
    await p.setContent(`<body style="margin:0;height:100vh;display:grid;place-items:center;background:${bg}">
      <div style="width:${landscape ? 30 : 46}%;aspect-ratio:3/4;background:${fg};border-radius:12px 12px 28px 28px;display:grid;place-items:center;font:300 220px Georgia,serif;color:${bg}">${i + 1}</div></body>`);
    files.push({ name: `contoh-${i + 1}.png`, mimeType: 'image/png', buffer: await p.screenshot() });
  }
  await p.close();
  return files;
}

test('screenshot galeri: produk 1 foto dan 5 foto, desktop 1440 dan ponsel 390', async ({ page, request }) => {
  test.setTimeout(180_000);
  mkdirSync(OUT, { recursive: true });
  const auth = { Authorization: `Bearer ${await apiAdminToken(request)}` };
  const cats = await (await request.get(`${API}/api/categories`)).json();
  await adminLoginUI(page);

  for (const count of [1, 5]) {
    const prod = await (await request.post(`${API}/api/products`, { headers: auth, data: { categoryId: cats[0].id, name: `Kopi Gayo Contoh ${count} Foto 250g`, price: 85000, stock: 12, description: 'Medium roast, aroma rempah dan cokelat. Digiling sesuai pesanan.' } })).json();
    await page.goto(`/admin/produk/${prod.id}`);
    await page.getByLabel('Pilih foto produk').setInputFiles(await samplePhotos(page, count));
    await page.locator('.imgs .tile img').nth(count - 1).waitFor();

    for (const v of VIEWPORTS) {
      await page.setViewportSize(v);
      await page.goto(`/produk/${prod.id}`);
      await page.locator('.gallery img').first().waitFor();
      await page.evaluate(() => { window.scrollTo(0, 0); return document.fonts.ready; });
      await page.waitForTimeout(400);
      await page.screenshot({ path: `${OUT}${TAG}-${count}foto-${v.name}.png` });
      if (TAG === 'sesudah' && count === 5) {
        await page.getByRole('button', { name: /perbesar/ }).first().click();
        await page.getByRole('dialog').waitFor();
        await page.waitForTimeout(400);
        await page.screenshot({ path: `${OUT}${TAG}-lightbox-${v.name}.png` });
      }
    }
    await page.setViewportSize({ width: 1280, height: 800 });
  }
});
