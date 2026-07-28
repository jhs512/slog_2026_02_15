# 13 — 프론트 소규모 개선 묶음

**What to build:** 특수문자 제목에서도 해시 앵커 스크롤이 동작하고, 로그아웃 호출부가 콜백 없이 단순해지며, 실시간 구독의 최초 연결 시 메시지 중복 수신이 사라진다.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] 해시 스크롤: 하드코딩 접미사 폴백 → CSS.escape + prefix 셀렉터. scroll-padding-top과 최초 1회 가드는 유지
- [ ] logout() 시그니처 단순화 (호출부 콜백 제거), 내부는 기존 SPA 방식(clearLoginMember + router) 유지
- [ ] stompClient 최초 연결 시 pending 구독이 재구독과 중복되지 않음
- [ ] pnpm check 통과
