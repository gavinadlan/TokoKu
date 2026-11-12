import { expect, test } from '@playwright/test';
import { adminLoginUI, apiPaidOrder, apiUser, API } from './helpers';

test.describe('admin', () => {
  test('buat produk lewat form, muncul di katalog publik, lalu dinonaktifkan', async ({ page }) => {
    await adminLoginUI(page);
    await page.goto('/admin/produk');
    const name = `Kopi Uji ${Date.now()}`;

    await page.getByRole('link', { name: 'Tambah produk' }).click();
    await expect(page.getByRole('heading', { name: 'Produk baru', level: 1 })).toBeVisible();
    await page.getByLabel('Nama produk').fill(name);
    await page.getByRole('combobox', { name: 'Kategori' }).click();
    await page.getByRole('option').first().click();
    await page.getByLabel('Harga').fill('1450000');
    await expect(page.getByLabel('Harga')).toHaveValue('1.450.000'); // diformat saat mengetik
    await page.getByLabel('Stok').fill('7');
    await page.getByRole('button', { name: 'Buat produk' }).click();

    // setelah dibuat: halaman ubah dengan pengelola foto
    await expect(page.getByRole('heading', { name, level: 1 })).toBeVisible();
    await expect(page.getByRole('heading', { name: /Foto produk/ })).toBeVisible();

    await page.goto('/admin/produk');
    await page.getByLabel('Cari nama').fill(name);
    const row = page.getByRole('row', { name: new RegExp(name) });
    await expect(row).toBeVisible();
    await expect(row).toContainText('Rp 1.450.000');

    await page.goto(`/?q=${encodeURIComponent(name)}`);
    await expect(page.getByRole('link', { name: new RegExp(name) })).toBeVisible();

    await page.goto('/admin/produk');
    await page.getByLabel('Cari nama').fill(name);
    await page.getByRole('button', { name: `Aksi untuk ${name}` }).click();
    await page.getByRole('menuitem', { name: 'Nonaktifkan' }).click();
    await page.getByRole('button', { name: 'Nonaktifkan', exact: true }).click();
    await expect(page.getByRole('row', { name: new RegExp(name) })).toContainText('Nonaktif');
    await page.goto(`/?q=${encodeURIComponent(name)}`);
    await expect(page.getByRole('heading', { name: /Tidak ada produk untuk/ })).toBeVisible();
  });

  test('tabel produk bisa diurutkan per kolom', async ({ page }) => {
    await adminLoginUI(page);
    await page.goto('/admin/produk');
    const harga = page.getByRole('columnheader', { name: 'Harga' });
    await harga.getByRole('button').click();
    await expect(harga).toHaveAttribute('aria-sort', 'ascending');
    await expect.poll(async () => {
      const nums = (await page.locator('.tbl tbody tr td.n:nth-child(3)').allInnerTexts()).map((t) => Number(t.replace(/\D/g, '')));
      return nums.length > 1 && nums.every((n, i) => i === 0 || nums[i - 1]! <= n);
    }).toBe(true);
    await harga.getByRole('button').click();
    await expect(harga).toHaveAttribute('aria-sort', 'descending');
  });

  test('kategori: tambah, ubah nama, hapus; kategori berisi produk ditolak', async ({ page }) => {
    await adminLoginUI(page, undefined, undefined, '/admin/kategori');
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

  test('pengaturan toko: simpan dan tampil di info publik; banner hilang saat lengkap', async ({ page, request }) => {
    await adminLoginUI(page, undefined, undefined, '/admin/pengaturan');
    await expect(page.getByLabel('Nama toko')).toHaveValue('TokoKu');
    await page.getByLabel('Jam buka').fill('Senin sampai Sabtu, 09.00 sampai 17.00');
    await page.getByRole('button', { name: 'Simpan pengaturan' }).click();
    await expect(page.getByRole('status').getByText('Pengaturan toko disimpan.')).toBeVisible();
    const store = await (await request.get(`${API}/api/store`)).json();
    expect(store.openingHours).toBe('Senin sampai Sabtu, 09.00 sampai 17.00');
    expect(store.configured).toBe(true);
    await expect(page.getByText('Lengkapi pengaturan toko.')).toHaveCount(0);
  });

  test('order kirim: admin mengisi kurir dan resi lewat dialog, status mengikuti jalur', async ({ page, request }) => {
    const customer = await apiUser(request);
    const orderId = await apiPaidOrder(request, customer.token, 'DELIVERY');
    await adminLoginUI(page, undefined, undefined, '/admin/order');

    const row = page.getByRole('row').filter({ hasText: String(orderId).padStart(4, '0') }).first();
    await expect(row).toContainText('Dibayar');
    await row.getByRole('button', { name: 'Proses pesanan' }).click();
    await expect(row).toContainText('Diproses');

    await row.getByRole('button', { name: 'Tandai dikirim' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByRole('button', { name: 'Simpan dan tandai dikirim' }).click(); // kosong: ditolak browser (required)
    await expect(dialog).toBeVisible();
    await dialog.getByLabel('Kurir').fill('JNE');
    await dialog.getByLabel('Nomor resi').fill('CGK1234567890');
    await dialog.getByRole('button', { name: 'Simpan dan tandai dikirim' }).click();
    await expect(row).toContainText('Dikirim');

    await row.getByRole('button', { name: 'Tandai selesai' }).click();
    await expect(row).toContainText('Selesai');
    await expect(row.getByRole('button', { name: /Proses|Tandai|Selesaikan|Siap/ })).toHaveCount(0);
  });
});
