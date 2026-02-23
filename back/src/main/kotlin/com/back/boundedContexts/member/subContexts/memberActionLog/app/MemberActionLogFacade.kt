package com.back.boundedContexts.member.subContexts.memberActionLog.app

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.subContexts.memberActionLog.domain.MemberActionLog
import com.back.boundedContexts.member.subContexts.memberActionLog.out.MemberActionLogRepository
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.domain.PostLike
import com.back.boundedContexts.post.event.*
import com.back.standard.dto.EventPayload
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class MemberActionLogFacade(
    private val memberActionLogRepository: MemberActionLogRepository,
) {
    fun save(event: EventPayload) {
        when (event) {
            is PostWrittenEvent -> save(event)
            is PostModifiedEvent -> save(event)
            is PostDeletedEvent -> save(event)
            is PostCommentWrittenEvent -> save(event)
            is PostCommentModifiedEvent -> save(event)
            is PostCommentDeletedEvent -> save(event)
            is PostLikedEvent -> save(event)
            is PostUnlikedEvent -> save(event)
            else -> {}
        }
    }

    private fun save(event: PostWrittenEvent) {
        val data = Ut.JSON.toString(event)

        memberActionLogRepository.save(
            MemberActionLog(
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

        memberActionLogRepository.save(
            MemberActionLog(
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

        memberActionLogRepository.save(
            MemberActionLog(
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

        memberActionLogRepository.save(
            MemberActionLog(
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

        memberActionLogRepository.save(
            MemberActionLog(
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

        memberActionLogRepository.save(
            MemberActionLog(
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

    private fun save(event: PostLikedEvent) {
        val data = Ut.JSON.toString(event)

        memberActionLogRepository.save(
            MemberActionLog(
                PostLikedEvent::class.simpleName!!,
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

    private fun save(event: PostUnlikedEvent) {
        val data = Ut.JSON.toString(event)

        memberActionLogRepository.save(
            MemberActionLog(
                PostUnlikedEvent::class.simpleName!!,
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
