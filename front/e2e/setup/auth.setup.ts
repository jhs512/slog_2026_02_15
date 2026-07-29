import { expect, request, test as setup } from "@playwright/test";

import { ACCOUNTS, API_BASE, AccountKey, storageStatePath } from "../accounts";

// API 로그인으로 쿠키를 받아 storageState로 저장 — 테스트들이 재사용한다
for (const key of Object.keys(ACCOUNTS) as AccountKey[]) {
  setup(`login ${key}`, async () => {
    const account = ACCOUNTS[key];
    const api = await request.newContext({ baseURL: API_BASE });

    const res = await api.post("/member/api/v1/auth/login", {
      data: { username: account.username, password: account.password },
    });
    expect(res.ok(), `${key} 로그인 실패: ${res.status()}`).toBeTruthy();

    await api.storageState({ path: storageStatePath(key) });
    await api.dispose();
  });
}
