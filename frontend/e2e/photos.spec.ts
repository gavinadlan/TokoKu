import { expect, test } from '@playwright/test';
import { API, adminLoginUI, apiAdminToken, fixture } from './helpers';

test.describe('foto produk', () => {
  test('admin mengunggah, mengurutkan, menjadikan utama, dan menghapus; galeri tampil di toko', async ({ page, request }) => {
    const token = await apiAdminToken(request);
    const auth = { Authorization: `Bearer ${token}` };
    const cats = await (await request.get(`${API}/api/categories`)).json();
    const name = `Kopi Foto ${Date.now()}`;
    const created = await (await request.post(`${API}/api/products`, { headers: auth, data: { categoryId: cats[0].id, name, price: 50000, stock: 5 } })).json();

    await adminLoginUI(page, undefined, undefined, `/admin/produk/${created.id}`);
    await expect(page.getByRole('heading', { name: /Foto produk/ })).toBeVisible();
    await expect(page.getByRole('heading', { name: /0 dari 6 foto/ })).toBeVisible();

    // unggah tiga foto
    await page.getByLabel('Pilih foto produk').setInputFiles([fixture('foto-1.png'), fixture('foto-2.png'), fixture('foto-3.png')]);
    await expect(page.getByRole('heading', { name: /3 dari 6 foto/ })).toBeVisible();
    await expect(page.locator('.imgs .tile img')).toHaveCount(3);
    await expect(page.locator('.primary-tag')).toHaveCount(1);

    const ids = async () => (await (await request.get(`${API}/api/products/${created.id}`)).json()).images.map((i: { id: number }) => i.id) as number[];
    const before = await ids();
    expect(before).toHaveLength(3);

    // urutan: geser foto 2 ke kiri
    await page.getByRole('button', { name: 'Geser foto 2 ke kiri' }).click();
    await expect.poll(ids).toEqual([before[1], before[0], before[2]]);
    await expect(page.getByRole('button', { name: 'Geser foto 1 ke kiri' })).toBeDisabled();

    // jadikan foto 3 sebagai utama
    await page.locator('.tile').nth(2).getByRole('button', { name: 'Jadikan utama' }).click();
    await expect.poll(async () => (await (await request.get(`${API}/api/products/${created.id}`)).json()).images.map((i: { primary: boolean }) => i.primary)).toEqual([false, false, true]);

    // hapus dengan konfirmasi
    await page.getByRole('button', { name: 'Hapus foto 1' }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Hapus foto' }).click();
    await expect(page.getByRole('heading', { name: /2 dari 6 foto/ })).toBeVisible();
    expect(await ids()).toHaveLength(2);

    // galeri di toko: foto nyata dengan srcset, navigasi keyboard
    await page.goto(`/produk/${created.id}`);
    const gallery = page.getByRole('group', { name: `Foto ${name}` });
    await expect(gallery.locator('.stage img')).toHaveAttribute('srcset', /160w.*480w.*960w/);
    await expect(gallery.getByText('1 / 2').or(gallery.getByText('2 / 2'))).toBeVisible();
    const first = await gallery.locator('output').innerText();
    await gallery.getByRole('button', { name: 'Foto berikutnya' }).focus();
    await page.keyboard.press('ArrowRight');
    await expect(gallery.locator('output')).not.toHaveText(first);

    // kartu katalog memakai foto nyata
    await page.goto(`/?q=${encodeURIComponent(name)}`);
    await expect(page.locator('.grid .card .vis img').first()).toBeVisible();
  });

  test('klien menolak format salah dan berkas besar sebelum mengunggah, dengan alasan yang jelas', async ({ page, request }) => {
    const token = await apiAdminToken(request);
    const cats = await (await request.get(`${API}/api/categories`)).json();
    const created = await (await request.post(`${API}/api/products`, { headers: { Authorization: `Bearer ${token}` }, data: { categoryId: cats[0].id, name: `Kopi Tolak ${Date.now()}`, price: 1000, stock: 1 } })).json();
    await adminLoginUI(page, undefined, undefined, `/admin/produk/${created.id}`);

    await page.getByLabel('Pilih foto produk').setInputFiles({ name: 'animasi.gif', mimeType: 'image/gif', buffer: Buffer.from('GIF89a') });
    await expect(page.getByRole('alert').getByText(/Format harus jpg, png, atau webp/)).toBeVisible();
    await page.getByLabel('Pilih foto produk').setInputFiles({ name: 'besar.png', mimeType: 'image/png', buffer: Buffer.alloc(5 * 1024 * 1024 + 10) });
    await expect(page.getByRole('alert').getByText(/melebihi batas 5 MB/)).toBeVisible();
    await expect(page.locator('.imgs .tile')).toHaveCount(0);
  });
});
