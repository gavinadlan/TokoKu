import { defineConfig, devices } from '@playwright/test';

// Konfigurasi terpisah: backend kedua (:8081, DB sendiri, APP_ADMIN_FORCE_PASSWORD_CHANGE=true) dan Vite kedua (:5174).
export default defineConfig({
  testDir: './e2e',
  testMatch: /firstlogin\.spec\.ts/,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  workers: 1,
  reporter: [['list']],
  use: { baseURL: 'http://localhost:5174', trace: 'retain-on-failure', locale: 'id-ID', ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } },
  webServer: [
    { command: 'bash ../scripts/e2e-firstlogin-backend.sh', url: 'http://localhost:8081/actuator/health', reuseExistingServer: false, timeout: 240_000 },
    { command: 'VITE_API_URL=http://localhost:8081 npx vite --port 5174 --strictPort', url: 'http://localhost:5174', reuseExistingServer: false, timeout: 60_000 },
  ],
});
