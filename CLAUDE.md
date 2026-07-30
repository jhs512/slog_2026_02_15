# 작업지침

- 한국어 사용
- 짧고 간결하게 말해
- PR 사용 안 함, 항상 main 브랜치에 바로 커밋 후 푸시
- 일반적인 흐름 : /grill-with-docs, /to-spec, /to-tickets, /implement
- 필요하다면 /diagnosing-bugs, /improve-codebase-architecture 사용
- 최대한 matt 스킬들을 활용

# 유용한 명령어

- 설명 : 이 부분의 내용은 계속 업데이트

## 로컬 인프라 (PostgreSQL + Redis) — `back/devInfra/`

- 기동: `docker compose up -d`
- 중지: `docker compose down` (볼륨까지 삭제: `docker compose down -v`)
- 상태: `docker ps`
- 컨테이너: `db_1`(PostgreSQL, 5432), `redis_1`(Redis, 6379)

## 백엔드 (Spring Boot · Kotlin) — `back/`

- 실행: `./gradlew bootRun`
- 실행(포트 변경): `./gradlew bootRun --args='--server.port=8081'`
- 빌드: `./gradlew build` (테스트 포함)
- 테스트: `./gradlew test` (slog_test DB 필요 → 인프라 먼저 기동)
- 클린: `./gradlew clean`
- 프로필: 기본 `dev`, 테스트 `test`, 운영 `prod`, e2e `e2e`

## CI 동등 검증 (푸시 전 필수) — `back/`

`./gradlew test` 통과가 CI 통과를 보장하지 않는다. CI는 컨테이너 안에서 DB를 새로 만들고
전체 스위트를 돌리므로, Spring 컨텍스트 구성·시퀀스 초기 상태 등이 로컬과 다르다.
실제로 로컬은 초록인데 CI만 깨지는 일이 반복됐다.

- CI와 같은 방식으로 검증: `docker build -t slog-ci-check .`
  (Dockerfile 안에서 PostgreSQL 기동 → `slog_test` 생성 → `./gradlew build` 수행)
- 캐시 없이(권장): `docker build --no-cache -t slog-ci-check .`
- 로그가 길므로 파일로 받고 요약만 읽을 것: `docker build --no-cache . > /tmp/b.log 2>&1; grep -E "tests completed|BUILD" /tmp/b.log`

## 프론트엔드 (Next.js · pnpm) — `front/`

- 개발 서버: `pnpm dev` (기본 3000)
- 빌드 / 운영 실행: `pnpm build` / `pnpm start`
- 린트: `pnpm lint`
- 포맷: `pnpm format`
- 타입체크: `pnpm tsc`
- 전체 점검: `pnpm check` (format → tsc → lint)
- API 대상 변경: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081 pnpm dev`

## E2E (Playwright) — `front/`

- 실행: `pnpm e2e` (헤드리스) / `pnpm e2e:ui` (UI 모드) / `pnpm exec playwright test e2e/<파일>.spec.ts`
- 백엔드(e2e 프로필 :8091)와 프론트(:3001)를 **자동 기동**하고, 떠 있으면 재사용한다.
  전용 DB `slog_e2e`(매 기동 재생성) + Redis DB 2를 쓰므로 dev 데이터를 건드리지 않는다.
- 인프라(`db_1`, `redis_1`)는 미리 기동돼 있어야 한다.
- `Unable to acquire lock at .next/dev/lock` 오류 시: `rm -f front/.next/dev/lock` 후 재실행
- **CI에는 포함되지 않는다** — 백엔드 변경(특히 컨트롤러·트랜잭션 경계) 후에는 직접 돌릴 것

## DB 접속 (docker)

- PostgreSQL: `docker exec -it db_1 psql -U postgres -d slog_dev`
  - 비밀번호 `lldj123414`, 포트 5432, DB: `slog_dev` / `slog_test` / `slog_e2e`
- Redis: `docker exec -it redis_1 redis-cli -a lldj123414`

# 에이전트 스킬

## 이슈 트래커

이슈와 PRD는 `.scratch/<feature>/` 아래 로컬 마크다운 파일로 관리합니다. `docs/agents/issue-tracker.md` 참고.

## Triage 라벨

다섯 가지 표준 triage 역할을 기본 문자열 그대로 사용합니다. `docs/agents/triage-labels.md` 참고.

## 도메인 문서

단일 컨텍스트 레이아웃 (`CONTEXT.md` + `docs/adr/`). `docs/agents/domain.md` 참고.
