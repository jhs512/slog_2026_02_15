# 10 — memberMixin 인터페이스 + MemberProxy useRealState

**What to build:** Member의 부가 능력(security, profileImgUrl)이 인터페이스 믹스인으로 옮겨져 MemberProxy에서 override 가능해지고, 프록시가 real 엔티티를 로드한 뒤에는 토큰 캐시값과 DB값이 갈리지 않는다. post 도메인은 기존 확장 함수 구조 유지.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] MemberAware/MemberHasSecurity/MemberHasProfileImgUrl 인터페이스 도입, Member가 구현
- [ ] MemberProxy가 profileImgUrl 등을 override
- [ ] useRealState: real 로드 후 nickname 등도 real에서 읽음 — 왜 필요한지 주석 필수
- [ ] 기존 member 확장 프로퍼티 호출부 정리, post 쪽은 미변경
- [ ] 기존 인증/member 테스트 통과
