import { expect, test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { adminLoginUI, API, apiAdminToken, samplePhotos } from './helpers';

// Screenshot Putaran 3 (galeri detail produk). SHOTS_TAG=sebelum|sesudah menentukan awalan nama berkas.
const OUT = fileURLToPath(new URL('../../docs/design/round3/', import.meta.url));
const TAG = process.env.SHOTS_TAG ?? 'sesudah';
const VIEWPORTS = [{ name: 'desktop-1440', width: 1440, height: 900 }, { name: 'ponsel-390', width: 390, height: 844 }];

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
      await page.locator('.gallery .track img').first().waitFor();
      await page.evaluate(() => { window.scrollTo(0, 0); return document.fonts.ready; });
      await page.waitForTimeout(400);
      await page.screenshot({ path: `${OUT}${TAG}-${count}foto-${v.name}.png` });
      // ukuran pasti (px, CSS) untuk catatan DESIGN.md
      const box = async (sel: string) => page.locator(sel).first().boundingBox();
      const m = { viewport: `${v.width}x${v.height}`, foto: count, utama: await box('.gallery .stage'), thumbs: count > 1 ? await box('.gallery .thumbs') : null, thumb1: count > 1 ? await box('.gallery .thumbs button') : null, panel: await box('.panel') };
      console.log(`UKURAN ${JSON.stringify(m)}`);
      if (v.width === 1440 && count > 1) expect(m.utama!.width, 'foto utama di 1440x900').toBeGreaterThanOrEqual(620);
      if (TAG === 'sesudah' && count === 5) {
        await page.getByRole('button', { name: /perbesar/ }).first().click();
        await page.getByRole('dialog').waitFor();
        await page.waitForTimeout(400);
        await page.screenshot({ path: `${OUT}${TAG}-lightbox-${v.name}.png` });
        // foto ke-4 mendatar: tampil utuh, tidak dipotong 4:5
        for (let i = 0; i < 3; i++) await page.getByRole('dialog').getByRole('button', { name: 'Foto berikutnya' }).click();
        await page.waitForTimeout(400);
        await page.screenshot({ path: `${OUT}${TAG}-lightbox-mendatar-${v.name}.png` });
      }
    }
    await page.setViewportSize({ width: 1280, height: 800 });
  }
});
