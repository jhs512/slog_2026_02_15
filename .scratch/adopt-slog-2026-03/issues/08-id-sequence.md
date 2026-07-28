# 08 — ID 생성 전략 SEQUENCE 전환

**What to build:** 엔티티 ID 생성이 IDENTITY에서 엔티티별 SEQUENCE로 바뀌어 JDBC 배치 insert가 가능해진다. 생성자에 id 파라미터는 노출하지 않는다(호출부 변경 없음).

**Blocked by:** 06 — 설정 구조 개선 (ddl-auto 프로필 이관 선행)

**Status:** done

- [ ] 전 엔티티가 엔티티별 시퀀스(allocationSize 1) 사용, id 처리는 기반 클래스 유지
- [ ] 엔티티 생성 호출부 시그니처 변화 없음
- [ ] 전체 테스트 통과 (dev/test는 ddl-auto 재생성으로 마이그레이션 불필요)
- [ ] prod 스키마 전환 방법을 티켓 코멘트에 기록 (시퀀스 생성 + 현재 max id로 초기화)

## Comments

**구현 방식**: BaseEntity의 `@GeneratedValue`를 `SEQUENCE` + 고정 제너레이터 이름(`entity_seq_gen`)으로 변경하고,
각 엔티티 클래스 레벨에 `@SequenceGenerator(name = "entity_seq_gen", sequenceName = "<엔티티>_seq", allocationSize = 1)`를 선언.
JPA 3.2(Hibernate 7, Boot 4)부터 제너레이터 이름이 정의 클래스 로컬 스코프라 엔티티별 시퀀스가 정확히 잡힌다.
생성자 시그니처 변경 없음. slog_test DB에서 8개 시퀀스(member_seq, member_attr_seq, member_action_log_seq,
post_seq, post_attr_seq, post_comment_seq, post_like_seq, task_seq)가 각자 독립 증가하는 것 확인.

**prod 전환 SQL** (배포 전 1회 실행 — ddl-auto: update는 기존 테이블의 시퀀스를 만들어주지 않으므로 수동 필요):

```sql
-- 1) 엔티티별 시퀀스 생성 + 현재 max(id) 다음 값으로 초기화
CREATE SEQUENCE IF NOT EXISTS member_seq;
SELECT setval('member_seq', COALESCE((SELECT MAX(id) FROM member), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS member_attr_seq;
SELECT setval('member_attr_seq', COALESCE((SELECT MAX(id) FROM member_attr), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS member_action_log_seq;
SELECT setval('member_action_log_seq', COALESCE((SELECT MAX(id) FROM member_action_log), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS post_seq;
SELECT setval('post_seq', COALESCE((SELECT MAX(id) FROM post), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS post_attr_seq;
SELECT setval('post_attr_seq', COALESCE((SELECT MAX(id) FROM post_attr), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS post_comment_seq;
SELECT setval('post_comment_seq', COALESCE((SELECT MAX(id) FROM post_comment), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS post_like_seq;
SELECT setval('post_like_seq', COALESCE((SELECT MAX(id) FROM post_like), 0) + 1, false);

CREATE SEQUENCE IF NOT EXISTS task_seq;
SELECT setval('task_seq', COALESCE((SELECT MAX(id) FROM task), 0) + 1, false);

-- 2) (선택) IDENTITY 잔재 제거 — 기존 컬럼이 identity/serial이었다면 default 해제
ALTER TABLE member ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE member_attr ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE member_action_log ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE post ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE post_attr ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE post_comment ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE post_like ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE task ALTER COLUMN id DROP IDENTITY IF EXISTS;
```
