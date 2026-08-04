# 백로그

티켓화되지 않은, 근본 해결이 필요한 항목들.

## 1. post 도메인의 전역 레포지토리 홀더 제거 — ✅ 2026-07-30 완료

`postExtensions/PostRepositories.kt`의 톱레벨 `lateinit var` 3개(`postAttrRepository`,
`postCommentRepository`, `postLikeRepository`)와 `PostAppConfig`의 "첫 컨텍스트 승리" 가드.

**문제**: 테스트가 여러 Spring 컨텍스트를 띄우면 일부 테스트가 다른 컨텍스트의 EntityManager에
붙은 레포지토리를 사용한다. 증상은 `Detached entity passed to persist`, 또는 저장한 엔티티가
현재 세션에 보이지 않아 FK 제약 위반. **테스트를 추가할 때마다 무관해 보이는 테스트가 깨진다.**

**이력**: 2026-07-29 `PostHitExtensions.incrementHitCount()`만 리포지토리를 파라미터로 받도록
고쳤다(당시 PostSseServiceTest 실패 대응). 2026-07-30 이벤트 직렬화 테스트를 추가하자
좋아요/댓글 경로에서 같은 증상이 재발해, 테스트를 순수 단위 테스트로 바꿔 우회했다.

**해결(2026-07-30)**: PostCommentsExtensions·PostLikeExtensions의 함수들이 리포지토리를 파라미터로
받도록 변경, 컨트롤러가 확장 함수를 직접 부르던 3곳은 파사드 경유로 정리,
`PostRepositories.kt`·`PostAppConfig` 삭제. 검증: 홀더가 있을 때 13건을 실패시켰던
@SpringBootTest 형태를 되돌려도 CI 동등 Docker 빌드가 통과함을 확인.

## 2. PROCESSING 상태로 영구히 멈추는 task 회수 로직 부재 — ✅ 2026-07-31 완료

`markAsProcessing()` 후 프로세스가 죽거나 예외가 삼켜지면 그 task를 다시 집는 주체가 없다.
prod에서 task 4581이 2026-05-20부터 두 달 넘게 `PROCESSING`으로 방치됐다(2026-07-30 수동 재큐잉).

**해결(2026-07-31)**: `TaskRepository.reclaimStaleProcessingTasks`가 modified_at 기준으로
10분 넘게 PROCESSING에 머문 task를 PENDING으로 되돌린다. 스케줄러가 매 주기 PENDING을 집기 전에
먼저 수행하고, 회수 시 warn 로그를 남긴다. 처리 시작 시각 컬럼은 두지 않고 modified_at을
근사치로 쓴다(PROCESSING 중에는 다른 수정이 없어 사실상 동일). 경계 양쪽을 TaskReclaimTest로 고정.

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

## 7. 비공개 글 SSR 인증 사각지대 잔여 경로

쿠키가 Domain=api.slog.gg라 Next 서버(www)에는 인증 쿠키가 실리지 않는다.
`/p/[id]`는 2026-08-04 브라우저 재조회 폴백으로 해결했지만, 같은 패턴의
ppt 페이지(`/p/[id]/ppt`)와 raw 라우트(`/p/[id]/raw`)는 여전히 비공개 글을
작성자에게도 403으로 응답한다. 또 비공개 글의 SSR 메타데이터(title)는 에러 문구가 들어간다.
근본 해결은 쿠키 도메인 slog.gg 복귀 또는 해당 경로의 클라이언트 전환.

## 6. member_attr 최초 생성 경합 (race condition)

`member_attr`에는 `(subject_id, name)` 유니크 제약이 있는데, 해당 attr이 아직 없는 상태에서
같은 회원에 대한 요청이 동시에 들어오면 각자 "없으면 생성" 경로를 타 중복 INSERT로 충돌한다
(`duplicate key value violates unique constraint`). 실사용에서는 회원이 첫 글/첫 댓글을
동시에 여러 개 만들 때만 드러나 잘 보이지 않는다.

**발견 경위**: 2026-07-30 E2E 리셋이 카운터 attr을 DELETE하자 매 실행마다 이 조건이 만들어져
병렬 테스트가 깨졌다. 리셋을 UPSERT(0 보장)로 바꿔 우회했지만 앱 코드의 결함은 남아 있다.

**해결 방향**: attr 생성 지점을 `INSERT ... ON CONFLICT DO UPDATE`(upsert)로 바꾸거나,
회원 가입 시점에 카운터 attr을 미리 만들어 두어 "생성" 경로 자체를 없앤다.
