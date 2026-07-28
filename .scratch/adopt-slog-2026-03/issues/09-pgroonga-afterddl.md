# 09 — pgroonga 가변 인자화 + AfterDDL

**What to build:** 새 엔티티에 전문검색을 붙일 때 SQL 함수를 추가 등록할 필요가 없어지고(단일 가변 인자 매치 함수), JPA로 표현 불가한 DDL(DESC 인덱스)을 엔티티에 선언적으로 붙일 수 있다.

**Blocked by:** None — can start immediately

**Status:** done

- [ ] 엔티티별 pgroonga 매치 함수들을 단일 가변 인자 함수로 통합, 기존 검색 쿼리 동작 동일
- [ ] 기존 @PGroongaIndex 어노테이션 추상화는 유지
- [ ] @AfterDDL 반복 가능 어노테이션 + 기동 직후 실행 러너 도입
- [ ] created_at/modified_at DESC 인덱스를 @AfterDDL로 선언
- [ ] 검색 관련 기존 테스트 통과
