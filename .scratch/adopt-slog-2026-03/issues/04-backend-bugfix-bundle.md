# 04 — 백엔드 버그픽스/무위험 개선 묶음

**What to build:** 참조 리포에서 이미 고쳐진 잠재 버그 9건을 이식한다. 외부 동작은 동일하되 엣지 케이스(프록시 동등성, 빈 스캔, 초기화 순서)가 안전해진다.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] BaseEntity equals/hashCode: Hibernate 프록시 동일 취급 + 미영속(id=0) 동등성 거부 + identity hashCode 폴백
- [ ] TaskHandlerConfigurer가 싱글톤 빈만 스캔
- [ ] MemberApiClient 인증 헤더 지연 평가
- [ ] Task 엔티티 어노테이션 `@field:` 타깃 명시
- [ ] PostFacade 조회 메서드 전체 readOnly 트랜잭션, MemberActionLogFacade.save 트랜잭션
- [ ] forEventLog()가 전 필드 나열 대신 copy(마스킹 필드만)
- [ ] 스케줄 잡 nullable 안전 처리, 검색 키워드 trim
- [ ] 전체 테스트 통과
