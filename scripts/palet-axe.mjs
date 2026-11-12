#!/usr/bin/env node
// Pindai axe (WCAG A/AA, termasuk color-contrast) tiap varian pada home, produk, masuk, dan login admin. Baca-saja.
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
const ROOT = fileURLToPath(new URL('..', import.meta.url));
const req = createRequire(`${ROOT}frontend/`);
const { chromium } = req('@playwright/test');
const AxeBuilder = req('@axe-core/playwright').default;
const WEB = process.env.WEB_URL ?? 'http://localhost:5173';
const browser = await chromium.launch();
const prod = (await (await fetch('http://localhost:8080/api/products?q=Gayo')).json()).content[0].id;
let bad = 0;
for (const v of ['base', 'a', 'b', 'c']) for (const path of ['/', `/produk/${prod}`, '/masuk', '/admin/masuk']) {
  const page = await (await browser.newContext({ baseURL: WEB, locale: 'id-ID' })).newPage();
  await page.addInitScript(() => localStorage.setItem('tokoku.devBanner', 'hidden'));
  await page.goto(path); await page.getByRole('heading', { level: 1 }).first().waitFor();
  await page.evaluate((p) => { document.documentElement.dataset.palet = p; }, v);
  const r = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21aa']).analyze();
  console.log(v.padEnd(4), path.padEnd(14), r.violations.length ? 'GAGAL ' + r.violations.map((x) => `${x.id}(${x.nodes.length})`).join(',') : 'ok');
  bad += r.violations.length; await page.context().close();
}
await browser.close(); process.exit(bad ? 1 : 0);
