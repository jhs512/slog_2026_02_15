# 중복 지식 위치 가이드

같은 개념이 여러 곳에 흩어져 있는 것들. 하나를 바꾸면 나머지도 함께 바꿔야 한다.

| 관심사 | 백 | 프론트 | 인프라/CI |
|---|---|---|---|
| **API 타입** | `back/.../dto/`, Controller 응답 타입 | `front/src/global/backend/apiV1/schema.d.ts` (자동 생성) | — |
| **인증 방식** | `global/security/config/CustomAuthenticationFilter.kt` | `front/src/global/auth/`, `front/src/global/backend/client.ts` | — |
| **환경변수** | `back/src/main/resources/application.yaml`, `global/app/config/CustomConfigProperties.kt` | `front/.env.*` | `infra/variables.tf`, `infra/secrets.tf`, `.github/workflows/deploy.yml` |
| **Docker 이미지** | `back/Dockerfile` | `front/Dockerfile` | `infra/main.tf`, `.github/workflows/deploy.yml` |
| **STOMP 채널** | `global/websocket/`, `post/app/PostStompService.kt` | `front/src/global/websocket/stompClient.ts` | — |

---

# 백엔드 코드 철학

## 패키지 구조

헥사고날 아키텍처 기반. 각 도메인/모듈은 아래 레이어로 구성된다.

```
boundedContexts/
  {domain}/
    in/           ← 인바운드 (외부에서 안으로)
    app/          ← 애플리케이션 로직
    domain/       ← 순수 도메인 모델
    out/          ← 아웃바운드 (안에서 외부로)
    config/       ← 빈 설정
    annotation/   ← 이 모듈 전용 어노테이션

global/
  {concern}/
    in/
    app/
    domain/
    out/
    config/
    annotation/   ← 이 관심사 전용 어노테이션
```

`annotation/` 패키지는 모든 곳에 생기는 게 아니라, 해당 모듈/관심사가 자체 어노테이션을 정의할 때만 생긴다.

현재 존재하는 것:
- `global/pGroonga/annotation/` — `@PGroongaIndex`
- `global/pgPubSub/annotation/` — `@PgSubscribe`
- `global/task/annotation/` — `@Task`, `@TaskHandler`

어노테이션이 `domain/`이나 `dto/`에 섞이면 안 된다. 어노테이션은 메타데이터이지 도메인 모델이나 DTO가 아니다.

---

## 클래스 이름 = 역할의 표준 어휘

**이름만 봐도 어느 패키지에 있는지, 어떤 어노테이션인지 알 수 있어야 한다.**

| 접미사 | 패키지 | 어노테이션 | 역할 |
|--------|--------|-----------|------|
| `Controller` | `in/` | `@RestController` | HTTP 인바운드 |
| `ScheduledJob` | `in/` | `@Component` + `@Scheduled` | 타이머 인바운드 |
| `Listener` | `in/` | `@Component` + `@EventListener` / `@TransactionalEventListener` | 이벤트 인바운드 |
| `InitData` | `in/` | `@Configuration` + `@Bean(ApplicationRunner)` | 앱 시작 시 초기 데이터 생성 |
| `Facade` | `app/` | `@Service` | 유즈케이스 조율 (여러 서비스 오케스트레이션) |
| `Service` | `app/` | `@Service` | 단일 기술 도메인 서비스 |
| `Manager` | `app/` | `@Component` | 인프라 자원 생명주기 관리 (커넥션, 스레드 등) |
| `Registry` | `app/` | `@Component` | 타입 → 핸들러 조회 테이블 |
| `Repository` | `out/` | extends `JpaRepository` | DB 접근 |
| `Client` | `out/` | `@Service` | 외부 API 접근 |
| `Config` | `config/` | `@Configuration` + **`@Bean` 반드시 있음** | 빈을 소유하고 선언 |
| `Configurer` | `config/` | `@Component` | 빈 생성에 기여 (런타임 서비스 제공 안 함) |
| `Properties` | `config/` | `@ConfigurationProperties` | 설정값 바인딩 |

---

## Config vs Configurer — 핵심 구분

```
Config   = @Bean 메서드가 반드시 있다. 빈의 소유자.
Configurer = @Bean 없다. 다른 컴포넌트의 초기화에 기여할 뿐.
```

두 가지 Configurer 패턴:

**① Config에 주입되어 DSL 기여**
```
SecurityConfig (@Configuration, @Bean)
  ← MemberSecurityConfigurer (@Component)   configure(AuthorizeHttpRequestsDsl) 호출
  ← AuthSecurityConfigurer   (@Component)
  ← PostSecurityConfigurer   (@Component)

WebSocketSecurityConfig (@Configuration, @Bean)
  ← PostWebSocketSecurityConfigurer (@Component)   configure(MessageMatcherBuilder) 호출
```

**② 시작 시 이벤트로 다른 컴포넌트를 초기화**
```
TaskHandlerRegistry (app/, @Component)   ← 런타임 조회 서비스
  ← TaskHandlerConfigurer (config/, @Component)   ContextRefreshedEvent → register() 호출
```

---

## 레이어별 판단 기준

**`in/`에 넣는 기준:** 외부 트리거가 있다.
- HTTP 요청 (`@RestController`)
- 시간 트리거 (`@Scheduled`)
- 이벤트 트리거 (`@EventListener`)
- 앱 시작 트리거 (`ApplicationRunner` — InitData)

**`app/`에 넣는 기준:** 런타임에 다른 클래스가 주입받아 직접 호출한다.
- Facade, Service, Manager, Registry 모두 외부에서 의존성으로 쓰임

**`config/`에 넣는 기준:** 빈을 선언하거나, 빈 생성 과정에 기여한다.
- `Config`: `@Bean` 메서드 있음
- `Configurer`: `@Bean` 없지만 다른 Config/컴포넌트 초기화를 돕는 역할
- `Properties`: `@ConfigurationProperties`

**`annotation/`에 넣는 기준:** 이 모듈이 리플렉션으로 스캔하거나 AOP로 처리하는 마커 어노테이션.
- 어노테이션을 처리하는 인프라와 같은 모듈 안에 위치시킨다.
- DTO나 도메인 모델 파일에 같이 두지 않는다.

**`config/`에 넣으면 안 되는 것:** `@Aspect`, `@ControllerAdvice` 등 횡단 관심사 — 이들은 Config 패턴이 아니다.

---

## 자주 헷갈리는 경우

### Manager vs Service
- `Service`: 단일 도메인 내 기술 서비스. 상태 없거나 단순.
- `Manager`: 인프라 자원의 생명주기 전체를 책임짐. 커넥션 유지, 스레드 루프, 재연결 등.

### Facade vs Service
- `Facade`: 여러 Service/Repository를 조율. 유즈케이스 진입점.
- `Service`: 단일 관심사만 처리.

### Config의 `@Bean` 규칙
`@Configuration`이 붙었어도 `@Bean` 메서드가 없으면 `Config`가 아니다.
→ `@Component`로 바꾸고 역할에 맞는 접미사를 붙일 것.

---

# 프론트엔드 코드 철학

## 패키지 구조

Next.js App Router 기반.

```
src/
  app/                  ← Next.js 라우트 (파일시스템 = URL)
    {route}/
      page.tsx          ← 서버 컴포넌트 진입점
      layout.tsx
      _components/      ← 이 라우트 전용 컴포넌트 (언더스코어 = 라우트 아님)
      _hooks/           ← 이 라우트 전용 훅

  components/           ← 여러 라우트에서 재사용되는 공통 컴포넌트
    ui/                 ← shadcn/ui 기반 원자 컴포넌트 (직접 수정 금지)

  domain/               ← 도메인별 타입/비즈니스 로직
    {domain}/

  global/               ← 앱 전역 인프라
    auth/               ← 인증 HOC, hooks
    backend/            ← 백엔드 API 클라이언트
      apiV1/
        schema.d.ts     ← 백엔드 OpenAPI 스펙에서 자동 생성 (직접 수정 금지)
    websocket/          ← STOMP 클라이언트

  lib/                  ← 순수 유틸리티 (프레임워크 독립)
    business/           ← 비즈니스 유틸
    utils.ts
```

## 핵심 규칙

**`_components/`, `_hooks/`** — 언더스코어 prefix는 Next.js가 라우트로 인식하지 않는 관례. 해당 페이지에서만 쓰는 것은 반드시 여기에.

**`schema.d.ts`** — 백엔드 DTO 변경 시 자동 재생성. 손으로 수정하지 않는다.

**`components/ui/`** — shadcn/ui가 생성한 파일. 직접 수정하지 않고 래핑해서 쓴다.
