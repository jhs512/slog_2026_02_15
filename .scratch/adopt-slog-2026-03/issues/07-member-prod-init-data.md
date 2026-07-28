# 07 — MemberProdInitData

**What to build:** prod 최초 기동 시(회원 0명) system/admin 계정이 자동 생성되어 수동 시딩 없이 배포할 수 있다. 초기 비밀번호는 DB 비밀번호 재사용이 아니라 전용 환경변수로 받는다.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] prod 프로필에서 회원 0명일 때만 system/admin 생성, 이후 기동에서는 no-op
- [ ] 초기 비밀번호는 전용 환경변수 사용 (미설정 시 기동 실패 등 안전한 실패)
- [ ] 기존 NotProd 초기 데이터와 충돌 없음
