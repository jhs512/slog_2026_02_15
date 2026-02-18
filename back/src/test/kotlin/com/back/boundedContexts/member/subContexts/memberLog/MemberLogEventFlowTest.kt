package com.back.boundedContexts.member.subContexts.memberLog

import com.back.boundedContexts.member.app.shared.ActorFacade
import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.subContexts.memberLog.domain.MemberLog
import com.back.boundedContexts.member.subContexts.memberLog.out.MemberLogRepository
import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.domain.PostLike
import com.back.boundedContexts.post.event.PostCommentDeletedEvent
import com.back.boundedContexts.post.event.PostCommentModifiedEvent
import com.back.boundedContexts.post.event.PostCommentWrittenEvent
import com.back.boundedContexts.post.event.PostDeletedEvent
import com.back.boundedContexts.post.event.PostLikeToggledEvent
import com.back.boundedContexts.post.event.PostModifiedEvent
import com.back.boundedContexts.post.event.PostWrittenEvent
import com.back.standard.extensions.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest
class MemberLogEventFlowTest {
    @Autowired
    private lateinit var actorFacade: ActorFacade

    @Autowired
    private lateinit var postFacade: PostFacade

    @Autowired
    private lateinit var memberLogRepository: MemberLogRepository

    @Test
    fun `회원 이벤트가 발생하면 해당 회원 로그가 기록되어 남는지 확인한다`() {
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
            PostLikeToggledEvent::class.simpleName!!,
            PostLike::class.simpleName!!,
            { it.likeId },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )
        assertThat(likeResult.likeId).isPositive()

        val unliked = verifyAction(
            { postFacade.toggleLike(post, actor) },
            PostLikeToggledEvent::class.simpleName!!,
            PostLike::class.simpleName!!,
            { it.likeId },
            Post::class.simpleName!!,
            { post.id },
            actor.id
        )
        assertThat(unliked.likeId).isPositive()

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
        assertThat(added).hasSize(1)

        val log = added.single()

        assertThat(log.type).isEqualTo(expectedType)
        assertThat(log.primaryType).isEqualTo(expectedPrimaryType)
        assertThat(log.primaryId).isEqualTo(expectedPrimaryId)
        assertThat(log.secondaryType).isEqualTo(expectedSecondaryType)
        assertThat(log.secondaryId).isEqualTo(expectedSecondaryId)
        assertThat(log.actor.id).isEqualTo(expectedActorId)
    }

    private fun lastLogId(): Int =
        memberLogRepository.findAll().maxOfOrNull { it.id } ?: 0

    private fun addedLogs(before: Int): List<MemberLog> =
        memberLogRepository.findAll().filter { it.id > before }
}
