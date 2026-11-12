import { defineConfig, devices } from '@playwright/test';

// Konfigurasi terpisah untuk admin di ponsel: backend ketiga (:18082, DB sendiri tokoku_mobile) dan Vite ketiga (:5175).
// Tidak menyentuh DB dev/demo. Jalankan: npm run e2e:mobile
process.env.E2E_API = 'http://localhost:18082';
process.env.E2E_ADMIN_EMAIL = 'ponsel@tokoku.local';
process.env.E2E_ADMIN_PASSWORD = 'ponsel-uji-123';

// E2E_REGRESI=1: jalankan suite desktop (shop, admin, fulfillment, a11y, ...) di backend/DB terpisah ini, bukan di DB dev.
const regresi = Boolean(process.env.E2E_REGRESI);

export default defineConfig({
  testDir: './e2e',
  testMatch: regresi ? /(shop|admin|a11y|realm|fulfillment|photos|gallery|live)\.spec\.ts/ : /admin-mobile\.spec\.ts/,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  workers: 1,
  reporter: [['list']],
  use: { baseURL: 'http://localhost:5175', trace: 'retain-on-failure', locale: 'id-ID', ...(regresi ? { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } : { ...devices['Pixel 7'], viewport: { width: 390, height: 844 } }) },
  webServer: [
    { command: 'bash ../scripts/e2e-mobile-backend.sh', url: 'http://localhost:18082/actuator/health', reuseExistingServer: false, timeout: 240_000 },
    { command: 'VITE_API_URL=http://localhost:18082 npx vite --port 5175 --strictPort', url: 'http://localhost:5175', reuseExistingServer: false, timeout: 60_000 },
  ],
});
