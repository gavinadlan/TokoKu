import { test } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { fileURLToPath, pathToFileURL } from 'node:url';

/** Screenshot pratinjau email (HTML dari scripts/email-preview.sh) ke docs/design/email/. Jalankan: npm run shots */
const dir = fileURLToPath(new URL('../../docs/design/email/', import.meta.url));
const pages = ['order-created-pickup', 'order-created-delivery', 'paid-pickup', 'paid-delivery', 'ready-for-pickup-pickup', 'cancelled-pickup'];

for (const [label, width, height] of [['desktop', 900, 1100], ['mobile', 375, 900]] as const) {
  test(`pratinjau email ${label}`, async ({ page }) => {
    mkdirSync(dir, { recursive: true });
    await page.setViewportSize({ width, height });
    for (const name of pages) {
      await page.goto(pathToFileURL(`${dir}${name}.html`).href);
      await page.screenshot({ path: `${dir}${name}-${label}.png`, fullPage: true });
    }
  });
}
