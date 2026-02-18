package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.domain.PostLike

// ================================
// 좋아요 관리 (PostAttr + Repository 기반)
// ================================

val Post.likesCount: Int
    get() = likesCountAttr?.value?.toIntOrNull() ?: 0

private fun Post.setLikesCount(value: Int) {
    if (likesCountAttr == null)
        likesCountAttr = PostAttr(this, Post.LIKES_COUNT, value.toString())
    else
        likesCountAttr!!.value = value.toString()

    Post.postAttrRepository.save(likesCountAttr!!)
}

private fun Post.increaseLikesCount() {
    setLikesCount(likesCount + 1)
}

private fun Post.decreaseLikesCount() {
    setLikesCount(likesCount - 1)
}

fun Post.isLikedBy(liker: Member?): Boolean {
    if (liker == null) return false
    return Post.postLikeRepository.findByLikerAndPost(liker, this) != null
}

data class PostLikeToggleResult(
    val isLiked: Boolean,
    val likeId: Int,
)

/**
 * 좋아요 토글
 * @return true: 좋아요 추가됨, false: 좋아요 취소됨
 */
fun Post.toggleLike(liker: Member): PostLikeToggleResult {
    val existingLike = Post.postLikeRepository.findByLikerAndPost(liker, this)

    return if (existingLike != null) {
        Post.postLikeRepository.delete(existingLike)
        decreaseLikesCount()

        PostLikeToggleResult(false, existingLike.id)
    } else {
        val newLike = PostLike(liker, this)
        val savedLike = Post.postLikeRepository.save(newLike)
        increaseLikesCount()

        PostLikeToggleResult(true, savedLike.id)
    }
}
