#!/usr/bin/env node
// Screenshot halaman admin di 360/390/768/1440 + audit scroll horizontal halaman. Butuh backend+frontend dev dan data demo portofolio.
//   node scripts/admin-shots.mjs sebelum|sesudah
import { createRequire } from 'node:module';
import { readFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));
const { chromium } = createRequire(`${ROOT}frontend/`)('@playwright/test');
const TAG = process.argv[2] ?? 'sesudah';
const OUT = `${ROOT}docs/design/admin-mobile/${TAG}/`;
const WEB = process.env.WEB_URL ?? 'http://localhost:5173';
const API = process.env.API_URL ?? 'http://localhost:8080';
mkdirSync(OUT, { recursive: true });
const kv = (f) => Object.fromEntries(readFileSync(f, 'utf8').split('\n').filter(Boolean).map((l) => l.split(/[=\s]+(.*)/s).slice(0, 2)));
const adm = kv(`${ROOT}.dev-admin.env`);
const prod = (await (await fetch(`${API}/api/products?q=Gayo`)).json()).content[0].id;
const PAGES = { order: '/admin/order', produk: '/admin/produk', kategori: '/admin/kategori', pengaturan: '/admin/pengaturan', 'produk-edit': `/admin/produk/${prod}` };
const WIDTHS = [360, 390, 768, 1440];
const NO_MOTION = '*,*::before,*::after{animation:none!important;transition:none!important;caret-color:transparent!important}';
const browser = await chromium.launch({ args: ['--hide-scrollbars'] });
let overflow = 0;
for (const w of WIDTHS) {
  const mobile = w < 768;
  const ctx = await browser.newContext({ viewport: { width: w, height: w === 1440 ? 900 : 844 }, deviceScaleFactor: mobile ? 2 : 1, isMobile: mobile, hasTouch: mobile, locale: 'id-ID', timezoneId: 'Asia/Jakarta', reducedMotion: 'reduce', baseURL: WEB });
  const page = await ctx.newPage();
  page.setDefaultTimeout(20000);
  await page.addInitScript(() => localStorage.setItem('tokoku.devBanner', 'hidden'));
  await page.goto('/admin/masuk');
  await page.getByLabel('Email').fill(adm.ADMIN_EMAIL); await page.getByLabel('Password').fill(adm.ADMIN_PASSWORD);
  await page.getByRole('button', { name: 'Masuk', exact: true }).click();
  await page.getByRole('navigation', { name: 'Kelola toko' }).waitFor();
  for (const [name, path] of Object.entries(PAGES)) {
    await page.goto(path); await page.locator('.topbar h1').waitFor(); await page.waitForTimeout(1200);
    await page.addStyleTag({ content: NO_MOTION });
    await page.evaluate(async () => { [...document.images].forEach((i) => { i.loading = 'eager'; }); await Promise.race([Promise.all([...document.images].map((i) => i.decode().catch(() => {}))), new Promise((r) => setTimeout(r, 6000))]); });
    // halaman melebar, atau tabel tampak yang isinya terpotong di kanan (menggulir di dalam kotaknya)
    const over = await page.evaluate(() => Math.max(document.documentElement.scrollWidth - (window.visualViewport?.width ?? window.innerWidth), ...[...document.querySelectorAll('.tablebox')].filter((b) => b.offsetParent).map((b) => b.scrollWidth - b.clientWidth)));
    if (over > 0) overflow++;
    console.log(`${String(w).padEnd(5)} ${name.padEnd(12)} ${over > 0 ? 'SCROLL-X +' + over + 'px' : 'ok'}`);
    await page.screenshot({ path: `${OUT}${name}-${w}.png`, fullPage: name === 'order' || name === 'produk' ? false : false });
  }
  await ctx.close();
}
await browser.close();
process.exit(overflow ? 1 : 0);
