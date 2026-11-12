import { expect, test } from '@playwright/test';
import { PASSWORD, adminLoginUI, apiPaidOrder, apiUser, loginUI, uniqueEmail, API } from './helpers';

async function register(page: import('@playwright/test').Page, name = 'Siti Rahmawati') {
  const email = uniqueEmail();
  await page.goto('/daftar');
  await page.getByLabel('Nama').fill(name);
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Daftar dan masuk' }).click();
  await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();
  return email;
}

test.describe('ambil di toko dan pengiriman', () => {
  test('pengiriman: ongkir benar di ringkasan dan order, resi diisi admin dan terlihat pembeli', async ({ page, context, request }) => {
    await register(page);
    await page.goto('/?q=gayo');
    await page.getByRole('link', { name: /Kopi Arabika Gayo/ }).click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();

    // default: kirim. Subtotal 85.000 < ambang 500.000 -> ongkir 15.000, total 100.000
    const summary = page.getByRole('region', { name: 'Pesanan Anda' });
    await expect(summary).toContainText('Biaya kirim');
    await expect(summary.getByText(/Rp\s15\.000/)).toBeVisible();
    await expect(summary.locator('.total b')).toHaveText(/Rp\s100\.000/);
    await expect(summary).toContainText('Tambah');
    await page.getByLabel('Nama penerima').fill('Siti Rahmawati');
    await page.getByLabel('Nomor telepon').fill('081234567890');
    await page.getByLabel('Alamat lengkap').fill('Jl. Kopi No. 12, Menteng, Jakarta Pusat');
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click();
    await expect(page).toHaveURL(/\/dev\/bayar\/(.+)\?order=(\d+)/);
    const orderId = Number(new URL(page.url()).searchParams.get('order'));
    await page.getByRole('button', { name: 'Simulasikan pembayaran berhasil' }).click();
    await expect(page.getByRole('heading', { name: 'Pembayaran diterima' })).toBeVisible();

    // admin: proses lalu kirim dengan resi
    const adminPage = await context.newPage();
    await adminLoginUI(adminPage, undefined, undefined, '/admin/order');
    const row = adminPage.getByRole('row').filter({ hasText: String(orderId).padStart(4, '0') }).first();
    await row.getByRole('button', { name: 'Proses pesanan' }).click();
    await expect(row).toContainText('Diproses');
    await row.getByRole('button', { name: 'Tandai dikirim' }).click();
    await adminPage.getByLabel('Kurir').fill('JNE');
    await adminPage.getByLabel('Nomor resi').fill('CGK1234567890');
    await adminPage.getByRole('button', { name: 'Simpan dan tandai dikirim' }).click();
    await expect(row).toContainText('Dikirim');

    // pembeli melihat linimasa kirim dengan resi, ongkir, dan total yang konsisten
    await page.goto(`/order/${orderId}`);
    const timeline = page.getByRole('list', { name: 'Linimasa pesanan' });
    await expect(timeline).toContainText('Dikirim');
    await expect(timeline).toContainText('CGK1234567890');
    await expect(timeline).not.toContainText('Siap diambil');
    const rincian = page.getByRole('region', { name: 'Rincian' });
    await expect(rincian).toContainText('Biaya kirim');
    await expect(rincian.locator('.total b')).toHaveText(/Rp\s100\.000/);
    void request;
  });

  test('ambil di toko: tanpa alamat dan ongkir, kode ambil muncul saat siap, selesai dengan kode (huruf kecil juga)', async ({ page, context }) => {
    await register(page, 'Budi Santoso');
    await page.goto('/?q=dripper');
    await page.getByRole('link', { name: /Dripper V60/ }).click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await page.getByRole('link', { name: 'Lihat keranjang' }).click();
    await page.getByRole('button', { name: 'Lanjut ke checkout' }).click();

    await page.getByRole('radio', { name: /Ambil di toko/ }).check();
    await expect(page.getByLabel('Alamat lengkap')).toHaveCount(0);
    await expect(page.getByRole('region', { name: 'Lokasi pengambilan' })).toContainText('Jl. Contoh No. 1');
    const summary = page.getByRole('region', { name: 'Pesanan Anda' });
    await expect(summary).toContainText('Ambil di toko');
    await expect(summary).toContainText('Gratis');
    await expect(summary.locator('.total b')).toHaveText(/Rp\s165\.000/);
    await page.getByLabel('Nama pengambil').fill('Budi Santoso');
    await page.getByLabel('Nomor telepon').fill('081234567890');
    await page.getByRole('button', { name: 'Buat pesanan dan bayar' }).click();
    await expect(page).toHaveURL(/\/dev\/bayar\//);
    const orderId = Number(new URL(page.url()).searchParams.get('order'));
    await page.getByRole('button', { name: 'Simulasikan pembayaran berhasil' }).click();
    await page.getByRole('link', { name: 'Lihat order' }).click();
    await expect(page.getByRole('list', { name: 'Linimasa pesanan' })).not.toContainText('Kode ambil'); // belum siap

    const adminPage = await context.newPage();
    await adminLoginUI(adminPage, undefined, undefined, '/admin/order');
    const row = adminPage.getByRole('row').filter({ hasText: String(orderId).padStart(4, '0') }).first();
    await expect(row).toContainText('Ambil di toko');
    await row.getByRole('button', { name: 'Proses pesanan' }).click();
    await expect(row).toContainText('Diproses');
    await row.getByRole('button', { name: 'Siap diambil' }).click();
    await expect(row).toContainText('Siap diambil');

    // pembeli melihat kode ambil
    await page.reload();
    const codeEl = page.locator('.tl .code');
    await expect(codeEl).toBeVisible();
    const code = (await codeEl.innerText()).trim();
    expect(code).toMatch(/^[ABCDEFGHJKMNPQRSTUVWXYZ2-9]{6}$/);

    // admin: kode salah ditolak, kode benar (huruf kecil) menyelesaikan
    await row.getByRole('button', { name: 'Selesaikan (kode)' }).click();
    const dialog = adminPage.getByRole('dialog');
    await dialog.getByLabel('Kode ambil dari pembeli').fill('AAAAAA');
    await dialog.getByRole('button', { name: 'Verifikasi dan selesaikan' }).click();
    await expect(dialog.getByRole('alert')).toContainText(/tidak/i);
    await dialog.getByLabel('Kode ambil dari pembeli').fill(code.toLowerCase());
    await dialog.getByRole('button', { name: 'Verifikasi dan selesaikan' }).click();
    await expect(row).toContainText('Selesai');
    await page.reload();
    await expect(page.locator('.badge').first()).toHaveText('Selesai');
  });

  test('invoice: unduh PDF berstatus 200 bertipe application/pdf dan berkas valid; belum dibayar tidak punya invoice', async ({ page, request }) => {
    const user = await apiUser(request);
    const orderId = await apiPaidOrder(request, user.token, 'DELIVERY');
    await loginUI(page, user.email, user.password);
    await page.goto(`/order/${orderId}`);
    await expect(page.getByRole('region', { name: 'Rincian' })).toBeVisible();

    const [response, download] = await Promise.all([
      page.waitForResponse((r) => r.url().includes(`/api/orders/${orderId}/invoice`)),
      page.waitForEvent('download'),
      page.getByRole('button', { name: 'Unduh PDF' }).click(),
    ]);
    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('application/pdf');
    expect(download.suggestedFilename()).toMatch(/^INV-\d{6}-\d{5}\.pdf$/);
    const path = await download.path();
    const head = (await import('node:fs')).readFileSync(path!).subarray(0, 5).toString('ascii');
    expect(head).toBe('%PDF-');

    // order yang belum dibayar: tidak ada tombol invoice
    await page.goto('/?q=gayo');
    await page.getByRole('link', { name: /Kopi Arabika Gayo/ }).click();
    await page.getByRole('button', { name: 'Tambah ke keranjang' }).click();
    await expect(page.getByRole('status').getByText('masuk keranjang')).toBeVisible(); // tunggu item benar-benar masuk
    const auth = { Authorization: `Bearer ${user.token}` };
    const unpaid = await (await request.post(`${API}/api/orders`, { headers: auth, data: { fulfillmentType: 'DELIVERY', shippingName: 'U', shippingPhone: '0812345678', shippingAddress: 'Jl. Uji Coba No. 1, Jakarta' } })).json();
    await page.goto(`/order/${unpaid.id}`);
    await expect(page.getByRole('button', { name: 'Bayar sekarang' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Unduh PDF' })).toHaveCount(0);
  });
});
