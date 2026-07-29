# Spec: Playwright E2E 테스트 도입

Status: ready-for-agent

## Problem Statement

리포에 브라우저 레벨 테스트가 전혀 없다. 백엔드 통합 테스트(181건)는 API 계약을 지키지만, 프론트–백엔드가 실제로 붙었을 때의 흐름(로그인 쿠키, SSE 토스트, STOMP 반영, 특수 라우트)은 사람이 수동으로 확인해야 하고, 최근처럼 인증·실시간 축을 크게 바꾸면 회귀를 자동으로 잡을 방법이 없다.

## Solution

Playwright(Chromium) E2E 스위트를 `front/e2e/`에 도입한다. `pnpm e2e` 한 방이면 전용 e2e 프로필의 백엔드(빈 DB)와 프론트 dev 서버가 자동 기동되고, 결정적인 브라우저 테스트가 돈다. 실행 과정에 LLM은 개입하지 않는다 — 산출물은 평범한 테스트 코드다.

## User Stories

1. As a 개발자, I want `pnpm e2e` 한 명령으로 전체 E2E가 돌기를, so that 사전 준비 없이 회귀를 확인한다
2. As a 개발자, I want E2E가 전용 DB(slog_e2e)·Redis DB(2)에서 돌기를, so that dev 데이터가 오염되지 않는다
3. As a 개발자, I want 테스트가 매 실행마다 깨끗한 스키마에서 시작하기를, so that 결과가 결정적이다
4. As a 방문자, I want 글 목록에서 글을 클릭해 뷰어가 열리는 흐름이 검증되기를, so that 핵심 경로가 항상 동작한다
5. As a 관리자, I want 로그인 페이지 UI 플로우가 검증되기를, so that 인증 화면 회귀를 잡는다
6. As a 로그인 사용자, I want 글 작성→발행→목록 노출 흐름이 검증되기를, so that 저작 경로가 항상 동작한다
7. As a 로그인 사용자, I want 댓글 작성/삭제가 검증되기를, so that 상호작용 경로가 항상 동작한다
8. As a 방문자, I want 어떤 글이 조회수 10을 돌파하는 순간 열려 있는 다른 브라우저에 알림 토스트가 뜨는 것이 검증되기를, so that SSE 알림 회귀를 잡는다
9. As a 글 작성자, I want 내 글 알림이 나에게는 안 뜨는 것이 검증되기를, so that 본인 제외 필터 회귀를 잡는다
10. As a 독자, I want 글 상세를 보는 중 다른 브라우저에서 수정하면 실시간 반영되는 것이 검증되기를, so that STOMP 경로 회귀를 잡는다
11. As a 독자, I want PPT 슬라이드 렌더링(`/p/{id}/ppt/{pptId}`)과 해시 슬라이드 이동이 검증되기를, so that PPT 회귀를 잡는다
12. As a 외부 도구 사용자, I want raw 아티팩트(`/p/{id}/raw/{rawId}`)가 언어 태그에서 유도된 Content-Type으로 서빙되는 것이 검증되기를, so that Postman import 경로가 항상 동작한다
13. As a 독자, I want 해시 앵커(`#제목`) 진입 시 해당 제목으로 스크롤되고 sticky 헤더에 가려지지 않는 것이 검증되기를, so that 링크 공유가 깨지지 않는다
14. As a 사용자, I want `/ken/{id}` → `/p/{id}` 리다이렉트가 검증되기를, so that 구 URL이 계속 동작한다
15. As a 글 작성자, I want 내 글 목록에서 임시저장/비공개/미노출 배지가 상태별로 정확히 표시되는 것이 검증되기를, so that 3단계 상태 UI 회귀를 잡는다
16. As a 관리자, I want 회원 목록·검색 화면이 검증되기를, so that 관리 화면 회귀를 잡는다

## Implementation Decisions

### 도구·구조
- Playwright + Chromium 단일 프로젝트(Firefox/WebKit은 CI 도입 시 확장). 테스트는 `front/e2e/`, 설정은 front 패키지에 둔다. `pnpm e2e`(헤드리스)·`pnpm e2e:ui`(UI 모드) 스크립트 추가.
- 실행 과정에 LLM 개입 없음. 셀렉터는 role/텍스트 우선, 필요한 곳에만 `data-testid` 최소 추가.

### 실행 환경
- 백엔드에 **e2e 프로필** 신설: dev 기반 + DB `slog_e2e` + Redis database 2 + `ddl-auto: create` — 매 기동마다 빈 스키마, NotProd 시드 계정 생성. 기존 dev/test 프로필과 완전 격리.
- Playwright `webServer` 2개: back(`gradlew bootRun` e2e 프로필, 전용 포트) + front(dev 서버, `NEXT_PUBLIC_API_BASE_URL`을 e2e 백엔드로). 이미 떠 있으면 재사용(`reuseExistingServer`), CI 아닌 로컬 전용.
- Docker 인프라(PostgreSQL/Redis)는 기동돼 있다고 전제하되, `slog_e2e` DB가 없으면 생성하는 준비 스크립트를 포함한다.

### 인증
- setup 프로젝트에서 API 로그인(`POST /member/api/v1/auth/login`, 시드 계정 user1·user2·admin)으로 쿠키를 받아 **storageState** 파일로 저장, 테스트 프로젝트들이 재사용한다.
- 관리자 로그인 UI 플로우는 storageState를 쓰지 않는 별도 테스트 1개로만 커버. OAuth(카카오/네이버)는 범위 외.
- 쿠키가 `SameSite=None; Secure`지만 Chromium은 `http://localhost`를 신뢰 출처로 취급하므로 문제없다. 만약 실제로 쿠키가 저장되지 않으면 그때 해결책(예: baseURL https화)을 검토한다 — 선제 우회는 하지 않는다.

### 데이터
- 계정만 시드에 의존한다. 글/댓글 등 콘텐츠는 각 테스트가 백엔드 API로 직접 생성하고 그것만 검증한다 — 테스트 간 독립, 병렬 안전.
- 조회수 10 돌파는 비로그인 API 호출을 10회 반복해 재현한다(작성자 아닌 요청이므로 증가함).
- 실시간 시나리오는 브라우저 컨텍스트 2개(구독자/행위자)로 검증한다.

### 범위 외 확인
- CI(GitHub Actions)는 이번 범위 아님 — 안정화 후 별도 이슈.

## Testing Decisions

- 이 스펙의 산출물 자체가 테스트다. 심은 하나: **Playwright가 실제 HTTP로 front–back 전체를 관통**한다. 보조 심으로 setup/시딩에 Playwright의 API request context를 쓴다.
- 좋은 테스트 기준: 사용자가 보는 것(렌더된 텍스트, 토스트, URL, 응답 헤더)만 단언하고 내부 구현(스토어 상태, 클래스명)은 단언하지 않는다. 대기는 Playwright auto-wait/`expect` 폴링만 쓰고 고정 sleep 금지.
- 선례: 백엔드 SSE 통합 테스트(PostSseServiceTest)의 시나리오(9/10/11번째 조회)를 브라우저 관점으로 재검증한다.

## Out of Scope

- CI 파이프라인, Firefox/WebKit 매트릭스
- OAuth 로그인 플로우 (모킹 포함)
- 시각적 회귀(스크린샷 diff), 성능 측정
- 모바일 뷰포트 전용 시나리오

## Further Notes

- e2e 프로필의 시드 계정/비밀번호는 NotProdInitData 기준(user1/1234 등). 시드가 바뀌면 setup만 고치면 되도록 계정 상수를 한 파일에 모은다.
- 새글 알림 토스트의 표시 컴포넌트(NewPostNotification)가 전역 레이아웃에 마운트되어 있는지 구현 시 확인 — 안 되어 있으면 그 지점이 첫 번째 발견 버그다.
