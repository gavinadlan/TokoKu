#!/usr/bin/env node
// Screenshot portofolio TokoKu. Dipanggil oleh scripts/portfolio-shots.sh (backend dev + frontend dev sudah hidup).
// Keluaran: docs/portfolio/tokoku/*.png. Desktop 1440x900 @2x diperkecil ke 1600x1000; ponsel 390x844 @2x.
import { createRequire } from 'node:module';
import { readFileSync, writeFileSync, mkdirSync, rmSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const { chromium } = createRequire(`${ROOT}frontend/`)('@playwright/test');
const OUT = process.env.OUT_DIR ?? `${ROOT}docs/portfolio/tokoku/`;
const WEB = process.env.WEB_URL ?? 'http://localhost:5173';
const API = process.env.API_URL ?? 'http://localhost:8080';
mkdirSync(OUT, { recursive: true });

const kv = (f) => Object.fromEntries(readFileSync(f, 'utf8').split('\n').filter(Boolean).map((l) => l.split(/[=\s]+(.*)/s).slice(0, 2)));
const adminEnv = kv(`${ROOT}.dev-admin.env`);
const [buyerEmail, buyerPass] = readFileSync(`${OUT}.demo-credentials`, 'utf8').split('\n')[0].split(/\s+/);

const NO_MOTION = `*,*::before,*::after{animation:none!important;transition:none!important;caret-color:transparent!important;scroll-behavior:auto!important}
::-webkit-scrollbar{display:none!important}html{scrollbar-width:none!important}`;
const browser = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb'] });

const DESKTOP = { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 };
const PHONE = { viewport: { width: 390, height: 844 }, deviceScaleFactor: 2, isMobile: true, hasTouch: true };

async function open(opts) {
  const ctx = await browser.newContext({ ...opts, locale: 'id-ID', timezoneId: 'Asia/Jakarta', reducedMotion: 'reduce', baseURL: WEB });
  const page = await ctx.newPage();
  page.setDefaultTimeout(20000);
  await page.addInitScript(() => localStorage.setItem('tokoku.devBanner', 'hidden'));
  return page;
}

async function settle(page) {
  await page.addStyleTag({ content: NO_MOTION });
  await page.evaluate(async () => {
    await document.fonts.ready;
    [...document.images].forEach((i) => { i.loading = 'eager'; });
    const wait = (ms) => new Promise((r) => setTimeout(r, ms));
    await Promise.all([...document.images].map((i) => (i.complete ? null : Promise.race([new Promise((r) => { i.onload = i.onerror = r; }), wait(8000)]))));
    await Promise.all([...document.images].map((i) => i.decode().catch(() => {})));
    document.activeElement?.blur?.();
  });
  await page.mouse.move(-10, -10);
  await page.waitForTimeout(400);
}

const SIZE = { desktop: '1600 1000' };
async function shoot(page, name, { full = false, clip, kind = 'desktop' } = {}) {
  await settle(page);
  const path = `${OUT}${name}.png`;
  await page.screenshot({ path, fullPage: full, clip });
  if (kind === 'desktop' && !clip) execFileSync('sips', ['-z', ...SIZE.desktop.split(' ').reverse(), path], { stdio: 'ignore' });
  console.log('ok', name);
}

async function login(page, email, password, admin = false) {
  await page.goto(admin ? '/admin/masuk' : '/masuk');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await page.getByRole('button', { name: /Menu akun/ }).or(page.getByRole('navigation', { name: 'Kelola toko' })).first().waitFor();
}

async function api(method, path, token, body) {
  const res = await fetch(`${API}${path}`, { method, headers: { ...(body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  if (!res.ok) throw new Error(`${method} ${path} ${res.status}`);
  return res;
}
const buyerToken = (await (await api('POST', '/api/auth/login', null, { email: buyerEmail, password: buyerPass })).json()).accessToken;
const adminToken = (await (await api('POST', '/api/admin/auth/login', null, { email: adminEnv.ADMIN_EMAIL, password: adminEnv.ADMIN_PASSWORD })).json()).accessToken;

async function fillCheckout(page) {
  await page.getByLabel('Nama penerima').fill('Siti Rahmawati');
  await page.getByLabel('Nomor telepon').fill('0812-5550-0101');
  await page.getByLabel('Alamat lengkap').fill('Jl. Dago Asri No. 17, Coblong, Kota Bandung 40135');
}

async function fillCart() {
  const cart = await (await api('GET', '/api/cart', buyerToken)).json();
  for (const it of cart.items ?? []) await api('DELETE', `/api/cart/items/${it.id}`, buyerToken);
  for (const [productId, quantity] of [[1, 2], [11, 1], [21, 1]]) await api('POST', '/api/cart/items', buyerToken, { productId, quantity });
}

// ---- Desktop publik
{
  const page = await open(DESKTOP);
  await page.goto('/');
  await page.locator('.grid .card').first().waitFor();
  await shoot(page, 'home');
  await page.goto('/produk/11');
  await page.getByRole('heading', { level: 1 }).waitFor();
  await shoot(page, 'produk-galeri');
  await page.context().close();
}

// ---- Pembeli: checkout (desktop dan ponsel), katalog dan produk ponsel
await fillCart();
{
  const page = await open(DESKTOP);
  await login(page, buyerEmail, buyerPass);
  await page.goto('/checkout');
  await page.getByLabel('Nama penerima').waitFor();
  await fillCheckout(page);
  await shoot(page, 'keranjang-checkout');
  await page.context().close();
}
{
  const page = await open(PHONE);
  await page.goto('/');
  await page.locator('.grid .card').first().waitFor();
  await shoot(page, 'mobile-katalog', { kind: 'phone' });
  await page.goto('/produk/11');
  await page.getByRole('heading', { level: 1 }).waitFor();
  await shoot(page, 'mobile-produk', { kind: 'phone' });
  await login(page, buyerEmail, buyerPass);
  await page.goto('/checkout');
  await page.getByLabel('Nama penerima').waitFor();
  await fillCheckout(page);
  await shoot(page, 'mobile-checkout', { kind: 'phone' });
  await page.context().close();
}

// ---- Admin
{
  const page = await open(DESKTOP);
  await login(page, adminEnv.ADMIN_EMAIL, adminEnv.ADMIN_PASSWORD, true);
  await page.goto('/admin/order');
  await page.getByRole('table').waitFor();
  await shoot(page, 'admin-pesanan');
  await page.goto('/admin/produk/11');
  await page.locator('.imgs .tile img').first().waitFor();
  await page.evaluate(() => { const h = [...document.querySelectorAll('h2,h3')].find((e) => /Foto produk/.test(e.textContent)); window.scrollTo(0, h.getBoundingClientRect().top + scrollY - 190); });
  await shoot(page, 'admin-produk');
  await page.context().close();
}

// ---- Invoice: PDF order selesai pertama -> PNG (QuickLook/sips macOS)
{
  const orders = (await (await api('GET', '/api/admin/orders?status=COMPLETED', adminToken)).json()).content;
  const id = orders.sort((a, b) => a.id - b.id)[1]?.id ?? orders[0].id;
  const pdf = Buffer.from(await (await api('GET', `/api/admin/orders/${id}/invoice`, adminToken)).arrayBuffer());
  const tmp = `${OUT}.invoice.pdf`;
  writeFileSync(tmp, pdf);
  execFileSync('qlmanage', ['-t', '-s', '2400', '-o', OUT, tmp], { stdio: 'ignore' });
  const raw = `${tmp}.png`;
  // kanvas 16:10 krem, halaman A4 di tengah dengan bayangan lembut
  const page = await open({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 1 });
  const b64 = readFileSync(raw).toString('base64');
  // bagian atas halaman (seluruh isi invoice) diperbesar, dipotong halus di bawah
  await page.setContent(`<body style="margin:0;background:#F6F1E9;height:100vh;display:flex;justify-content:center;overflow:hidden"><div style="margin-top:44px;width:1000px;height:956px;overflow:hidden;background:#fff;box-shadow:0 1px 2px rgba(27,22,19,.12),0 18px 40px rgba(27,22,19,.14)"><img src="data:image/png;base64,${b64}" style="width:1000px;display:block"></div></body>`);
  await page.waitForTimeout(500);
  await shoot(page, 'invoice', { kind: 'none' });
  await page.context().close();
  rmSync(tmp); rmSync(raw);
}

// ---- Email struk: pratinjau template (data contoh backend disesuaikan dengan katalog demo)
{
  const store = await (await api('GET', '/api/store')).json();
  let html = await (await fetch(`${API}/api/dev/email-preview/PAID?variant=delivery`)).text();
  const sub = [['Jl. Contoh No. 1, Kota Anda', store.address], ['0800-0000-0000', store.phone], ['halo@tokoku.example', store.email],
    ['Dripper keramik V60 02', 'Dripper V60 Keramik 02'], ['Filter kertas V60 isi 100', 'Kertas Filter V60 02 isi 100'], ['pembeli@example.com', 'siti.rahmawati@surel.test'],
    ['125.500', '165.000'], ['12.500', '42.000'], ['37.500', '126.000'], ['333.000', '461.000'], ['348.000', '476.000'], ['08.00 sampai 17.00', '09.00 sampai 17.00']];
  for (const [a, b] of sub) html = html.replaceAll(a, b);
  // 1000x625 @1.6 = 1600x1000; zoom memuat kolom email dari kepala sampai total
  const page = await open({ viewport: { width: 1000, height: 625 }, deviceScaleFactor: 1.6 });
  await page.setContent(html);
  await page.addStyleTag({ content: 'html{background:#F6F1E9}body{zoom:.72}' });
  await shoot(page, 'email-struk', { kind: 'none' });
  await page.context().close();
}

// ---- Mockup gabungan: laptop + ponsel generik (tanpa merek) di atas krem
{
  const b64 = (f) => readFileSync(`${OUT}${f}`).toString('base64');
  const page = await open({ viewport: { width: 1600, height: 1000 }, deviceScaleFactor: 1 });
  await page.setContent(`<body style="margin:0;height:100vh;background:#F6F1E9;position:relative;overflow:hidden">
    <div style="position:absolute;left:70px;top:190px;width:1150px">
      <div style="background:#1B1613;border-radius:18px 18px 6px 6px;padding:14px 14px 18px;box-shadow:0 30px 60px rgba(27,22,19,.22)"><img style="display:block;width:100%;border-radius:4px" src="data:image/png;base64,${b64('home.png')}"></div>
      <div style="height:16px;margin:0 -50px;background:linear-gradient(#CFC6B8,#B9AFA0);border-radius:0 0 22px 22px;box-shadow:0 14px 24px rgba(27,22,19,.18)"></div>
    </div>
    <div style="position:absolute;left:1270px;top:215px;width:290px;background:#1B1613;border-radius:44px;padding:11px;box-shadow:0 30px 60px rgba(27,22,19,.28)">
      <img style="display:block;width:100%;border-radius:34px" src="data:image/png;base64,${b64('mobile-katalog.png')}"></div></body>`);
  await page.waitForTimeout(500);
  await shoot(page, 'mockup-gabungan', { kind: 'none' });
  await page.context().close();
}

// ---- Optimasi PNG bila pngquant ada (target kira-kira di bawah 600 KB)
try {
  for (const f of ['home', 'produk-galeri', 'keranjang-checkout', 'admin-pesanan', 'admin-produk', 'email-struk', 'invoice', 'mockup-gabungan', 'mobile-katalog', 'mobile-produk', 'mobile-checkout'])
    execFileSync('pngquant', ['--quality=80-98', '--speed', '1', '--force', '--ext', '.png', `${OUT}${f}.png`], { stdio: 'ignore' });
} catch { console.log('pngquant tidak tersedia atau gagal; PNG tidak dioptimalkan'); }

await browser.close();
