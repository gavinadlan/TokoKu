import { expect, test } from '@playwright/test';
import { adminCredentials, apiPaidOrder, apiUser, loginUI } from './helpers';

test.describe('admin', () => {
  test('buat produk lalu muncul di katalog publik', async ({ page }) => {
    const admin = adminCredentials();
    await loginUI(page, admin.email, admin.password);
    await page.goto('/admin/produk');
    const name = `Kopi Uji ${Date.now()}`;

    await page.getByRole('button', { name: 'Tambah produk' }).click();
    await page.getByLabel('Nama produk').fill(name);
    await page.getByLabel('Kategori').selectOption({ index: 1 });
    await page.getByLabel('Harga (Rp)').fill('55000');
    await page.getByLabel('Stok').fill('7');
    await page.getByLabel('Deskripsi (opsional)').fill('Produk percobaan dari e2e.');
    await page.getByRole('button', { name: 'Simpan produk' }).click();

    await page.getByLabel('Cari nama').fill(name);
    const row = page.getByRole('row', { name: new RegExp(name) });
    await expect(row).toBeVisible();
    await expect(row).toContainText('7');

    await page.goto(`/?q=${encodeURIComponent(name)}`);
    await expect(page.getByRole('link', { name: new RegExp(name) })).toBeVisible();

    // nonaktifkan: hilang dari katalog, tetap ada di admin
    await page.goto('/admin/produk');
    await page.getByLabel('Cari nama').fill(name);
    await page.getByRole('button', { name: `Nonaktifkan ${name}` }).click();
    await page.getByRole('button', { name: 'Nonaktifkan', exact: true }).click();
    await expect(page.getByRole('row', { name: new RegExp(name) })).toContainText('nonaktif');
    await page.goto(`/?q=${encodeURIComponent(name)}`);
    await expect(page.getByRole('heading', { name: /Tidak ada produk untuk/ })).toBeVisible();
  });

  test('ubah status order sesuai transisi yang sah', async ({ page, request }) => {
    const customer = await apiUser(request);
    const orderId = await apiPaidOrder(request, customer.token);
    const admin = adminCredentials();
    await loginUI(page, admin.email, admin.password);
    await page.goto('/admin/order');

    const row = page.getByRole('row').filter({ hasText: String(orderId).padStart(4, '0') }).first();
    await expect(row).toContainText('Dibayar');
    await row.getByRole('button', { name: 'Proses pesanan' }).click();
    await expect(row).toContainText('Diproses');
    await row.getByRole('button', { name: 'Tandai dikirim' }).click();
    await expect(row).toContainText('Dikirim');
    await row.getByRole('button', { name: 'Tandai selesai' }).click();
    await expect(row).toContainText('Selesai');
    await expect(row.getByRole('button')).toHaveCount(0);
  });

  test('kategori: tambah, ubah nama, hapus; kategori berisi produk ditolak', async ({ page }) => {
    const admin = adminCredentials();
    await loginUI(page, admin.email, admin.password);
    await page.goto('/admin/kategori');
    const name = `Kategori Uji ${Date.now()}`;
    await page.getByLabel('Kategori baru').fill(name);
    await page.getByRole('button', { name: 'Tambah kategori' }).click();
    await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();

    await page.getByRole('button', { name: `Ubah nama ${name}` }).click();
    await page.getByLabel('Nama kategori').fill(`${name} B`);
    await page.getByRole('button', { name: 'Simpan' }).click();
    await expect(page.getByRole('cell', { name: `${name} B`, exact: true })).toBeVisible();

    await page.getByRole('button', { name: `Hapus ${name} B` }).click();
    await page.getByRole('button', { name: 'Hapus', exact: true }).click();
    await expect(page.getByRole('cell', { name: `${name} B`, exact: true })).toHaveCount(0);

    await page.getByRole('button', { name: 'Hapus Biji Kopi Single Origin' }).click();
    await page.getByRole('button', { name: 'Hapus', exact: true }).click();
    await expect(page.getByRole('alert').getByText(/masih punya produk/)).toBeVisible();
  });
});
