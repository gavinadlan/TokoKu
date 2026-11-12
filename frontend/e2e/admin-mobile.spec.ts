import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { adminLoginUI, apiPaidOrder, apiUser } from './helpers';

// visualViewport: di ponsel, innerWidth ikut melebar bila konten melebar, jadi bukan pembanding yang jujur
const noScrollX = (page: Page) => page.evaluate(() => document.documentElement.scrollWidth <= Math.ceil(window.visualViewport?.width ?? window.innerWidth));

test.describe('admin di ponsel (390px)', () => {
  test('proses order sampai siap diambil dari kartu', async ({ page, request }) => {
    const user = await apiUser(request);
    const id = await apiPaidOrder(request, user.token, 'PICKUP');
    const no = String(id).padStart(4, '0');
    await adminLoginUI(page, undefined, undefined, '/admin/order');

    const list = page.getByRole('list', { name: 'Daftar order' });
    await expect(list).toBeVisible();
    await expect(page.getByRole('table')).toBeHidden(); // tabel hanya untuk lebar >= 768
    const card = list.getByRole('listitem').filter({ hasText: `Order ${no}` });
    await expect(card.getByText('Ambil di toko')).toBeVisible();

    // lencana satu baris, utuh
    const badge = card.locator('.badge').first();
    expect((await badge.boundingBox())!.height).toBeLessThan(34);

    // tombol utama selebar kartu dan cukup besar
    const proses = card.getByRole('button', { name: `Proses pesanan, order ${no}` });
    const [bb, cb] = [(await proses.boundingBox())!, (await card.boundingBox())!];
    expect(bb.height).toBeGreaterThanOrEqual(44);
    expect(bb.width).toBeGreaterThan(cb.width - 40);

    await proses.click();
    await expect(card.getByText('Diproses', { exact: true })).toBeVisible();
    await card.getByRole('button', { name: `Siap diambil, order ${no}` }).click();
    await expect(card.getByText('Siap diambil', { exact: true }).first()).toBeVisible();
    await expect(card.getByRole('button', { name: /Selesaikan \(kode\)/ })).toBeVisible();
    expect(await noScrollX(page)).toBe(true);
  });

  test('tab menu: tab aktif tergulir ke tampilan di 360px, tanpa scroll horizontal halaman', async ({ page }) => {
    await page.setViewportSize({ width: 360, height: 800 });
    await adminLoginUI(page, undefined, undefined, '/admin/pengaturan');
    const nav = page.getByRole('navigation', { name: 'Kelola toko' });
    const active = nav.getByRole('link', { name: 'Pengaturan toko' });
    await expect(active).toBeInViewport({ ratio: 0.99 });
    expect(await noScrollX(page)).toBe(true);
  });

  for (const path of ['/admin/order', '/admin/produk', '/admin/kategori', '/admin/pengaturan']) {
    test(`axe dan tanpa scroll horizontal: ${path}`, async ({ page, request }) => {
      if (path === '/admin/order') await apiPaidOrder(request, (await apiUser(request)).token, 'DELIVERY');
      await adminLoginUI(page, undefined, undefined, path);
      await expect(page.locator('.topbar h1')).toBeVisible();
      await page.waitForTimeout(800);
      expect(await noScrollX(page)).toBe(true);
      const r = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
      const bad = r.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
      expect(bad.map((v) => `${path}: ${v.id} (${v.nodes.length}x)`)).toEqual([]);
    });
  }
});
