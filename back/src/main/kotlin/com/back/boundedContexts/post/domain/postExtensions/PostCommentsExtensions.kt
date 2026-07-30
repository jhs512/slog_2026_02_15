package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.domain.PostComment
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository

// ================================
// 댓글 관리 (PostAttr + Repository 기반)
// ================================
// 리포지토리는 호출자(파사드)가 주입한다. 전역 홀더를 두면 여러 Spring 컨텍스트가 뜨는
// 테스트에서 다른 컨텍스트의 EntityManager에 붙은 리포지토리를 쓰게 되어 detached 오류가 난다.

val Post.commentsCount: Int
    get() = commentsCountAttr?.value?.toIntOrNull() ?: 0

private fun Post.setCommentsCount(value: Int, attrRepository: PostAttrRepository) {
    val attr = commentsCountAttr
        ?: attrRepository.findBySubjectAndName(this, Post.COMMENTS_COUNT)?.also { commentsCountAttr = it }
        ?: PostAttr(this, Post.COMMENTS_COUNT, value.toString()).also { commentsCountAttr = it }
    attr.value = value.toString()
    attrRepository.save(attr)
}

fun Post.getComments(commentRepository: PostCommentRepository): List<PostComment> =
    commentRepository.findByPostOrderByIdDesc(this)

fun Post.findCommentById(id: Int, commentRepository: PostCommentRepository): PostComment? =
    commentRepository.findByPostAndId(this, id)

fun Post.addComment(
    author: Member,
    content: String,
    commentRepository: PostCommentRepository,
    attrRepository: PostAttrRepository,
): PostComment {
    val postComment = PostComment(author, this, content)
    commentRepository.save(postComment)

    setCommentsCount(commentsCount + 1, attrRepository)
    author.incrementPostCommentsCount()

    return postComment
}

fun Post.deleteComment(
    postComment: PostComment,
    commentRepository: PostCommentRepository,
    attrRepository: PostAttrRepository,
) {
    postComment.author.decrementPostCommentsCount()
    setCommentsCount(commentsCount - 1, attrRepository)

    commentRepository.delete(postComment)
}
