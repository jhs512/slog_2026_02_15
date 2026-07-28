# Spec: slog_2026_03 개선점 수용 + SSE 도입 + 쿠키 인증 전환

Status: ready-for-agent

## Problem Statement

참조 리포(slog_2026_03)에는 이 리포에 없는 검증된 개선(SSE 알림 인프라, 다수의 버그픽스, 설정 구조 개선)이 쌓여 있다. 특히 현재 리포는:

- 새글 알림이 죽은 기능이다 — 프론트가 `/topic/posts/new`를 구독하지만 백엔드 발행 코드가 없다.
- 실시간 연결(EventSource/SockJS) 인증 수단이 없다. 참조 리포는 원타임토큰으로 우회했지만 토큰 발급 왕복과 Redis 키 관리가 번거롭다.
- 프록시 동등성, 필터 흐름, 트랜잭션 경계 등 곳곳에 참조 리포에서 이미 고쳐진 잠재 버그가 남아 있다.

## Solution

참조 리포의 좋은 점을 선별 수용한다. 실시간 인증은 원타임토큰 대신 **인증 쿠키를 `SameSite=None; Secure`로 전환하고 실시간 연결에 `withCredentials`를 켜는 방식**으로 해결한다 (ADR-0001). SSE는 STOMP를 대체하지 않고 역할을 분담하며 공존한다 (ADR-0002). 새글 알림은 조회수 10 최초 돌파 시 SSE로 발행되어 실제로 동작하는 기능이 된다.

## User Stories

1. As a 방문자(비로그인 포함), I want 사이트를 보는 동안 조회수 10을 최초 돌파한 글의 알림을 실시간으로 받기, so that 지금 뜨고 있는 글을 놓치지 않는다
2. As a 글 작성자, I want 내가 쓴 글의 알림은 나에게 오지 않기, so that 자기 글 알림에 시달리지 않는다
3. As a 글 작성자, I want 내 글을 내가 조회해도 조회수가 오르지 않기, so that 조회수가 실제 독자 수를 반영한다
4. As a 로그인 사용자, I want 별도 토큰 발급 없이 쿠키만으로 SSE/WebSocket 연결이 인증되기, so that 연결이 빠르고 단순하다
5. As a 개발자, I want 원타임토큰 같은 부가 인증 체계 없이 단일 쿠키 인증 축을 유지하기, so that 인증 코드의 복잡도가 늘지 않는다
6. As a 운영자, I want SSE 알림이 다중 인스턴스에서도 모든 접속자에게 도달하기(Redis pub/sub), so that 수평 확장 시 알림이 유실되지 않는다
7. As a 미인증 사용자, I want 보호된 API 호출 시 500이 아닌 401 응답을 받기, so that 클라이언트가 로그인 유도를 올바르게 처리한다
8. As a 글 작성자, I want 임시저장/비공개/미노출 세 상태가 구분되어 표시되기, so that "비공개"라는 말이 두 가지 의미로 혼용되지 않는다
9. As a 독자, I want 제목에 특수문자가 있어도 해시 앵커 스크롤이 정확히 동작하기, so that 링크 공유가 깨지지 않는다
10. As a 독자, I want 앵커 이동 시 sticky 헤더에 제목이 가려지지 않기, so that 이동한 위치를 바로 읽을 수 있다
11. As a 사용자, I want 로그아웃 시 페이지 전체 리로드 없이 상태가 정리되기, so that 화면 깜빡임이 없다
12. As a 프론트 개발자, I want 인증 상태가 확정되기 전에는 실시간 구독이 보류되기, so that 익명/인증 구독이 뒤섞이지 않는다
13. As a 실시간 구독 사용자, I want 최초 연결 시 같은 토픽이 이중 구독되지 않기, so that 메시지가 중복 수신되지 않는다
14. As a 관리자, I want 페이징 파라미터가 범위를 벗어나면 조용히 보정되는 대신 400으로 거절되기, so that 잘못된 호출을 즉시 알 수 있다
15. As a API 소비자, I want 잘못된 경로 변수(음수 id 등)에 400을 받기, so that 오류 원인을 빨리 파악한다
16. As a API 소비자, I want 생성 엔드포인트가 201을 반환하기, so that REST 규약대로 동작한다
17. As a 개발자, I want 로컬 테스트가 dev Redis 세션을 지우지 않기(DB 분리), so that 테스트 후 재로그인할 필요가 없다
18. As a 개발자, I want 환경변수 없이도 dev 프로필이 기동되기(기본값), so that 신규 셋업이 빨라진다
19. As a 운영자, I want prod 최초 기동 시 시스템/관리자 계정이 자동 생성되기, so that 수동 시딩 없이 배포할 수 있다
20. As a 개발자, I want 조회 전용 파사드 메서드에 readOnly 트랜잭션이 걸려 있기, so that 불필요한 flush가 없다
21. As a 개발자, I want 엔티티 동등성이 Hibernate 프록시/미영속 상태에서도 올바르기, so that Set/Map 사용 시 미묘한 버그가 없다
22. As a 개발자, I want 배치 insert가 가능한 ID 전략(SEQUENCE), so that 대량 삽입 성능을 확보한다
23. As a 개발자, I want 새 엔티티에 전문검색을 붙일 때 SQL 함수를 추가 등록하지 않아도 되기(가변 인자 pgroonga 함수), so that 검색 확장이 싸진다
24. As a 개발자, I want DESC 인덱스 같은 JPA로 표현 불가한 DDL을 엔티티에 선언적으로 붙이기(@AfterDDL), so that 스키마와 엔티티 정의가 한곳에 모인다
25. As a 개발자, I want 권한 정책이 Spring 없는 순수 단위 테스트로 고정되기, so that 정책 회귀를 빠르게 잡는다
26. As a 개발자, I want 이벤트 로그용 DTO 마스킹이 `copy()` 한 줄이기, so that 필드 추가 시 마스킹 누락이 없다

## Implementation Decisions

### 인증/쿠키 (ADR-0001)

- 인증 쿠키를 `SameSite=Strict` → `SameSite=None`(Secure 유지)으로 전환. 원타임토큰 체계는 도입하지 않음.
- CSRF 방어는 CORS allowedOrigins + "상태 변경은 항상 JSON 본문(preflight 유발)" 불변식이 담당. simple request로 상태를 변경하는 엔드포인트를 만들지 않는다.
- `/sse/**` 경로에 credentials 허용 CORS 등록. SSE 구독은 공개(permitAll), 로그인 사용자는 쿠키로 식별.
- `Rq.actor` 미인증 시 401 비즈니스 예외(현재 IllegalStateException→500).
- 인증 필터: doFilter 호출을 한 지점으로 모으고 authenticateIfPossible로 재구성. 에러 응답 직렬화는 정적 유틸 대신 Spring 구성 ObjectMapper 빈 주입.
- CustomUserDetailsService는 하드코딩 빈 문자열 대신 실제 password를 전달.

### SSE (ADR-0002)

- 범용 SseService: Redis pub/sub(`sse-multicast`) 멀티캐스트, `GET /sse/{channel}`, 타임아웃 30분, 죽은 emitter 자동 정리, 연결 직후 connect 이벤트. 참조 리포 구현을 이식.
- PostSseService: `posts-new` 채널에 글 요약(id, title, authorId, authorName, profileImgUrl, createdAt) 발행.
- 발행 트리거: 조회수 증가 로직을 컨트롤러에서 PostFacade로 이동. 본인 조회는 조회수 미증가(응답 메시지 구분). 조회수가 10을 최초 돌파하는 순간 posts-new 발행.
- 프론트 sseClient: EventSource 래퍼, `withCredentials: true` 고정. 원타임토큰 파라미터 없음.
- useNewPostNotification: STOMP 구독 → SSE 구독 전환. isPending 게이팅(인증 확정 전 보류), isLogin 변경 시 재구독. **작성자 본인 알림 무시(클라이언트 필터)는 유지** — 참조 리포는 이를 삭제했으나 회귀이므로 따라가지 않음.
- STOMP는 기존 역할(글 상세 실시간 동기화) 그대로 유지. stompClient의 최초 연결 시 이중 구독 버그(pending 처리분이 재구독에 중복 포함)는 수정.

### 설정

- yaml: `dbBaseName`으로 DB명 파생, dev/test Redis database 분리(dev 0/test 1), JWT 시크릿·시스템 API 키에 dev용 기본값, 공통 `ddl-auto` 제거 후 프로필별 이관, dev 전용 yaml 신설(ddl-auto update + unlogged 테이블 dialect).
- CustomConfigProperties를 immutable 생성자 바인딩 + `@ConfigurationPropertiesScan`으로 전환.
- MemberProdInitData: prod 최초 기동 시 회원 0명이면 system/admin 계정 생성. 초기 비밀번호는 **DB 비밀번호 재사용이 아니라 전용 환경변수**로 받는다 (참조 리포와 다른 부분).

### 도메인/JPA

- BaseEntity equals/hashCode: Hibernate.getClass 비교 + 미영속(id=0) 동등성 거부 + identity hashCode 폴백.
- ID 전략 IDENTITY → SEQUENCE(엔티티별 시퀀스, allocationSize 1). 단 생성자에 id 파라미터는 노출하지 않는다(참조 리포와 다른 부분) — 시퀀스 선언만 도입하고 id 처리는 기반 클래스 유지.
- pgroonga 매치 함수를 가변 인자 단일 함수로 통합. 기존 `@PGroongaIndex` 어노테이션 추상화는 유지(참조 리포의 원시 SQL 방식은 불채택).
- `@AfterDDL` 반복 가능 어노테이션 + 기동 직후 실행 러너 도입. created_at/modified_at DESC 인덱스에 사용.
- MemberProxy에 useRealState 도입: real 엔티티 로드 후에는 nickname 등도 real에서 읽음. 로직이 난해하므로 주석 필수.
- memberMixin 인터페이스(MemberAware/MemberHasSecurity/MemberHasProfileImgUrl) 도입 — 확장 프로퍼티는 프록시에서 override 불가하다는 실질 근거. **post 쪽은 기존 확장 함수 구조 유지**(혼재 허용).
- PostComment 권한 로직을 클래스 본문에서 정책 확장으로 분리(post와 일관되게, 참조 리포의 믹스인이 아닌 이 리포의 확장 함수 방식으로).
- Task 엔티티 어노테이션에 `@field:` 타깃 명시(컬럼 정의 미반영 잠재 버그).
- forEventLog()는 전 필드 나열 대신 `copy(마스킹 필드만)`.
- PostFacade 조회 메서드 전체에 readOnly 트랜잭션, MemberActionLogFacade.save에 트랜잭션.
- TaskHandlerConfigurer는 싱글톤 빈만 스캔. 스케줄 잡의 nullable 처리. MemberApiClient 인증 헤더 지연 평가. 검색 키워드 trim.

### 컨트롤러/검증

- `@Validated` + 경로 변수 `@Positive`, 생성 엔드포인트 `@ResponseStatus(CREATED)`, adm 페이징 `@Min/@Max`(클램프 대신 400).
- Swagger 어노테이션(@Tag/@Operation/@SecurityRequirement)은 전부 유지(참조 리포는 없음 — 따라가지 않음).
- 조건부 조회 304 응답 유지.

### 프론트

- 글 상태 3단계 모델: 임시저장(!published && !title) / 비공개(!published) / 미노출(published && !listed). 라벨·아이콘·툴팁·수정 페이지 검증 문구 재작성.
- 해시 앵커 스크롤: `getElementById(id+"강")` 하드코딩 폴백 → `CSS.escape` + prefix 셀렉터. 단 `scroll-padding-top` CSS와 최초 1회 실행 가드는 유지(참조 리포의 회귀는 불채택).
- logout 시그니처를 콜백 없는 `logout()`으로 단순화하되, 내부는 full reload가 아니라 기존 clearLoginMember + router 방식 유지.

## Testing Decisions

- 외부 행동만 검증한다: HTTP 응답(상태 코드, Set-Cookie 속성, 본문), SSE로 실제 수신된 이벤트, 정책 함수의 반환값. 내부 구현(Redis 키, emitter 목록 등)은 검증하지 않는다.
- 심 3개:
  1. **기존 MockMvc 컨트롤러 통합 테스트** — 쿠키 `SameSite=None; Secure` 속성은 로그인 응답 Set-Cookie로 검증. incrementHit의 본인 제외/메시지 분기, 401 응답 전환, Bean Validation 400도 이 심에서. 선례: 기존 ApiV1AuthControllerTest, ApiV1PostControllerTest.
  2. **신규 SSE 실연결 통합 테스트** — 실제 HTTP 클라이언트로 `/sse/{channel}` 구독 후 발행 이벤트 수신을 블로킹 큐로 대기. 조회수 10 돌파 시 posts-new 수신을 검증. 선례: 참조 리포 PostSseServiceTest.
  3. **신규 순수 도메인 단위 테스트** — Spring 컨텍스트 없이 읽기/수정/삭제 권한 매트릭스 검증. 선례: 참조 리포 PostPolicyTest/PostCommentPolicyTest(@Nested 구조).
- 프론트는 기존대로 `pnpm check`(format/tsc/lint)만.
- 기존 테스트 스위트(참조 리포 대비 1.8배 규모)는 전부 통과 유지가 전제.

## Out of Scope

- 소프트 삭제 전환(deletedAt + @SQLRestriction) — 별건 백로그
- EntityAttr 값 컬럼 분리(intValue/strValue) — 별건 백로그
- 테스트 베이스 클래스 + Kotlin MockMvc DSL 전환 — 별건 백로그
- post 도메인의 믹스인 전환, 엔티티 생성자 id 노출, companion 리포지토리 홀더 — 불채택
- full-reload 로그아웃, Swagger 제거, scroll-padding 제거 — 참조 리포의 회귀로 판단, 불채택
- STOMP 인증 도입 — 현재 무인증 구독으로 충분, 필요 시 쿠키로 해결 가능해짐

## Further Notes

- 참조 리포: https://github.com/jhs512/slog_2026_03 (탐색용 클론은 세션 스크래치패드에 있음, 구현 시 새로 클론 필요할 수 있음)
- 관련 ADR: docs/adr/0001(쿠키 SameSite=None, 원타임토큰 불채택), docs/adr/0002(SSE/STOMP 역할 분담)
- SameSite=None 전환 후 "상태 변경은 항상 JSON 본문" 불변식이 CSRF 방어의 전부다. 리뷰 시 form-encoded POST나 상태 변경 GET이 생기지 않는지 반드시 확인할 것.
