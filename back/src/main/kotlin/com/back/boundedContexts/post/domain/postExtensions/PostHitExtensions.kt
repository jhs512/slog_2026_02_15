package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.domain.PostAttr
import com.back.boundedContexts.post.out.PostAttrRepository

// ================================
// 조회수 관리 (PostAttr 기반)
// ================================
val Post.hitCount: Int
    get() = hitCountAttr?.value?.toIntOrNull() ?: 0

// 전역 postAttrRepository 대신 호출자(파사드)의 리포지토리를 받는다.
// 테스트에서 여러 Spring 컨텍스트가 동시에 살아있으면 전역 리포지토리가 다른 컨텍스트의
// EntityManagerFactory에 묶여, 여기서 저장한 PostAttr가 현재 세션 기준 detached가 되어
// Post의 cascade PERSIST에서 "Detached entity passed to persist"가 발생한다.
fun Post.incrementHitCount(attrRepository: PostAttrRepository) {
    val attr = hitCountAttr
        ?: attrRepository.findBySubjectAndName(this, Post.HIT_COUNT)?.also { hitCountAttr = it }
        ?: PostAttr(this, Post.HIT_COUNT, "0").also { hitCountAttr = it }
    attr.value = (hitCount + 1).toString()
    attrRepository.save(attr)
}
