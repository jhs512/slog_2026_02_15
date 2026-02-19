package com.back.boundedContexts.post.app

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.member.dto.MemberDto
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.domain.postExtensions.PostLikeToggleResult
import com.back.boundedContexts.post.domain.postExtensions.addComment
import com.back.boundedContexts.post.domain.postExtensions.deleteComment
import com.back.boundedContexts.post.domain.postExtensions.toggleLike
import com.back.boundedContexts.post.dto.PostCommentDto
import com.back.boundedContexts.post.dto.PostDto
import com.back.boundedContexts.post.event.PostCommentDeletedEvent
import com.back.boundedContexts.post.event.PostCommentModifiedEvent
import com.back.boundedContexts.post.event.PostCommentWrittenEvent
import com.back.boundedContexts.post.event.PostDeletedEvent
import com.back.boundedContexts.post.event.PostLikeToggledEvent
import com.back.boundedContexts.post.event.PostModifiedEvent
import com.back.boundedContexts.post.event.PostWrittenEvent
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository
import com.back.boundedContexts.post.out.PostRepository
import com.back.global.event.app.EventPublisher
import com.back.standard.dto.post.type1.PostSearchSortType1
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class PostFacade(
    private val postRepository: PostRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postAttrRepository: PostAttrRepository,
    private val postCommentRepository: PostCommentRepository,
    private val eventPublisher: EventPublisher,
    private val postNotificationService: PostNotificationService,
) {
    fun count(): Long = postRepository.count()

    @Transactional
    fun write(
        author: Member,
        title: String,
        content: String,
        published: Boolean = false,
        listed: Boolean = false,
    ): Post {
        val post = Post(author, title, content, published, listed)

        author.incrementPostsCount()

        val savedPost = postRepository.save(post)

        eventPublisher.publish(
            PostWrittenEvent(
                UUID.randomUUID(),
                PostDto(savedPost),
                MemberDto(author)
            )
        )

        return savedPost
    }


    fun findById(id: Int): Post? = postRepository.findById(id).getOrNull()


    @Transactional
    fun modify(
        actor: Member,
        post: Post,
        title: String,
        content: String,
        published: Boolean? = null,
        listed: Boolean? = null,
    ) {
        val wasPublishedAndListed = post.published && post.listed

        post.modify(title, content, published, listed)

        val isNowPublishedAndListed = post.published && post.listed

        // 처음 공개될 때만 알림
        if (!wasPublishedAndListed && isNowPublishedAndListed) {
            postNotificationService.notifyNewPost(post)
        }

        // 글 본문 변경사항 구독자에게 알림
        postNotificationService.notifyPostModified(post)

        eventPublisher.publish(
            PostModifiedEvent(
                UUID.randomUUID(),
                PostDto(post),
                MemberDto(actor)
            )
        )
    }

    fun findPagedByAuthor(
        author: Member,
        kw: String,
        sort: PostSearchSortType1,
        page: Int,
        pageSize: Int,
    ): Page<Post> = postRepository.findQPagedByAuthorAndKw(
        author,
        kw,
        PageRequest.of(page - 1, pageSize, sort.sortBy)
    )

    fun findTemp(author: Member) = postRepository.findFirstByAuthorAndTitleAndPublishedFalseOrderByIdAsc(author, "임시글")

    /**
     * 임시저장 글 조회 또는 생성
     * @return Pair(Post, isNew) - 기존 글이면 false, 새로 생성이면 true
     */
    @Transactional
    fun getOrCreateTemp(author: Member): Pair<Post, Boolean> {
        val existingTemp = findTemp(author)

        if (existingTemp != null) {
            return existingTemp to false
        }

        val newPost = Post(
            author,
            "임시글",
            "임시글 입니다.",
        )

        author.incrementPostsCount()

        return postRepository.save(newPost) to true
    }


    @Transactional
    fun writeComment(author: Member, post: Post, content: String): PostComment {
        val postComment = post.addComment(author, content)

        postRepository.flush()

        eventPublisher.publish(
            PostCommentWrittenEvent(
                UUID.randomUUID(),
                PostCommentDto(postComment),
                PostDto(post),
                MemberDto(author)
            )
        )

        return postComment
    }


    @Transactional
    fun deleteComment(post: Post, postComment: PostComment, actor: Member) {
        val postCommentDto = PostCommentDto(postComment)
        val postDto = PostDto(post)

        post.deleteComment(postComment)

        eventPublisher.publish(
            PostCommentDeletedEvent(
                UUID.randomUUID(),
                postCommentDto,
                postDto,
                MemberDto(actor)
            )
        )
    }


    @Transactional
    fun modifyComment(postComment: PostComment, actor: Member, content: String) {
        postComment.modify(content)

        eventPublisher.publish(
            PostCommentModifiedEvent(
                UUID.randomUUID(),
                PostCommentDto(postComment),
                PostDto(postComment.post),
                MemberDto(actor)
            )
        )
    }

    @Transactional
    fun delete(post: Post, actor: Member) {
        val postDto = PostDto(post)
        val actorDto = MemberDto(actor)

        post.author.decrementPostsCount()

        // 삭제 이벤트를 post 삭제 전에 발행 (이벤트 리스너는 BEFORE_COMMIT 유지)
        eventPublisher.publish(
            PostDeletedEvent(
                UUID.randomUUID(),
                postDto,
                actorDto
            )
        )

        // Post의 FK 참조를 먼저 null로 설정하고 flush해야 PostAttr 삭제 시 FK 제약 위반 방지
        post.likesCountAttr = null
        post.commentsCountAttr = null
        post.hitCountAttr = null
        postRepository.flush()

        postAttrRepository.deleteBySubjectId(post.id)
        postCommentRepository.deleteByPost(post)
        postLikeRepository.deleteByPost(post)

        postRepository.deleteRowById(post.id)
    }


    @Transactional
    fun toggleLike(post: Post, actor: Member): PostLikeToggleResult {
        val likeResult = post.toggleLike(actor)

        eventPublisher.publish(
            PostLikeToggledEvent(
                UUID.randomUUID(),
                post.id,
                post.author.id,
                likeResult.likeId,
                likeResult.isLiked,
                MemberDto(actor)
            )
        )

        return likeResult
    }


    fun findLatest() = postRepository.findFirstByOrderByIdDesc()


    fun findPagedByKw(
        kw: String,
        sort: PostSearchSortType1,
        page: Int,
        pageSize: Int
    ): Page<Post> =
        postRepository.findQPagedByKw(
            kw,
            PageRequest.of(
                page - 1,
                pageSize,
                sort.sortBy
            )
        )

    fun findLikedPostIds(liker: Member?, posts: List<Post>): Set<Int> {
        if (liker == null || posts.isEmpty()) return emptySet()

        return postLikeRepository
            .findByLikerAndPostIn(liker, posts)
            .map { it.post.id }
            .toSet()
    }
}
