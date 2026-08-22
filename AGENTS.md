# 작업지침

- 한국어 사용
- 짧고 간결하게 말해
- PR 사용 안 함, 항상 main 브랜치에 바로 커밋 후 푸시
- 아래 "유용한 명령어"는 계속 업데이트할 것

# 스킬 (mattpocock/skills)

어떤 스킬을 쓸지 모르겠으면 **`/ask-matt`** 에 상황을 설명하면 라우팅해 준다.

## 기본 흐름

```
/grill-with-docs  →  /to-spec  →  /to-tickets  →  /implement
   생각 다듬기        스펙으로       티켓으로        구현
```

| 스킬 | 쓸 때 |
|---|---|
| `/grill-with-docs` | 계획·설계를 캐물어 다듬는다. ADR·용어집도 같이 남긴다 |
| `/grill-me` | 문서 없이 생각만 검증할 때 |
| `/to-spec` | 지금 대화를 스펙으로 만들어 이슈 트래커에 올린다 (인터뷰 없음) |
| `/to-tickets` | 계획·스펙을 의존 관계가 선언된 티켓들로 쪼갠다 |
| `/implement` | 스펙·티켓 기준으로 실제 구현 |

## 상황별

| 스킬 | 쓸 때 |
|---|---|
| `/diagnosing-bugs` | 원인 모를 버그, 성능 회귀 |
| `/improve-codebase-architecture` | 구조 개선 지점을 훑고 HTML 리포트로 받아 고를 때 |
| `/codebase-design` | 모듈 인터페이스·경계를 설계할 때 쓰는 공용 어휘 |
| `/domain-modeling` | 도메인 용어를 정리하거나 ADR 을 남길 때 |
| `/tdd` | 테스트 먼저 쓰고 싶을 때 |
| `/prototype` | 설계 판단을 위해 버리는 프로토타입 |
| `/research` | 1차 출처 기준으로 조사하고 결과를 저장소에 남길 때 |
| `/code-review` | 특정 시점 이후 변경을 표준·스펙 두 축으로 리뷰 |
| `/triage` | 이슈·외부 PR 을 상태 기계로 넘길 때 |
| `/wayfinder` | 한 세션에 안 들어가는 큰 작업을 결정 티켓 지도로 |
| `/handoff` | 대화를 다른 에이전트가 이어받을 문서로 압축 |
| `/to-questionnaire` | 내가 못 정하는 결정을 남에게 물을 질문지로 |
| `/wait-what` | 방금 설명이 이해 안 됐을 때 다시 설명 |
| `/teach` | 개념을 배우고 싶을 때 |

# 유용한 명령어

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

## 앱 (Compose Multiplatform) — `kmp/`

Android / iOS / Web 이 UI 를 공유하고, **글 본문만 각 플랫폼 웹뷰**로 띄운다.
플랫폼 차이는 `platform/Platform.kt` 의 expect 두 개(`DocumentWebView`, `kakaoLogin`)뿐.

- Web 실행: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- Web 배포물: `./gradlew :composeApp:wasmJsBrowserDistribution`
- APK: `./gradlew :composeApp:assembleDebug`
- 테스트: `./gradlew :composeApp:testDebugUnitTest`

### 알아둘 것

- **안드로이드 타깃은 SDK 가 있을 때만 활성화**된다(`ANDROID_HOME` 또는 `local.properties`).
  SDK 없이도 Web 을 빌드할 수 있게 `composeApp/build.gradle.kts` 에서 분기한다.
- **iOS 는 CI 에서 제외**했다. `.ipa` 배포에 유료 Apple Developer 계정이 필요하다.
  소스(`iosMain`)는 남아 있고 Xcode 프로젝트는 아직 없다.
- **한글 폰트를 번들한다.** Compose for Web 은 캔버스에 글자를 그려 시스템 폰트를
  안 쓴다. 없으면 한글이 전부 두부(□)로 나온다. 색·폰트·카드 모양은 front 의
  `globals.css` 토큰을 옮긴 것(`ui/Theme.kt`).
- **설정 캐시는 꺼져 있다.** Kotlin/JS webpack 태스크가 아직 지원하지 않는다.
- 로컬에서 `https://api.slog.gg` 를 직접 부르면 CORS 로 막힌다(허용 origin 이
  `https://www.slog.gg` 뿐). 개발 중에는 프록시나 스텁이 필요하다.

### 에뮬레이터

- AVD 생성은 `avdmanager` 인자의 세미콜론이 Windows 배치에서 쪼개져 실패한다.
  `.bat` 파일로 감싸 실행할 것.
- 기동: `%ANDROID_HOME%\emulator\emulator.exe -avd slog_test -no-snapshot-load`
- 설치·실행: `adb install -r <apk>` → `adb shell am start -n gg.slog.app/.MainActivity`

### 릴리즈 (APK)

`gh release create android-v0.1.x --prerelease` 하면 `android.yml` 이 빌드해서 APK 를 첨부한다.

- CI 는 시크릿 `ANDROID_DEBUG_KEYSTORE_B64` 로 **로컬과 같은 디버그 키**를 복원해 서명한다.
  이게 없으면 키 해시가 달라져 **카카오 로그인이 조용히 실패**한다.
- 릴리즈 서명으로 바꾸면 키 해시가 달라지므로 카카오 콘솔에 새 키 해시를 등록해야 한다.

## 카카오 네이티브 로그인

앱이 카카오 SDK 로 액세스 토큰을 받아 백엔드가 slog 세션으로 교환한다
(`POST /member/api/v1/auth/social/kakao`). 웹의 리다이렉트 로그인과 별개 경로다.

- 백엔드는 토큰이 **우리 앱에서 발급된 것인지** `app_id` 로 검증한다.
  `CUSTOM__KAKAO__APP_ID` 가 없으면 요청을 거부한다(fail-closed).
  이 검증이 없으면 다른 카카오 앱의 토큰으로 남의 계정에 로그인할 수 있다.
- 운영에는 `deploy.yml` 이 시크릿에서 읽어 `docker run -e` 로 주입한다.
  `DOT_ENV` 는 한 덩어리라 덮어쓰면 운영 DB 정보까지 날아가므로 건드리지 않는다.
- 카카오톡이 없는 기기(에뮬레이터)에서는 웹 로그인으로 폴백한다. 백엔드로 가는
  흐름은 동일하므로 검증으로 충분하다.

## DB 접속 (docker)

- PostgreSQL: `docker exec -it db_1 psql -U postgres -d slog_dev`
  - 비밀번호 `lldj123414`, 포트 5432, DB: `slog_dev` / `slog_test` / `slog_e2e`
- Redis: `docker exec -it redis_1 redis-cli -a lldj123414`

# 에이전트 문서

## 이슈 트래커

이슈와 PRD는 `.scratch/<feature>/` 아래 로컬 마크다운 파일로 관리합니다. `docs/agents/issue-tracker.md` 참고.

## Triage 라벨

다섯 가지 표준 triage 역할을 기본 문자열 그대로 사용합니다. `docs/agents/triage-labels.md` 참고.

## 도메인 문서

단일 컨텍스트 레이아웃 (`CONTEXT.md` + `docs/adr/`). `docs/agents/domain.md` 참고.
