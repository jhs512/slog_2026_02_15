package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.domain.PostComment
// ================================
// 댓글 관리 (PostAttr + Repository 기반)
// ================================

val Post.commentsCount: Int
    get() = commentsCountAttr?.value?.toIntOrNull() ?: 0

private fun Post.setCommentsCount(value: Int) {
    val attr = commentsCountAttr
        ?: postAttrRepository.findBySubjectAndName(this, Post.COMMENTS_COUNT)?.also { commentsCountAttr = it }
        ?: PostAttr(this, Post.COMMENTS_COUNT, value.toString()).also { commentsCountAttr = it }
    attr.value = value.toString()
    postAttrRepository.save(attr)
}

fun Post.getComments(): List<PostComment> =
    postCommentRepository.findByPostOrderByIdDesc(this)

fun Post.findCommentById(id: Int): PostComment? =
    postCommentRepository.findByPostAndId(this, id)

fun Post.addComment(author: Member, content: String): PostComment {
    val postComment = PostComment(author, this, content)
    postCommentRepository.save(postComment)

    setCommentsCount(commentsCount + 1)
    author.incrementPostCommentsCount()

    return postComment
}

fun Post.deleteComment(postComment: PostComment) {
    postComment.author.decrementPostCommentsCount()
    setCommentsCount(commentsCount - 1)

    postCommentRepository.delete(postComment)
}
