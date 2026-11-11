import { expect, test } from '@playwright/test';
import { adminCredentials, adminLoginUI, apiUser, loginUI } from './helpers';

test.describe('login admin dan pembeli terpisah', () => {
  test('akun admin ditolak di login toko, akun pembeli ditolak di login admin, dengan pesan yang sama', async ({ page, request }) => {
    const admin = adminCredentials();
    await page.goto('/masuk');
    await page.getByLabel('Email').fill(admin.email);
    await page.getByLabel('Password').fill(admin.password);
    await page.getByRole('button', { name: 'Masuk', exact: true }).click();
    await expect(page.getByRole('alert').getByText('Email atau password salah.')).toBeVisible();

    const user = await apiUser(request);
    await page.goto('/admin/masuk');
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Password').fill(user.password);
    await page.getByRole('button', { name: 'Masuk', exact: true }).click();
    await expect(page.getByRole('alert').getByText('Email atau password salah.')).toBeVisible();
    await expect(page).toHaveURL(/\/admin\/masuk/);
  });

  test('halaman login toko tidak menyebut admin, dan header toko tidak punya tautan admin', async ({ page, request }) => {
    await page.goto('/masuk');
    await expect(page.locator('body')).not.toContainText(/admin/i);
    const user = await apiUser(request);
    await loginUI(page, user.email, user.password);
    await page.getByRole('button', { name: /Menu akun/ }).click();
    await expect(page.getByRole('menuitem')).toHaveText(['Riwayat order', 'Keluar']);
    await expect(page.locator('header a[href^="/admin"]')).toHaveCount(0);
  });

  test('satu browser bisa masuk sebagai pembeli dan admin sekaligus tanpa saling menimpa', async ({ page, context, request }) => {
    const user = await apiUser(request);
    await loginUI(page, user.email, user.password);
    const adminPage = await context.newPage();
    await adminLoginUI(adminPage);

    // penyimpanan terpisah: refresh toko di localStorage, refresh admin di sessionStorage
    expect(await page.evaluate(() => localStorage.getItem('tokoku.refresh'))).toBeTruthy();
    expect(await adminPage.evaluate(() => sessionStorage.getItem('tokoku.admin.refresh'))).toBeTruthy();
    expect(await adminPage.evaluate(() => localStorage.getItem('tokoku.admin.refresh'))).toBeNull();

    // muat ulang keduanya: sesi masing-masing pulih
    await page.reload();
    await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();
    await adminPage.reload();
    await expect(adminPage.getByRole('navigation', { name: 'Kelola toko' })).toBeVisible();

    // admin keluar: sesi pembeli tetap
    await adminPage.getByRole('button', { name: 'Keluar' }).click();
    await expect(adminPage).toHaveURL(/\/admin\/masuk/);
    await page.reload();
    await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();

    // pembeli keluar: tidak memengaruhi login admin berikutnya
    await page.getByRole('button', { name: /Menu akun/ }).click();
    await page.getByRole('menuitem', { name: 'Keluar' }).click();
    await adminLoginUI(adminPage);
  });

  test('area admin tanpa sesi admin mengarah ke login admin dan memakai noindex', async ({ page }) => {
    await page.goto('/admin/order');
    await expect(page).toHaveURL(/\/admin\/masuk/);
    await expect(page.locator('meta[name="robots"]')).toHaveAttribute('content', /noindex/);
    await expect(page.locator('header.site-header')).toHaveCount(0); // tanpa header toko
  });
});
