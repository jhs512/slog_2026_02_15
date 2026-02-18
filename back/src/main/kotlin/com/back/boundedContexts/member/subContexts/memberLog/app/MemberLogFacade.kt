package com.back.boundedContexts.member.subContexts.memberLog.app

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.subContexts.memberLog.domain.MemberLog
import com.back.boundedContexts.member.subContexts.memberLog.out.MemberLogRepository
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
import com.back.standard.dto.EventPayload
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class MemberLogFacade(
    private val memberLogRepository: MemberLogRepository,
) {
    fun save(event: EventPayload) {
        when (event) {
            is PostWrittenEvent -> save(event)
            is PostModifiedEvent -> save(event)
            is PostDeletedEvent -> save(event)
            is PostCommentWrittenEvent -> save(event)
            is PostCommentModifiedEvent -> save(event)
            is PostCommentDeletedEvent -> save(event)
            is PostLikeToggledEvent -> save(event)
            else -> {}
        }
    }

    private fun save(event: PostWrittenEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostWrittenEvent::class.simpleName!!,
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member::class.simpleName!!,
                event.actorDto.id,
                Member(event.actorDto.id),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostModifiedEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostModifiedEvent::class.simpleName!!,
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member::class.simpleName!!,
                event.actorDto.id,
                Member(event.actorDto.id),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostDeletedEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostDeletedEvent::class.simpleName!!,
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member::class.simpleName!!,
                event.actorDto.id,
                Member(event.actorDto.id),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostCommentWrittenEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostCommentWrittenEvent::class.simpleName!!,
                PostComment::class.simpleName!!,
                event.postCommentDto.id,
                Member(event.postCommentDto.authorId),
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostCommentModifiedEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostCommentModifiedEvent::class.simpleName!!,
                PostComment::class.simpleName!!,
                event.postCommentDto.id,
                Member(event.postCommentDto.authorId),
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostCommentDeletedEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostCommentDeletedEvent::class.simpleName!!,
                PostComment::class.simpleName!!,
                event.postCommentDto.id,
                Member(event.postCommentDto.authorId),
                Post::class.simpleName!!,
                event.postDto.id,
                Member(event.postDto.authorId),
                Member(event.actorDto.id),
                data
            )
        )
    }

    private fun save(event: PostLikeToggledEvent) {
        val data = Ut.JSON.toString(event)

        memberLogRepository.save(
            MemberLog(
                PostLikeToggledEvent::class.simpleName!!,
                PostLike::class.simpleName!!,
                event.likeId,
                Member(event.actorDto.id),
                Post::class.simpleName!!,
                event.postId,
                Member(event.postAuthorId),
                Member(event.actorDto.id),
                data
            )
        )
    }
}
