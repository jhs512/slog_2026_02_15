package com.back.boundedContexts.member.subContexts.memberActionLog.`in`

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.subContexts.memberActionLog.domain.MemberActionLog
import com.back.boundedContexts.member.subContexts.memberActionLog.out.MemberActionLogRepository
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.domain.PostLike
import com.back.boundedContexts.post.event.*
import com.back.standard.extensions.getOrThrow
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MemberActionLogEventListenerTest {
    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var memberActionLogRepository: MemberActionLogRepository

    @Test
    fun `회원 이벤트가 발생하면 해당 회원 액션 로그가 기록되어 남는지 확인한다`() {
        val actor = actorFacade.findByUsername("user1").getOrThrow()

        val post = verifyAction(
            { postFacade.write(actor, "로그 테스트 글", "로그 테스트 내용", true, true) },
            PostWrittenEvent::class.simpleName!!,
            Post::class.simpleName!!,
            { it.id },
            Member::class.simpleName!!,
            { actor.id },
            actor.id
        )

        verifyAction(
            { postFacade.modify(actor, post, "로그 테스트 글 수정", "로그 테스트 내용 수정", true, true) },
            PostModifiedEvent::class.simpleName!!,
            Post::class.simpleName!!,
            { post.id },
            Member::class.simpleName!!,
            { actor.id },
            actor.id
        )

        val comment = verifyAction(
            { postFacade.writeComment(actor, post, "댓글 테스트") },
            PostCommentWrittenEvent::class.simpleName!!,
            PostComment::class.simpleName!!,
            { it.id },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )

        verifyAction(
            { postFacade.modifyComment(comment, actor, "댓글 수정") },
            PostCommentModifiedEvent::class.simpleName!!,
            PostComment::class.simpleName!!,
            { comment.id },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )

        val likeResult = verifyAction(
            { postFacade.toggleLike(post, actor) },
            PostLikedEvent::class.simpleName!!,
            PostLike::class.simpleName!!,
            { it.likeId },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )
        Assertions.assertThat(likeResult.likeId).isPositive()

        val unliked = verifyAction(
            { postFacade.toggleLike(post, actor) },
            PostUnlikedEvent::class.simpleName!!,
            PostLike::class.simpleName!!,
            { it.likeId },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )
        Assertions.assertThat(unliked.likeId).isPositive()

        verifyAction(
            { postFacade.deleteComment(post, comment, actor) },
            PostCommentDeletedEvent::class.simpleName!!,
            PostComment::class.simpleName!!,
            { comment.id },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )

        verifyAction(
            { postFacade.delete(post, actor) },
            PostDeletedEvent::class.simpleName!!,
            Post::class.simpleName!!,
            { post.id },
            Member::class.simpleName!!,
            { actor.id },
            actor.id
        )
    }

    private fun <T> verifyAction(
        action: () -> T,
        expectedType: String,
        expectedPrimaryType: String,
        expectedPrimaryId: (T) -> Int,
        expectedSecondaryType: String,
        expectedSecondaryId: (T) -> Int,
        expectedActorId: Int,
    ): T {
        val before = lastLogId()
        val result = action()

        verifyLog(before, expectedType, expectedPrimaryType, expectedPrimaryId(result), expectedSecondaryType, expectedSecondaryId(result), expectedActorId)

        return result
    }

    private fun verifyLog(
        before: Int,
        expectedType: String,
        expectedPrimaryType: String,
        expectedPrimaryId: Int,
        expectedSecondaryType: String,
        expectedSecondaryId: Int,
        expectedActorId: Int,
    ) {
        val added = addedLogs(before)
        Assertions.assertThat(added).hasSize(1)

        val log = added.single()

        Assertions.assertThat(log.type).isEqualTo(expectedType)
        Assertions.assertThat(log.primaryType).isEqualTo(expectedPrimaryType)
        Assertions.assertThat(log.primaryId).isEqualTo(expectedPrimaryId)
        Assertions.assertThat(log.secondaryType).isEqualTo(expectedSecondaryType)
        Assertions.assertThat(log.secondaryId).isEqualTo(expectedSecondaryId)
        Assertions.assertThat(log.actor.id).isEqualTo(expectedActorId)
    }

    private fun lastLogId(): Int =
        memberActionLogRepository.findAll().maxOfOrNull { it.id } ?: 0

    private fun addedLogs(before: Int): List<MemberActionLog> =
        memberActionLogRepository.findAll().filter { it.id > before }
}