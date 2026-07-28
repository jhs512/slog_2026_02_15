# 01 — 인증 쿠키 SameSite=None 전환 + 인증 정비

**What to build:** 인증 쿠키가 `SameSite=None; Secure`로 발급되어 실시간 연결(withCredentials)에 쿠키가 실리고, 미인증 사용자가 보호 API를 호출하면 500이 아닌 401 JSON을 받는다. (ADR-0001)

**Blocked by:** None — can start immediately

**Status:** done

- [ ] 로그인/토큰 재발급 응답의 Set-Cookie에 `SameSite=None; Secure` 속성이 있다 (MockMvc 검증)
- [ ] 미인증 상태에서 Rq.actor 접근 시 401 코드의 비즈니스 예외 JSON 응답
- [ ] 인증 필터: doFilter 호출이 한 지점으로 모이고 authenticateIfPossible로 재구성 (동작 동일)
- [ ] 필터/SecurityConfig의 에러 응답 직렬화가 Spring 구성 ObjectMapper 빈 사용
- [ ] CustomUserDetailsService가 실제 password를 전달
- [ ] 기존 인증 관련 테스트 전부 통과
