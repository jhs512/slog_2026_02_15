# 11 — PostComment 정책 분리 + 도메인 정책 단위 테스트

**What to build:** 댓글 권한 로직이 Post와 같은 방식(정책 확장)으로 분리되어 일관성이 생기고, 읽기/수정/삭제 권한 매트릭스가 Spring 없는 순수 단위 테스트로 고정된다.

**Blocked by:** None — can start immediately

**Status:** ready-for-agent

- [ ] PostComment 권한 로직을 클래스 본문에서 정책 확장으로 분리 (이 리포의 확장 함수 방식, 믹스인 아님)
- [ ] Post/PostComment 권한 매트릭스 순수 단위 테스트 (@Nested 구조, Spring 컨텍스트 없음)
- [ ] Post 도메인 규칙(수정 등) 단위 테스트 이식
- [ ] 기존 컨트롤러 통합 테스트 통과 (동작 불변)
