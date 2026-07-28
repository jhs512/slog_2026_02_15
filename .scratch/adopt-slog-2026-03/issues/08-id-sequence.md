# 08 — ID 생성 전략 SEQUENCE 전환

**What to build:** 엔티티 ID 생성이 IDENTITY에서 엔티티별 SEQUENCE로 바뀌어 JDBC 배치 insert가 가능해진다. 생성자에 id 파라미터는 노출하지 않는다(호출부 변경 없음).

**Blocked by:** 06 — 설정 구조 개선 (ddl-auto 프로필 이관 선행)

**Status:** ready-for-agent

- [ ] 전 엔티티가 엔티티별 시퀀스(allocationSize 1) 사용, id 처리는 기반 클래스 유지
- [ ] 엔티티 생성 호출부 시그니처 변화 없음
- [ ] 전체 테스트 통과 (dev/test는 ddl-auto 재생성으로 마이그레이션 불필요)
- [ ] prod 스키마 전환 방법을 티켓 코멘트에 기록 (시퀀스 생성 + 현재 max id로 초기화)
