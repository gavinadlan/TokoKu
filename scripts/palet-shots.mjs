#!/usr/bin/env node
// Screenshot tiap varian palet (home, produk, checkout, admin) di 1440x900 dan 390x844 -> docs/design/palet/img/.
// Butuh backend dev + frontend dev hidup dan data demo portofolio (docs/portfolio/tokoku/.demo-credentials).
//   node scripts/palet-shots.mjs            semua varian
//   node scripts/palet-shots.mjs a c        hanya varian tertentu
import { createRequire } from 'node:module';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const { chromium } = createRequire(`${ROOT}frontend/`)('@playwright/test');
const OUT = `${ROOT}docs/design/palet/img/`;
const WEB = process.env.WEB_URL ?? 'http://localhost:5173';
const API = process.env.API_URL ?? 'http://localhost:8080';
mkdirSync(OUT, { recursive: true });

const VARIAN = process.argv.slice(2).length ? process.argv.slice(2) : ['base', 'a', 'b', 'c'];
const kv = (f) => Object.fromEntries(readFileSync(f, 'utf8').split('\n').filter(Boolean).map((l) => l.split(/[=\s]+(.*)/s).slice(0, 2)));
const adminEnv = kv(`${ROOT}.dev-admin.env`);
const [buyerEmail, buyerPass] = readFileSync(`${ROOT}docs/portfolio/tokoku/.demo-credentials`, 'utf8').split('\n')[0].split(/\s+/);

const NO_MOTION = `*,*::before,*::after{animation:none!important;transition:none!important;caret-color:transparent!important;scroll-behavior:auto!important}
::-webkit-scrollbar{display:none!important}html{scrollbar-width:none!important}`;
const browser = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb'] });
const VIEW = {
  d: { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1 },
  m: { viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true },
};

async function api(method, path, token, body) {
  const res = await fetch(`${API}${path}`, { method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  if (!res.ok) throw new Error(`${method} ${path} ${res.status}`);
  return res;
}
const buyerToken = (await (await api('POST', '/api/auth/login', null, { email: buyerEmail, password: buyerPass })).json()).accessToken;
const pid = async (q) => (await (await api('GET', `/api/products?q=${encodeURIComponent(q)}`)).json()).content[0].id;
const [GAYO, V60, MUG] = [await pid('Gayo'), await pid('Dripper V60'), await pid('Mug Keramik')];
const cart = await (await api('GET', '/api/cart', buyerToken)).json();
for (const it of cart.items ?? []) await api('DELETE', `/api/cart/items/${it.id}`, buyerToken);
for (const [productId, quantity] of [[GAYO, 2], [V60, 1], [MUG, 1]]) await api('POST', '/api/cart/items', buyerToken, { productId, quantity });

async function newPage(view, palet) {
  const ctx = await browser.newContext({ ...VIEW[view], locale: 'id-ID', timezoneId: 'Asia/Jakarta', reducedMotion: 'reduce', baseURL: WEB });
  const page = await ctx.newPage();
  page.setDefaultTimeout(20000);
  await page.addInitScript((p) => { localStorage.setItem('tokoku.devBanner', 'hidden'); }, palet);
  page.palet = palet; // atribut dipasang di settle(): saat init script jalan, <html> belum ada
  return page;
}
async function login(page, email, password, admin) {
  await page.goto(admin ? '/admin/masuk' : '/masuk');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await page.getByRole('button', { name: /Menu akun/ }).or(page.getByRole('navigation', { name: 'Kelola toko' })).first().waitFor();
}
async function settle(page) {
  await page.addStyleTag({ content: NO_MOTION });
  await page.evaluate((p) => { document.documentElement.dataset.palet = p; document.querySelector('meta[name="theme-color"]')?.setAttribute('content', getComputedStyle(document.documentElement).getPropertyValue('--paper').trim()); }, page.palet);
  await page.evaluate(async () => {
    await document.fonts.ready;
    [...document.images].forEach((i) => { i.loading = 'eager'; });
    const wait = (ms) => new Promise((r) => setTimeout(r, ms));
    await Promise.all([...document.images].map((i) => (i.complete ? null : Promise.race([new Promise((r) => { i.onload = i.onerror = r; }), wait(8000)]))));
    document.activeElement?.blur?.();
  });
  await page.mouse.move(-10, -10);
  await page.waitForTimeout(300);
}
const shoot = async (page, name) => { await settle(page); await page.screenshot({ path: `${OUT}${name}.png` }); console.log('ok', name); };


for (const v of VARIAN) for (const view of ['d', 'm']) {
  let p = await newPage(view, v);
  await p.goto('/'); await p.locator('.grid .card').first().waitFor();
  await shoot(p, `${v}-home-${view}`);
  await p.goto(`/produk/${V60}`); await p.getByRole('heading', { level: 1 }).waitFor();
  await shoot(p, `${v}-produk-${view}`);
  await p.context().close();

  p = await newPage(view, v);
  await login(p, buyerEmail, buyerPass); // refresh token sekali pakai: login per konteks, bukan storageState
  await p.goto('/checkout'); await p.getByLabel('Nama penerima').waitFor();
  await p.getByLabel('Nama penerima').fill('Siti Rahmawati');
  await p.getByLabel('Nomor telepon').fill('0812-5550-0101');
  await p.getByLabel('Alamat lengkap').fill('Jl. Dago Asri No. 17, Coblong, Kota Bandung 40135');
  await shoot(p, `${v}-checkout-${view}`);
  await p.context().close();

  p = await newPage(view, v);
  await login(p, adminEnv.ADMIN_EMAIL, adminEnv.ADMIN_PASSWORD, true); // sesi admin di sessionStorage, tidak ikut storageState
  await p.goto('/admin/order'); await p.locator('.topbar h1').waitFor(); await p.waitForTimeout(1200);
  await shoot(p, `${v}-admin-${view}`);
  await p.context().close();
}
await browser.close();
