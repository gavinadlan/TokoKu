import AxeBuilder from '@axe-core/playwright';
import { expect, test, type APIRequestContext, type Page } from '@playwright/test';
import { adminLoginUI, API, apiAdminToken, fixture } from './helpers';

/** Produk baru dengan sejumlah foto (diunggah lewat halaman admin, provider fake). */
async function productWithPhotos(page: Page, request: APIRequestContext, count: number) {
  const auth = { Authorization: `Bearer ${await apiAdminToken(request)}` };
  const cats = await (await request.get(`${API}/api/categories`)).json();
  const name = `Kopi Galeri ${count} ${Date.now()}`;
  const prod = await (await request.post(`${API}/api/products`, { headers: auth, data: { categoryId: cats[0].id, name, price: 50000, stock: 5 } })).json();
  if (count > 0) {
    await adminLoginUI(page, undefined, undefined, `/admin/produk/${prod.id}`);
    await page.getByLabel('Pilih foto produk').setInputFiles(Array.from({ length: count }, (_, i) => fixture(`foto-${(i % 3) + 1}.png`)));
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

    // galeri mengambil 55-60% lebar konten selama layar cukup tinggi (di layar pendek lebar dibatasi supaya foto 4:5 tidak melebihi layar)
    await page.setViewportSize({ width: 1440, height: 1000 });
    const ratio = (await gallery.boundingBox())!.width / (await page.locator('.detail').boundingBox())!.width;
    expect(ratio).toBeGreaterThan(0.55);
    expect(ratio).toBeLessThan(0.6);
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
