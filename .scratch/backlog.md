# 백로그

티켓화되지 않은, 근본 해결이 필요한 항목들.

## 1. post 도메인의 전역 레포지토리 홀더 제거 (우선순위 높음)

`postExtensions/PostRepositories.kt`의 톱레벨 `lateinit var` 3개(`postAttrRepository`,
`postCommentRepository`, `postLikeRepository`)와 `PostAppConfig`의 "첫 컨텍스트 승리" 가드.

**문제**: 테스트가 여러 Spring 컨텍스트를 띄우면 일부 테스트가 다른 컨텍스트의 EntityManager에
붙은 레포지토리를 사용한다. 증상은 `Detached entity passed to persist`, 또는 저장한 엔티티가
현재 세션에 보이지 않아 FK 제약 위반. **테스트를 추가할 때마다 무관해 보이는 테스트가 깨진다.**

**이력**: 2026-07-29 `PostHitExtensions.incrementHitCount()`만 리포지토리를 파라미터로 받도록
고쳤다(당시 PostSseServiceTest 실패 대응). 2026-07-30 이벤트 직렬화 테스트를 추가하자
좋아요/댓글 경로에서 같은 증상이 재발해, 테스트를 순수 단위 테스트로 바꿔 우회했다.

**해결 방향**: 남은 확장 함수들도 리포지토리를 파라미터로 받게 하고(`PostHitExtensions` 선례),
`PostRepositories.kt`와 `PostAppConfig`를 삭제한다. 호출부는 대부분 `PostFacade` 한 곳이다.

## 2. PROCESSING 상태로 영구히 멈추는 task 회수 로직 부재

`markAsProcessing()` 후 프로세스가 죽거나 예외가 삼켜지면 그 task를 다시 집는 주체가 없다.
prod에서 task 4581이 2026-05-20부터 두 달 넘게 `PROCESSING`으로 방치됐다(2026-07-30 수동 재큐잉).

**해결 방향**: `PROCESSING` 상태가 일정 시간(예: 10분) 이상 지속된 task를 `PENDING`으로
되돌리는 회수 스케줄이나, 처리 시작 시각 컬럼 + 타임아웃 판정.

## 3. 임시저장 판정의 센티널 문자열 의존

프론트가 `title === "임시글"`로 임시저장을 판정한다(`app/p/mine/page.tsx`). 사용자가 실제로
"임시글"이라는 제목을 붙인 미공개 글을 임시저장으로 오분류한다.

**해결 방향**: 백엔드 DTO에 `isTemp` 플래그를 내려 프론트가 문자열 비교를 하지 않게 한다.

## 4. e2e CI 도입 / 브라우저 매트릭스 확장

현재 e2e는 로컬 전용(Chromium). GitHub Actions에서 돌리려면 서비스 컨테이너(PG/Redis) 구성과
front build+start 경로가 필요하다. `.scratch/e2e-tests/spec.md`의 Out of Scope 참고.

## 5. adopt-slog-2026-03 잔여 항목

`.scratch/adopt-slog-2026-03/spec.md`의 Out of Scope에 기록된 3건:
소프트 삭제 전환, EntityAttr 값 컬럼 분리(intValue/strValue), 테스트 베이스 클래스 + Kotlin MockMvc DSL.
