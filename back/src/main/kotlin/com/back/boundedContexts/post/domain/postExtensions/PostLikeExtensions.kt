package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.member.domain.shared.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.domain.PostLike
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostLikeRepository

// ================================
// 좋아요 관리 (PostAttr + Repository 기반)
// ================================
// 리포지토리는 호출자(파사드)가 주입한다 — PostCommentsExtensions의 주석 참고.

val Post.likesCount: Int
    get() = likesCountAttr?.value?.toIntOrNull() ?: 0

private fun Post.setLikesCount(value: Int, attrRepository: PostAttrRepository) {
    val attr = likesCountAttr
        ?: attrRepository.findBySubjectAndName(this, Post.LIKES_COUNT)?.also { likesCountAttr = it }
        ?: PostAttr(this, Post.LIKES_COUNT, value.toString()).also { likesCountAttr = it }
    attr.value = value.toString()
    attrRepository.save(attr)
}

fun Post.isLikedBy(liker: Member?, likeRepository: PostLikeRepository): Boolean {
    if (liker == null) return false
    return likeRepository.findByLikerAndPost(liker, this) != null
}

data class PostLikeToggleResult(
    val isLiked: Boolean,
    val likeId: Int,
)

/**
 * 좋아요 토글
 * @return isLiked true: 좋아요 추가됨, false: 좋아요 취소됨
 */
fun Post.toggleLike(
    liker: Member,
    likeRepository: PostLikeRepository,
    attrRepository: PostAttrRepository,
): PostLikeToggleResult {
    val existingLike = likeRepository.findByLikerAndPost(liker, this)

    return if (existingLike != null) {
        likeRepository.delete(existingLike)
        setLikesCount(likesCount - 1, attrRepository)

        PostLikeToggleResult(false, existingLike.id)
    } else {
        val newLike = PostLike(liker, this)
        val savedLike = likeRepository.save(newLike)
        setLikesCount(likesCount + 1, attrRepository)

        PostLikeToggleResult(true, savedLike.id)
    }
}
