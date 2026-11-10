import { expect, type APIRequestContext, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export const API = 'http://localhost:8080';
export const PASSWORD = 'password-uji-123';

/** Kredensial admin lokal dibuat oleh scripts/dev-backend.sh dan disimpan di .dev-admin.env (di-gitignore). */
export function adminCredentials(): { email: string; password: string } {
  const text = readFileSync(fileURLToPath(new URL('../../.dev-admin.env', import.meta.url)), 'utf8');
  const get = (k: string) => text.match(new RegExp(`^${k}=(.*)$`, 'm'))?.[1]?.trim() ?? '';
  return { email: get('ADMIN_EMAIL'), password: get('ADMIN_PASSWORD') };
}

export const fixture = (name: string) => fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url));
export const uniqueEmail = () => `uji-${Date.now()}-${Math.floor(Math.random() * 1e6)}@example.com`;

/** Login pembeli lewat UI toko. */
export async function loginUI(page: Page, email: string, password: string) {
  await page.goto('/masuk');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await expect(page.getByRole('button', { name: /Menu akun/ })).toBeVisible();
}

/** Login admin lewat halaman admin (sesi terpisah dari toko). */
export async function adminLoginUI(page: Page, email?: string, password?: string, to = '/admin/produk') {
  const creds = adminCredentials();
  await page.goto('/admin/masuk');
  await page.getByLabel('Email').fill(email ?? creds.email);
  await page.getByLabel('Password').fill(password ?? creds.password);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await expect(page.getByRole('navigation', { name: 'Kelola toko' })).toBeVisible();
  if (to !== '/admin/produk') await page.goto(to);
}

export async function apiAdminToken(request: APIRequestContext): Promise<string> {
  const c = adminCredentials();
  const res = await request.post(`${API}/api/admin/auth/login`, { data: { email: c.email, password: c.password } });
  expect(res.status()).toBe(200);
  return (await res.json()).accessToken as string;
}

/** Membuat pengguna lewat API backend (lebih cepat dari UI) dan mengembalikan token akses. */
export async function apiUser(request: APIRequestContext) {
  const email = uniqueEmail();
  const reg = await request.post(`${API}/api/auth/register`, { data: { email, password: PASSWORD, name: 'Penguji Otomatis' } });
  expect(reg.status()).toBe(201);
  const login = await request.post(`${API}/api/auth/login`, { data: { email, password: PASSWORD } });
  const { accessToken } = await login.json();
  return { email, password: PASSWORD, token: accessToken as string };
}

/** Order lewat API: checkout (kirim atau ambil), bayar, simulasi settlement. Mengembalikan id order. */
export async function apiPaidOrder(request: APIRequestContext, token: string, type: 'DELIVERY' | 'PICKUP' = 'DELIVERY', query = 'gayo'): Promise<number> {
  const auth = { Authorization: `Bearer ${token}` };
  const products = await (await request.get(`${API}/api/products?q=${query}`)).json();
  const productId = products.content[0].id as number;
  await request.post(`${API}/api/cart/items`, { headers: auth, data: { productId, quantity: 1 } });
  const order = await (await request.post(`${API}/api/orders`, {
    headers: auth,
    data: { fulfillmentType: type, shippingName: 'Penguji', shippingPhone: '081234567890', shippingAddress: type === 'DELIVERY' ? 'Jl. Uji Coba No. 1, Jakarta' : undefined },
  })).json();
  const pay = await (await request.post(`${API}/api/orders/${order.id}/pay`, { headers: auth })).json();
  const sim = await request.post(`${API}/api/dev/payments/${pay.providerRef}/simulate?status=settlement`);
  expect(sim.ok()).toBeTruthy();
  return order.id as number;
}
