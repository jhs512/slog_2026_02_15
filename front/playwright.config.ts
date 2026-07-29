import { defineConfig, devices } from "@playwright/test";

// E2E 스택: back(e2e 프로필, :8091, slog_e2e DB) + front(dev, :3001)
// 로컬 전용 — 이미 떠 있으면 재사용한다. LLM 개입 없는 결정적 테스트만 둔다.
const FRONT_URL = "http://localhost:3001";
const BACK_URL = "http://localhost:8091";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: FRONT_URL,
    trace: "on-first-retry",
  },
  projects: [
    { name: "setup", testMatch: /.*\.setup\.ts/ },
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
      dependencies: ["setup"],
    },
  ],
  webServer: [
    {
      command:
        "node e2e/scripts/prepare-db.mjs && ..\\back\\gradlew.bat -p ..\\back bootRun --args=--spring.profiles.active=e2e",
      url: BACK_URL,
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: "pnpm dev --port 3001",
      url: FRONT_URL,
      reuseExistingServer: true,
      timeout: 120_000,
      env: { NEXT_PUBLIC_API_BASE_URL: BACK_URL },
    },
  ],
});
