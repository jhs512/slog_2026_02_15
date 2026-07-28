# 02 — SSE 인프라

**What to build:** 임의 채널에 대해 `GET /sse/{channel}`로 구독하고 서버 어디서든 `send(channel, data)`로 발행하면 모든 인스턴스의 구독자에게 이벤트가 도달한다(Redis pub/sub 멀티캐스트). 프론트에는 withCredentials가 켜진 EventSource 래퍼(sseClient)가 생긴다. (ADR-0002)

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] SSE 구독 엔드포인트: 타임아웃 30분, 연결 직후 connect 이벤트, 죽은 emitter 자동 정리
- [ ] 발행이 Redis pub/sub을 경유해 다중 인스턴스에서도 브로드캐스트됨
- [ ] `/sse/**` permitAll + credentials 허용 CORS 등록
- [ ] 프론트 sseClient: EventSource 래퍼, `withCredentials: true`, 원타임토큰 없음
- [ ] 실연결 통합 테스트: 실제 HTTP 클라이언트로 구독 → 발행 이벤트 수신을 블로킹 대기로 검증
