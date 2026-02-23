package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr

// ================================
// 조회수 관리 (PostAttr 기반)
// ================================
val Post.hitCount: Int
    get() = hitCountAttr?.value?.toIntOrNull() ?: 0

fun Post.incrementHitCount() {
    val attr = hitCountAttr
        ?: postAttrRepository.findBySubjectAndName(this, Post.HIT_COUNT)?.also { hitCountAttr = it }
        ?: PostAttr(this, Post.HIT_COUNT, "0").also { hitCountAttr = it }
    attr.value = (hitCount + 1).toString()
    postAttrRepository.save(attr)
}
