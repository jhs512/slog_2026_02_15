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
import org.junit.jupiter.api.DisplayName
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
    @DisplayName("글/댓글/좋아요 액션마다 MemberLog가 하나씩 남는다")
    fun `MemberLog 기록 확인`() {
        val actor = actorFacade.findByUsername("user1").getOrThrow()

        val post = verifyAction(
            action = { postFacade.write(actor, "로그 테스트 글", "로그 테스트 내용", published = true, listed = true) },
            expectedType = PostWrittenEvent::class.simpleName!!,
            expectedPrimaryType = Post::class.simpleName!!,
            expectedPrimaryId = { it.id },
            expectedSecondaryType = Member::class.simpleName!!,
            expectedSecondaryId = { actor.id },
            expectedActorId = actor.id
        )

        verifyAction(
            action = { postFacade.modify(actor, post, "로그 테스트 글 수정", "로그 테스트 내용 수정", published = true, listed = true) },
            expectedType = PostModifiedEvent::class.simpleName!!,
            expectedPrimaryType = Post::class.simpleName!!,
            expectedPrimaryId = { post.id },
            expectedSecondaryType = Member::class.simpleName!!,
            expectedSecondaryId = { actor.id },
            expectedActorId = actor.id
        )

        val comment = verifyAction(
            action = { postFacade.writeComment(actor, post, "댓글 테스트") },
            expectedType = PostCommentWrittenEvent::class.simpleName!!,
            expectedPrimaryType = PostComment::class.simpleName!!,
            expectedPrimaryId = { it.id },
            expectedSecondaryType = Post::class.simpleName!!,
            expectedSecondaryId = { post.id },
            expectedActorId = actor.id
        )

        verifyAction(
            action = { postFacade.modifyComment(comment, actor, "댓글 수정") },
            expectedType = PostCommentModifiedEvent::class.simpleName!!,
            expectedPrimaryType = PostComment::class.simpleName!!,
            expectedPrimaryId = { comment.id },
            expectedSecondaryType = Post::class.simpleName!!,
            expectedSecondaryId = { post.id },
            expectedActorId = actor.id
        )

        val likeResult = verifyAction(
            action = { postFacade.toggleLike(post, actor) },
            expectedType = PostLikeToggledEvent::class.simpleName!!,
            expectedPrimaryType = PostLike::class.simpleName!!,
            expectedPrimaryId = { it.likeId },
            expectedSecondaryType = Post::class.simpleName!!,
            expectedSecondaryId = { post.id },
            expectedActorId = actor.id
        )
        assertThat(likeResult.likeId).isPositive()

        val unliked = verifyAction(
            action = { postFacade.toggleLike(post, actor) },
            expectedType = PostLikeToggledEvent::class.simpleName!!,
            expectedPrimaryType = PostLike::class.simpleName!!,
            expectedPrimaryId = { it.likeId },
            expectedSecondaryType = Post::class.simpleName!!,
            expectedSecondaryId = { post.id },
            expectedActorId = actor.id
        )
        assertThat(unliked.likeId).isPositive()

        verifyAction(
            action = { postFacade.deleteComment(post, comment, actor) },
            expectedType = PostCommentDeletedEvent::class.simpleName!!,
            expectedPrimaryType = PostComment::class.simpleName!!,
            expectedPrimaryId = { comment.id },
            expectedSecondaryType = Post::class.simpleName!!,
            expectedSecondaryId = { post.id },
            expectedActorId = actor.id
        )

        verifyAction(
            action = { postFacade.delete(post, actor) },
            expectedType = PostDeletedEvent::class.simpleName!!,
            expectedPrimaryType = Post::class.simpleName!!,
            expectedPrimaryId = { post.id },
            expectedSecondaryType = Member::class.simpleName!!,
            expectedSecondaryId = { actor.id },
            expectedActorId = actor.id
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

        verifyLog(
            before = before,
            expectedType = expectedType,
            expectedPrimaryType = expectedPrimaryType,
            expectedPrimaryId = expectedPrimaryId(result),
            expectedSecondaryType = expectedSecondaryType,
            expectedSecondaryId = expectedSecondaryId(result),
            expectedActorId = expectedActorId
        )

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
