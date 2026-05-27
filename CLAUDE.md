# 작업지침
- 한국어 사용
- /caveman 스킬 사용
- 일반적인 작업 흐름 : /grill-with-docs, /to-prd, /to-issues, /tdd and /diagnosis, /improve-codebase-architecture(이 스킬은 필요할 때만 사용)
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
- 프로필: 기본 `dev`, 테스트 `test`, 운영 `prod`

## 프론트엔드 (Next.js · pnpm) — `front/`
- 개발 서버: `pnpm dev` (기본 3000)
- 빌드 / 운영 실행: `pnpm build` / `pnpm start`
- 린트: `pnpm lint`
- 포맷: `pnpm format`
- 타입체크: `pnpm tsc`
- 전체 점검: `pnpm check` (format → tsc → lint)
- API 대상 변경: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8081 pnpm dev`

## DB 접속 (docker)
- PostgreSQL: `docker exec -it db_1 psql -U postgres -d slog_dev`
  - 비밀번호 `lldj123414`, 포트 5432, DB: `slog_dev` / `slog_test`
- Redis: `docker exec -it redis_1 redis-cli -a lldj123414`

# 에이전트 스킬

## 이슈 트래커

이슈와 PRD는 `.scratch/<feature>/` 아래 로컬 마크다운 파일로 관리합니다. `docs/agents/issue-tracker.md` 참고.

## Triage 라벨

다섯 가지 표준 triage 역할을 기본 문자열 그대로 사용합니다. `docs/agents/triage-labels.md` 참고.

## 도메인 문서

단일 컨텍스트 레이아웃 (`CONTEXT.md` + `docs/adr/`). `docs/agents/domain.md` 참고.
