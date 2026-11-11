import { expect, test } from '@playwright/test';

const API = 'http://localhost:8081';
const EMAIL = 'pertama@tokoku.local';
const TEMP = 'sementara-uji-123';
const NEW_PASSWORD = 'password-baru-uji-456';

// Dijalankan oleh playwright.firstlogin.config.ts: backend :8081 dengan APP_ADMIN_FORCE_PASSWORD_CHANGE=true dan DB baru.
test('login admin pertama kali: dipaksa ganti password sebelum bisa mengelola toko', async ({ page, request }) => {
  // Server menegakkan: token sementara hanya boleh /me dan ganti password
  const login = await request.post(`${API}/api/admin/auth/login`, { data: { email: EMAIL, password: TEMP } });
  const { accessToken, user } = await login.json();
  expect(user.mustChangePassword).toBe(true);
  const auth = { Authorization: `Bearer ${accessToken}` };
  expect((await request.get(`${API}/api/admin/orders`, { headers: auth })).status()).toBe(403);
  expect((await request.get(`${API}/api/admin/me`, { headers: auth })).status()).toBe(200);

  // UI: login mengarah ke halaman ganti password; halaman admin lain memantul kembali
  await page.goto('/admin/masuk');
  await page.getByLabel('Email').fill(EMAIL);
  await page.getByLabel('Password').fill(TEMP);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/ganti-password/);
  await page.goto('/admin/produk');
  await expect(page).toHaveURL(/\/admin\/ganti-password/);
  await expect(page.getByRole('navigation', { name: 'Kelola toko' })).toHaveCount(0);

  // Validasi sisi klien dan password saat ini salah
  const form = page.locator('form');
  await form.getByLabel('Password saat ini').fill('salah-password-1');
  await form.getByLabel('Password baru', { exact: true }).fill(NEW_PASSWORD);
  await form.getByLabel('Ulangi password baru').fill('beda-sekali-789');
  await form.getByRole('button', { name: 'Simpan password' }).click();
  await expect(page.getByText('Konfirmasi tidak sama.')).toBeVisible();
  await form.getByLabel('Ulangi password baru').fill(NEW_PASSWORD);
  await form.getByRole('button', { name: 'Simpan password' }).click();
  await expect(page.getByRole('alert').getByText('Password saat ini salah.')).toBeVisible();

  // Berhasil: keluar ke halaman masuk, password sementara tidak berlaku lagi
  await form.getByLabel('Password saat ini').fill(TEMP);
  await form.getByRole('button', { name: 'Simpan password' }).click();
  await expect(page).toHaveURL(/\/admin\/masuk/);
  expect((await request.post(`${API}/api/admin/auth/login`, { data: { email: EMAIL, password: TEMP } })).status()).toBe(401);

  // Masuk dengan password baru: langsung ke area admin dan tetap bertahan setelah muat ulang
  await page.getByLabel('Email').fill(EMAIL);
  await page.getByLabel('Password').fill(NEW_PASSWORD);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await expect(page.getByRole('navigation', { name: 'Kelola toko' })).toBeVisible();
  await expect(page).toHaveURL(/\/admin\/produk/);
  await page.reload();
  await expect(page.getByRole('navigation', { name: 'Kelola toko' })).toBeVisible();
});
