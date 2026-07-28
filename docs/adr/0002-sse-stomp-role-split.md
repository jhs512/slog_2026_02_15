# ADR-0002: 실시간 채널 역할 분담 — SSE(단방향 브로드캐스트) / STOMP(양방향·토픽)

날짜: 2026-07-28
상태: 승인됨

## 맥락

실시간 기능은 STOMP/SockJS(`/ws`) 단일 축이었고, 새글 알림은 프론트가 `/topic/posts/new`를 구독만 하고 백엔드 발행 코드가 없는 죽은 기능이었다. 참조 리포(slog_2026_03)는 Redis pub/sub 기반 범용 SSE 멀티캐스트(`SseService`, `/sse/{channel}`)를 도입해 새글 알림만 SSE로 옮기고 STOMP는 유지했다.

## 결정

- 범용 `SseService`(Redis pub/sub `sse-multicast` 채널, 다중 인스턴스 대응)를 도입한다.
- 역할 분담: **단방향 전역 브로드캐스트는 SSE**, **글 상세 실시간 동기화 등 토픽성/양방향 트래픽은 STOMP**. 두 프로토콜은 공존한다.
- `/sse/**`는 공개 구독(permitAll). 로그인 사용자는 쿠키(withCredentials)로 식별한다 ([ADR-0001](0001-cookie-auth-samesite-none-no-onetime-token.md)).
- 새글 알림(`posts-new` 채널)의 발행 트리거는 글 작성 시점이 아니라 **조회수 10 최초 돌파 시**(본인 조회 제외)로 한다 — 검증된 글만 알림을 보내 스팸성을 막는다.

## 결과

- 프로토콜이 2개가 되지만 기존 STOMP 코드는 손대지 않는다.
- 향후 개인화 채널이 필요해지면 채널별 인가를 그때 추가한다.
