import AxeBuilder from '@axe-core/playwright';
import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { adminLoginUI, API, apiAdminToken, fixture, samplePhotos } from './helpers';

/** Produk baru dengan sejumlah foto (diunggah lewat halaman admin, provider fake). */
async function productWithPhotos(page: Page, request: APIRequestContext, count: number, real = false) {
  const auth = { Authorization: `Bearer ${await apiAdminToken(request)}` };
  const cats = await (await request.get(`${API}/api/categories`)).json();
  const name = `Kopi Galeri ${count} ${Date.now()}`;
  const prod = await (await request.post(`${API}/api/products`, { headers: auth, data: { categoryId: cats[0].id, name, price: 50000, stock: 5 } })).json();
  if (count > 0) {
    await adminLoginUI(page, undefined, undefined, `/admin/produk/${prod.id}`);
    await page.getByLabel('Pilih foto produk').setInputFiles(real ? await samplePhotos(page, count) : Array.from({ length: count }, (_, i) => fixture(`foto-${(i % 3) + 1}.png`)));
    await expect(page.locator('.imgs .tile img')).toHaveCount(count);
  }
  return { id: prod.id as number, name };
}

async function expectNoSeriousViolations(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  expect(results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical').map((v) => `${v.id}: ${v.help}`)).toEqual([]);
}

test.describe('galeri detail produk', () => {
  test('desktop: panah, thumbnail, keyboard, dan lightbox', async ({ page, request }) => {
    const { id, name } = await productWithPhotos(page, request, 5);
    await page.goto(`/produk/${id}`);
    const gallery = page.getByRole('group', { name: `Foto ${name}` });
    const counter = gallery.locator('output');
    const prev = gallery.getByRole('button', { name: 'Foto sebelumnya' });
    const next = gallery.getByRole('button', { name: 'Foto berikutnya' });
    const thumbs = gallery.getByRole('group', { name: 'Pilih foto' }).getByRole('button');
    const slide = (n: number) => gallery.getByRole('group', { name: `${n} dari 5` });

    // foto pertama prioritas, sisanya lazy, srcset sampai ukuran besar
    await expect(slide(1).locator('img')).toHaveAttribute('loading', 'eager');
    await expect(slide(2).locator('img')).toHaveAttribute('loading', 'lazy');
    await expect(slide(1).locator('img')).toHaveAttribute('srcset', /160w.*480w.*960w.*1600w/);

    // 1440x900: foto utama 4:5 dan kolom thumbnail vertikal di kiri, semuanya terlihat tanpa menggulir
    await page.setViewportSize({ width: 1440, height: 900 });
    const stage900 = (await gallery.locator('.stage').boundingBox())!;
    const col = (await gallery.getByRole('group', { name: 'Pilih foto' }).boundingBox())!;
    expect(Math.round((stage900.height / stage900.width) * 100)).toBe(125); // 4:5
    expect(col.x + col.width).toBeLessThanOrEqual(stage900.x); // kolom di sisi kiri foto
    expect(col.width).toBeLessThanOrEqual(80);
    expect(Math.abs(col.y - stage900.y)).toBeLessThanOrEqual(1); // sejajar dengan sisi atas foto
    for (let i = 0; i < 5; i++) await expect(thumbs.nth(i)).toBeInViewport({ ratio: 1 }); // 5 thumbnail tanpa menggulir
    expect(stage900.y + stage900.height).toBeLessThanOrEqual(900);
    expect(await page.evaluate(() => window.scrollY)).toBe(0);

    // kolom galeri (thumbnail + foto utama) mengambil 55-61% lebar konten selama layar cukup tinggi (di layar pendek lebar dibatasi supaya foto 4:5 tidak melebihi layar)
    await page.setViewportSize({ width: 1440, height: 1000 });
    const ratio = (await gallery.boundingBox())!.width / (await page.locator('.detail').boundingBox())!.width;
    expect(ratio).toBeGreaterThan(0.55);
    expect(ratio).toBeLessThan(0.61);
    await page.setViewportSize({ width: 1280, height: 800 });
    const stage = (await gallery.locator('.stage').boundingBox())!;
    expect(stage.height).toBeLessThanOrEqual(800 - 64);


    // panah: nonaktif di ujung
    await expect(counter).toHaveText('1 / 5');
    await expect(prev).toHaveAttribute('aria-disabled', 'true');
    await next.click();
    await expect(counter).toHaveText('2 / 5');
    await expect(slide(2)).toBeInViewport();
    await expect(thumbs.nth(1)).toHaveAttribute('aria-current', 'true');

    // thumbnail
    await thumbs.nth(4).click();
    await expect(counter).toHaveText('5 / 5');
    await expect(slide(5)).toBeInViewport();
    await expect(next).toHaveAttribute('aria-disabled', 'true');

    // keyboard
    await page.keyboard.press('ArrowLeft');
    await page.keyboard.press('ArrowLeft');
    await expect(counter).toHaveText('3 / 5');

    // lightbox: buka dari foto, navigasi, tutup dengan Esc; fokus kembali ke pemicu
    const trigger = slide(3).getByRole('button');
    await trigger.click();
    const dialog = page.getByRole('dialog', { name: /tampilan besar/ });
    await expect(dialog.getByRole('img')).toHaveAccessibleName(`${name}, foto 3 dari 5`);
    await expect(dialog.getByRole('button', { name: 'Tutup' })).toBeFocused();
    await expectNoSeriousViolations(page);
    await dialog.getByRole('button', { name: 'Foto berikutnya' }).click();
    await expect(dialog.getByRole('img')).toHaveAccessibleName(`${name}, foto 4 dari 5`);
    await page.keyboard.press('ArrowRight');
    await expect(dialog.getByText('5 / 5')).toBeVisible();
    // fokus terkurung: Tab berkali-kali tidak keluar dari dialog
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press('Tab');
      expect(await page.evaluate(() => !document.activeElement || document.activeElement === document.body || !!document.activeElement.closest('dialog'))).toBe(true);
    }
    await page.keyboard.press('Escape');
    await expect(dialog).toBeHidden();
    await expect(trigger).toBeFocused();
    await expect(counter).toHaveText('3 / 5');

    // tutup lewat tombol dan lewat klik latar
    await trigger.click();
    await dialog.getByRole('button', { name: 'Tutup' }).click();
    await expect(dialog).toBeHidden();
    await trigger.click();
    await expect(dialog).toBeVisible();
    await page.mouse.click(40, 780);
    await expect(dialog).toBeHidden();
    await expectNoSeriousViolations(page);
  });

  test('layar pendek: kolom thumbnail lebih tinggi dari foto, menggulir sendiri dan mengikuti foto aktif', async ({ page, request }) => {
    const { id, name } = await productWithPhotos(page, request, 6); // batas foto per produk
    await page.setViewportSize({ width: 1100, height: 600 }); // foto 4:5 minimal 380px lebar = 475px tinggi; 6 thumbnail butuh 528px
    await page.goto(`/produk/${id}`);
    const gallery = page.getByRole('group', { name: `Foto ${name}` });
    const col = gallery.getByRole('group', { name: 'Pilih foto' });
    const stage = (await gallery.locator('.stage').boundingBox())!;
    expect(Math.abs((await col.boundingBox())!.height - stage.height)).toBeLessThanOrEqual(1); // tinggi kolom = tinggi foto
    expect(await col.evaluate((el) => el.scrollHeight > el.clientHeight)).toBe(true);
    expect(await col.evaluate((el) => getComputedStyle(el).overflowY)).toBe('auto');
    const lastThumb = col.getByRole('button').last();
    const hiddenBelow = () => Promise.all([lastThumb.boundingBox(), col.boundingBox()]).then(([t, c]) => t!.y + t!.height - (c!.y + c!.height)); // >0: terpotong di bawah kolom
    expect(await hiddenBelow()).toBeGreaterThan(0);
    for (let i = 0; i < 5; i++) await gallery.getByRole('button', { name: 'Foto berikutnya' }).click();
    await expect.poll(hiddenBelow).toBeLessThanOrEqual(1); // thumbnail aktif ikut tergulir ke dalam kolom
    expect(await col.evaluate((el) => el.scrollTop)).toBeGreaterThan(0);
    expect(await page.evaluate(() => window.scrollY)).toBe(0); // halaman tidak ikut bergulir
  });

  test('lightbox: foto mendatar dan tegak tampil utuh di dalam layar, tanpa dipotong', async ({ page, request }) => {
    const { id, name } = await productWithPhotos(page, request, 5, true); // foto ke-4 mendatar 3:2... lihat samplePhotos
    for (const vp of [{ width: 1440, height: 900 }, { width: 390, height: 844 }]) {
      await page.setViewportSize(vp);
      await page.goto(`/produk/${id}`);
      const gallery = page.getByRole('group', { name: `Foto ${name}` });
      const dialog = page.getByRole('dialog', { name: /tampilan besar/ });
      for (const n of [1, 4]) {
        const opener = gallery.getByRole('group', { name: `${n} dari 5` }).getByRole('button');
        await expect(async () => {
          await opener.evaluate((b: HTMLElement) => b.click()); // klik lewat DOM: slide di luar layar ponsel tidak bisa diklik sungguhan
          await expect(dialog).toBeVisible({ timeout: 1000 });
        }).toPass();
        const img = dialog.getByRole('img');
        await expect(img).toHaveAccessibleName(`${name}, foto ${n} dari 5`);
        await expect.poll(() => img.evaluate((el: HTMLImageElement) => el.complete && el.naturalWidth > 0)).toBe(true);
        const box = (await img.boundingBox())!;
        const nat = await img.evaluate((el: HTMLImageElement) => ({ w: el.naturalWidth, h: el.naturalHeight }));
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.y).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(vp.width + 0.5);
        expect(box.y + box.height).toBeLessThanOrEqual(vp.height + 0.5);
        expect(await img.evaluate((el) => getComputedStyle(el).objectFit)).toBe('contain');
        expect(Math.abs(box.width / box.height - nat.w / nat.h)).toBeLessThan(0.01); // rasio asli, bukan 4:5
        expect(n === 4 ? nat.w > nat.h : nat.w < nat.h).toBe(true);
        await page.keyboard.press('Escape');
        await expect(dialog).toBeHidden();
      }
    }
  });

  test('ponsel: foto selebar layar, geser berpindah foto, penanda posisi, tanpa panah dan thumbnail', async ({ page, request }) => {
    const { id, name } = await productWithPhotos(page, request, 5);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(`/produk/${id}`);
    const gallery = page.getByRole('group', { name: `Foto ${name}` });
    const track = gallery.locator('.track');
    const box = (await track.boundingBox())!;
    expect(box.x).toBe(0);
    expect(box.width).toBe(390);
    expect(Math.round(box.height)).toBe(488); // 4:5
    await expect(gallery.getByRole('button', { name: 'Foto berikutnya' })).toBeHidden();
    await expect(gallery.getByRole('group', { name: 'Pilih foto' })).toBeHidden();
    await expect(gallery.locator('output')).toHaveText('1 / 5');

    // geser: gulir horizontal di lintasan (scroll-snap berhenti tepat di foto berikutnya)
    await page.mouse.move(195, box.y + 200);
    await page.mouse.wheel(300, 0);
    await expect(gallery.locator('output')).toHaveText('2 / 5');
    await expect.poll(() => track.evaluate((el) => el.scrollLeft)).toBe(390);
    await page.mouse.wheel(300, 0);
    await expect(gallery.locator('output')).toHaveText('3 / 5');
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390); // tanpa overflow horizontal

    await gallery.getByRole('group', { name: '3 dari 5' }).getByRole('button').click();
    const dialog = page.getByRole('dialog', { name: /tampilan besar/ });
    await expect(dialog.getByText('3 / 5')).toBeVisible();
    await dialog.getByRole('button', { name: 'Tutup' }).click();
    await expect(dialog).toBeHidden();
    await expectNoSeriousViolations(page);
  });

  test('satu foto: tanpa panah, thumbnail, dan penanda; tanpa foto: placeholder label kemasan', async ({ page, request }) => {
    const one = await productWithPhotos(page, request, 1);
    await page.goto(`/produk/${one.id}`);
    const gallery = page.getByRole('group', { name: `Foto ${one.name}` });
    await expect(gallery.getByRole('img', { name: one.name })).toBeVisible();
    await expect(gallery.getByRole('button')).toHaveCount(1);
    await expect(gallery.locator('output')).toHaveCount(0);
    await gallery.getByRole('button').click();
    await expect(page.getByRole('dialog').getByRole('img')).toBeVisible();
    await page.keyboard.press('Escape');
    await expectNoSeriousViolations(page);

    const none = await productWithPhotos(page, request, 0);
    await page.goto(`/produk/${none.id}`);
    await expect(page.locator('.gallery .vis.big .cap')).toContainText('No.');
    await expect(page.locator('.gallery button')).toHaveCount(0);
  });
});

test.describe('lightbox ponsel: cubit dan ketuk dua kali', () => {
  test.use({ hasTouch: true, viewport: { width: 390, height: 844 } });

  test('cubit memperbesar dan memperkecil, geser saat diperbesar, ketuk dua kali zoom dan kembali', async ({ page, request }) => {
    const { id, name } = await productWithPhotos(page, request, 3, true);
    await page.goto(`/produk/${id}`);
    await page.getByRole('group', { name: `Foto ${name}` }).getByRole('group', { name: '1 dari 3' }).getByRole('button').tap();
    const dialog = page.getByRole('dialog', { name: /tampilan besar/ });
    const img = dialog.getByRole('img');
    await expect.poll(() => img.evaluate((el: HTMLImageElement) => el.complete && el.naturalWidth > 0)).toBe(true);
    const zoom = () => img.getAttribute('data-zoom').then(Number);
    const base = (await img.boundingBox())!;
    expect(await zoom()).toBe(1);

    // cubit dua jari: renggangkan dari 80px ke 240px
    const cdp = await page.context().newCDPSession(page);
    const cy = 422;
    const touch = (type: 'touchStart' | 'touchMove' | 'touchEnd', a: number, b: number) => cdp.send('Input.dispatchTouchEvent', { type, touchPoints: type === 'touchEnd' ? [] : [{ x: 195 - a, y: cy, id: 0 }, { x: 195 + b, y: cy, id: 1 }] });
    await touch('touchStart', 40, 40);
    for (const d of [60, 80, 100, 120]) await touch('touchMove', d, d);
    await touch('touchEnd', 0, 0);
    const zoomedIn = await zoom();
    expect(zoomedIn).toBeGreaterThan(2.5);
    expect(zoomedIn).toBeLessThanOrEqual(4);
    const big = (await img.boundingBox())!;
    expect(big.width).toBeGreaterThan(base.width * 2.5);

    // geser satu jari saat diperbesar: gambar bergerak, tepi tidak lepas dari layar
    const before = (await img.boundingBox())!;
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: 200, y: 400, id: 0 }] });
    for (const dx of [-20, -50, -90]) await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: 200 + dx, y: 400, id: 0 }] });
    await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
    const after = (await img.boundingBox())!;
    expect(after.x).toBeLessThan(before.x);
    expect(after.x + after.width).toBeGreaterThanOrEqual(390 - 0.5); // tepi kanan tidak masuk ke dalam layar

    // cubit mengecil kembali ke 1x
    await touch('touchStart', 120, 120);
    for (const d of [90, 60, 30, 10]) await touch('touchMove', d, d);
    await touch('touchEnd', 0, 0);
    await expect.poll(zoom).toBe(1);
    const back = (await img.boundingBox())!;
    expect(Math.abs(back.width - base.width)).toBeLessThan(1);

    // ketuk dua kali: zoom, ketuk dua kali lagi: kembali
    await page.touchscreen.tap(195, 422);
    await page.touchscreen.tap(195, 422);
    await expect.poll(zoom).toBeCloseTo(2.5, 1);
    await page.touchscreen.tap(195, 422);
    await page.touchscreen.tap(195, 422);
    await expect.poll(zoom).toBe(1);

    // satu ketukan pada foto tidak menutup dialog; ketukan di latar menutup
    await page.touchscreen.tap(195, 422);
    await expect(dialog).toBeVisible();
    await expectNoSeriousViolations(page);
    await dialog.getByRole('button', { name: 'Tutup' }).tap();
    await expect(dialog).toBeHidden();
  });
});
