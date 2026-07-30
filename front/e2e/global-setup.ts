import { request } from "@playwright/test";

import { API_BASE } from "./accounts";

/**
 * 매 실행 전 e2e DB의 콘텐츠를 비운다.
 *
 * webServer가 `reuseExistingServer`로 서버를 재사용하므로 ddl-auto: create만으로는
 * DB가 초기화되지 않는다. 콘텐츠가 누적되면 목록 페이지네이션에 밀려 테스트가 조용히 깨진다.
 * 초기화 대상은 콘텐츠뿐이고 시드 계정은 유지되므로 auth.setup은 그대로 동작한다.
 */
export default async function globalSetup() {
  const api = await request.newContext({ baseURL: API_BASE });

  const res = await api.post("/e2e/reset");
  if (!res.ok()) {
    throw new Error(
      `E2E 초기화 실패 (${res.status()}). 백엔드가 e2e 프로필로 떴는지 확인하세요: ${await res.text()}`,
    );
  }

  await api.dispose();
  console.info("[e2e] 콘텐츠 초기화 완료");
}
