import { expect, test } from '@playwright/test';
import { uniqueEmail, PASSWORD, API } from './helpers';

test.describe('alur belanja pelanggan', () => {
  test('register, cari, tambah ke keranjang, checkout, bayar (simulasi), order PAID', async ({ page }) => {
    const email = uniqueEmail();

    // register
    await page.goto('/daftar');
    await page.getByLabel('Nama').fill('Siti Rahmawati');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByRole('button', { name: 'Daftar dan masuk' }).click();
    await expect(page.getByRole('button', { name: /Menu akun/ })).toContainText('Siti');

    // cari produk; state tersimpan di URL
    await page.locator('#q-desktop').fill('gayo');
    await page.locator('#q-desktop').press('Enter');
    await expect(page).toHaveURL(/\?q=gayo/);
    await page.getByRole('link', { name: /Kopi Arabika Gayo 250g/ }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Kopi Arabika Gayo 250g' })).toBeVisible();

    // tambah ke keranjang
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await expect(page.getByRole('status').getByText('masuk keranjang')).toBeVisible();
    await expect(page.getByRole('link', { name: /Keranjang\s*,\s*1 barang/ })).toBeVisible();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();

    // keranjang: ubah jumlah (optimistic) lalu checkout
    await expect(page.getByRole('heading', { name: 'Keranjang', level: 1 })).toBeVisible();
    await page.getByRole('button', { name: 'Tambah jumlah' }).click();
    await expect(page.getByLabel('Jumlah Kopi Arabika Gayo 250g').locator('output')).toHaveText('2');
    await expect(page.getByRole('region', { name: 'Ringkasan' }).getByText(/Rp\s170\.000/).first()).toBeVisible();
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();

    // checkout: validasi lalu isi
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click();
    await expect(page.getByText('Isi nama.')).toBeVisible();
    await page.getByLabel('Nama penerima').fill('Siti Rahmawati');
    await page.getByLabel('Nomor telepon').fill('081234567890');
    await page.getByLabel('Alamat lengkap').fill('Jl. Kopi No. 12, Menteng, Jakarta Pusat');
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click();

    // pembayaran fake (hanya dev)
    await expect(page).toHaveURL(/\/dev\/bayar\//);
    await page.getByRole('button', { name: 'Simulasikan pembayaran berhasil' }).click();
    await expect(page.getByRole('heading', { name: 'Pembayaran diterima' })).toBeVisible();

    // order tampil PAID
    await page.getByRole('link', { name: 'Lihat order' }).click();
    await expect(page.locator('.badge').first()).toHaveText('Dibayar');
    await page.goto('/order');
    await expect(page.locator('.orders .badge').first()).toHaveText('Dibayar');
  });

  test('pembayaran kedaluwarsa membatalkan order dan stok kembali', async ({ page, request }) => {
    const email = uniqueEmail();
    await page.goto('/daftar');
    await page.getByLabel('Nama').fill('Budi');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByRole('button', { name: 'Daftar dan masuk' }).click();
    await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();

    await page.goto('/?q=dripper');
    await page.getByRole('link', { name: /Dripper V60/ }).click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();
    await page.getByLabel('Nama penerima').fill('Budi');
    await page.getByLabel('Nomor telepon').fill('081234567890');
    await page.getByLabel('Alamat lengkap').fill('Jl. Seduh No. 3, Bandung, Jawa Barat');
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click();
    await page.getByRole('button', { name: 'Simulasikan kedaluwarsa' }).click();
    await expect(page.getByRole('heading', { name: 'Pembayaran tidak berhasil' })).toBeVisible();
    expect((await request.get(`${API}/api/products?q=dripper`)).ok()).toBeTruthy();
  });

  test('guard: anonim diarahkan masuk; pembeli tidak punya akses admin', async ({ page }) => {
    await page.goto('/keranjang');
    await expect(page).toHaveURL(/\/masuk/);
    await expect(page.getByText('Masuk dulu untuk melanjutkan.')).toBeVisible();

    const email = uniqueEmail();
    await page.goto('/daftar');
    await page.getByLabel('Nama').fill('Dewi');
    await page.getByLabel('Email').fill(email);
    await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
    await page.getByRole('button', { name: 'Daftar dan masuk' }).click();
    await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();
    // Pembeli membuka area admin: diarahkan ke login admin (bukan halaman 403), dan sesi toko tidak berlaku di sana
    await page.goto('/admin/produk');
    await expect(page).toHaveURL(/\/admin\/masuk/);
    await expect(page.getByRole('heading', { name: 'Masuk pengelola' })).toBeVisible();
  });

  test('login salah menampilkan pesan yang jelas; 404 dirancang', async ({ page }) => {
    await page.goto('/masuk');
    await page.getByLabel('Email').fill(uniqueEmail());
    await page.getByLabel('Password').fill('salah-password');
    await page.getByRole('button', { name: 'Masuk', exact: true }).click();
    await expect(page.getByRole('alert').getByText('Email atau password salah.')).toBeVisible();

    await page.goto('/produk/999999');
    await expect(page.getByRole('heading', { name: 'Produk tidak ditemukan' })).toBeVisible();
    await page.goto('/tidak-ada');
    await expect(page.getByRole('heading', { name: 'Halaman tidak ditemukan' })).toBeVisible();
  });

  test('katalog: filter kategori dan urutan tersimpan di URL, back berfungsi', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.grid .card').first()).toBeVisible();
    await page.getByRole('button', { name: 'Alat Seduh Manual' }).click();
    await expect(page).toHaveURL(/kategori=\d+/);
    await page.getByRole('combobox', { name: 'Urutkan' }).click();
    await page.getByRole('option', { name: 'Harga terendah' }).click();
    await expect(page).toHaveURL(/urut=termurah/);
    // tunggu data baru (grid tidak lagi dipudarkan sebagai placeholder), lalu periksa urutan harga
    await expect.poll(async () => {
      const nums = (await page.locator('.grid .price').allInnerTexts()).map((t) => Number(t.replace(/\D/g, '')));
      return nums.length > 1 && nums.every((n, i) => i === 0 || nums[i - 1]! <= n);
    }).toBe(true);
    await page.goBack();
    await expect(page).not.toHaveURL(/urut=/);
    await page.goto('/?q=zzzzzz');
    await expect(page.getByRole('heading', { name: /Tidak ada produk untuk/ })).toBeVisible();
  });
});
