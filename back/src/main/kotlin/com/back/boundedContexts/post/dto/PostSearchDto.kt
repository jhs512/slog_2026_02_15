package com.back.boundedContexts.post.dto

import com.back.boundedContexts.post.domain.Post
import java.time.Instant

data class PostSearchDto(
    val id: Long,
    val title: String,
    val body: String,
    val authorId: Long,
    val createdAt: Instant,
) {
    companion object {
        fun from(post: Post): PostSearchDto = PostSearchDto(
            id = post.id,
            title = post.title,
            body = post.body,
            authorId = post.author.id,
            createdAt = post.createdAt,
        )
    }
}
