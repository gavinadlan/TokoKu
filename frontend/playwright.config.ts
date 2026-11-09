import { defineConfig, devices } from '@playwright/test';

const FRONTEND = 'http://localhost:5173';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1, // satu backend dan satu database bersama
  reporter: [['list']],
  use: { baseURL: FRONTEND, trace: 'retain-on-failure', locale: 'id-ID', viewport: { width: 1280, height: 800 } },
  projects: [
    { name: 'e2e', testMatch: /(shop|admin|a11y)\.spec\.ts/, use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 800 } } },
    { name: 'screens', testMatch: /screens\.spec\.ts/, use: { ...devices['Desktop Chrome'] } },
  ],
  // Backend (profil dev, provider fake, data seed) dan frontend dijalankan otomatis; yang sudah hidup dipakai ulang.
  webServer: [
    {
      command: 'bash ../scripts/dev-backend.sh',
      url: 'http://localhost:8080/actuator/health',
      reuseExistingServer: true,
      timeout: 240_000,
    },
    { command: 'npm run dev', url: FRONTEND, reuseExistingServer: true, timeout: 60_000 },
  ],
});
