# ADR-0001: 쿠키 인증 SameSite=None + CORS 기반 CSRF 방어 (원타임토큰 불채택)

날짜: 2026-07-28
상태: 승인됨

## 맥락

EventSource(SSE)와 SockJS는 커스텀 헤더를 붙일 수 없어, 크로스오리진 실시간 연결의 인증이 문제였다. 참조 리포(slog_2026_03)는 Redis TTL 30초짜리 원타임토큰을 쿼리 파라미터로 붙이는 방식을 썼다.

프로덕션 토폴로지는 `www.slog.gg`(front) ↔ `api.slog.gg`(back)로 same-site이며, 이 서비스에는 전통적 form POST가 전혀 없고 모든 상태 변경이 JSON 본문 fetch(CORS preflight 유발)로만 이루어진다.

## 결정

- 인증 쿠키(accessToken/apiKey)를 `SameSite=None; Secure; Partitioned`로 설정한다.
  (Partitioned(CHIPS)는 2026-08-03 채택 — 브라우저의 3자 쿠키 차단 대비. front↔back은 same-site라
  파티션 키가 동일해 동작 변화 없음. 같은 날 잠시 뺐다가 재적용했으므로, 이후 속성을 바꿀 때는
  Partitioned 유/무가 별개 쿠키로 취급된다는 점(이관 만료 처리 필요)을 유의할 것.)
- 쿠키 Domain은 `api.slog.gg`(백엔드 호스트 한정)로 둔다. (2026-08-03 `slog.gg`에서 변경 —
  인증 쿠키는 HttpOnly라 프론트가 읽을 수 없고 백엔드만 소비하므로 범위를 최소화.
  옛 `slog.gg` 쿠키를 만료시키는 이관 코드는 잠시 운영 후 같은 날 제거했다.
  옛 쿠키가 남은 브라우저는 그 쿠키가 만료될 때까지 로그아웃이 완전하지 않을 수 있음을 감수한 결정.)
- 실시간 연결(EventSource, SockJS)은 `withCredentials: true`로 쿠키를 실어 보낸다.
  (2026-08-03: SockJS 및 xhr fallback 제거 — 네이티브 WebSocket만 사용. 핸드셰이크에 쿠키가 자동으로 실리므로 인증 방식은 동일.)
- 원타임토큰 체계는 도입하지 않는다.
- CSRF 방어는 CORS `allowedOrigins`(front URL 고정) + "상태 변경은 항상 JSON 본문"이라는 불변식이 담당한다.

## 결과

- SSE/WebSocket 인증에 별도 토큰 발급 왕복과 Redis 키가 필요 없다.
- **불변식 유지 의무**: 상태를 변경하는 엔드포인트를 simple request(form-encoded POST, 인증 GET)로 만들면 CSRF에 노출된다. 이 전제가 깨지는 순간 이 ADR을 재검토해야 한다.
