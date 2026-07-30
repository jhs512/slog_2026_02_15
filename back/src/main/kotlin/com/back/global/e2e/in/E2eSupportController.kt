package com.back.global.e2e.`in`

import com.back.global.app.app.AppFacade
import com.back.global.dto.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Profile
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * E2E 테스트 지원용 엔드포인트.
 *
 * `@Profile("e2e")`이므로 dev/test/prod 프로필에서는 빈으로 등록조차 되지 않는다.
 * 그래도 실수로 다른 프로필에 노출되는 일을 막기 위해 prod 여부를 한 번 더 검사한다.
 *
 * 존재 이유: e2e는 서버를 재사용(reuseExistingServer)하므로 `ddl-auto: create`만으로는
 * 매 실행마다 DB가 초기화되지 않는다. 콘텐츠가 누적되면 목록 페이지네이션에 밀려
 * 테스트가 조용히 깨진다. 실행 전에 이 엔드포인트로 콘텐츠를 비워 멱등성을 확보한다.
 */
@Profile("e2e")
@RestController
@RequestMapping("/e2e")
@Tag(name = "E2eSupportController", description = "E2E 테스트 지원 (e2e 프로필 전용)")
class E2eSupportController(
    private val entityManager: EntityManager,
) {
    @PostMapping("/reset")
    @Transactional
    @Operation(summary = "콘텐츠 초기화", description = "시드 계정은 남기고 글·댓글·좋아요·로그·태스크를 비운다")
    fun reset(): RsData<Void> {
        check(!AppFacade.isProd) { "E2E 초기화는 운영 환경에서 사용할 수 없습니다." }

        // post ↔ post_attr은 서로 FK를 갖는다. post 쪽 참조를 먼저 끊어야 attr을 지울 수 있다.
        exec("UPDATE post SET likes_count_attr_id = NULL, comments_count_attr_id = NULL, hit_count_attr_id = NULL")
        exec("DELETE FROM post_attr")
        exec("DELETE FROM post_comment")
        exec("DELETE FROM post_like")
        exec("DELETE FROM post")

        // 회원은 유지하고 카운터만 0으로 되돌린다 (profileImgUrl 등 다른 attr은 보존).
        // 삭제하면 안 된다 — (subject_id, name) 유니크 제약이 있는데 병렬 요청이 동시에
        // "없으면 생성" 경로를 타면 중복 INSERT로 충돌한다. 모든 회원에 대해 0을 보장해
        // 이후 요청이 항상 UPDATE 경로만 타게 만든다.
        exec(
            """
            INSERT INTO member_attr (id, created_at, modified_at, name, subject_id, val)
            SELECT nextval('member_attr_seq'), now(), now(), n.name, m.id, '0'
            FROM member m CROSS JOIN (VALUES ('postsCount'), ('postCommentsCount')) AS n(name)
            ON CONFLICT (subject_id, name) DO UPDATE SET val = '0'
            """
        )

        exec("DELETE FROM member_action_log")
        exec("DELETE FROM task")

        // 시퀀스도 1부터 다시 — 테스트가 id를 눈으로 확인할 때 읽기 쉽다
        listOf("post_seq", "post_attr_seq", "post_comment_seq", "post_like_seq", "member_action_log_seq", "task_seq")
            .forEach { exec("ALTER SEQUENCE $it RESTART WITH 1") }

        // member_attr은 위에서 nextval을 썼으므로 현재 최댓값 다음으로 맞춘다.
        // setval은 값을 반환하므로 executeUpdate가 아니라 결과를 읽어야 한다.
        entityManager
            .createNativeQuery("SELECT setval('member_attr_seq', COALESCE((SELECT MAX(id) FROM member_attr), 0) + 1, false)")
            .singleResult

        entityManager.clear()

        return RsData("200-1", "E2E 콘텐츠가 초기화되었습니다.")
    }

    private fun exec(sql: String) {
        entityManager.createNativeQuery(sql).executeUpdate()
    }
}
