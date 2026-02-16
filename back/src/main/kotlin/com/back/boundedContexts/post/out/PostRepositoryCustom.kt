package com.back.boundedContexts.post.out

import com.back.boundedContexts.post.domain.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PostRepositoryCustom {
    fun findByKeyword(keyword: String, pageable: Pageable): Page<Post>
}
