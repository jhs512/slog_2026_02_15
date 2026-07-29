# 01 — E2E 하네스 부트스트랩

**What to build:** 개발자가 `pnpm e2e` 한 명령으로 — 전용 e2e 프로필 백엔드(빈 slog_e2e DB, Redis 2)와 프론트 dev 서버가 자동으로 뜨고 — 로그인 상태(storageState)가 준비된 채 스모크 테스트 1개(홈에서 글 목록이 보인다)가 통과하는 것까지의 전체 관통 경로.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] Playwright + Chromium 설치, `pnpm e2e`/`pnpm e2e:ui` 스크립트
- [ ] 백엔드 e2e 프로필: DB slog_e2e, Redis database 2, ddl-auto create, NotProd 시드 계정 생성
- [ ] slog_e2e DB 없으면 만들어 주는 준비 스크립트
- [ ] Playwright webServer 2개 자동 기동 + 로컬 재사용 (reuseExistingServer)
- [ ] setup 프로젝트: user1·user2·admin API 로그인 → storageState 저장, 계정 상수 한 파일로
- [ ] 스모크: 비로그인 방문자가 홈/글 목록을 본다 — `pnpm e2e` 전체가 초록
- [ ] LLM 개입 없음 — 결정적 코드만, 고정 sleep 금지
