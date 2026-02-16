package com.back.boundedContexts.post.app

import com.back.boundedContexts.member.domain.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.out.PostRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostFacade(
    private val postRepository: PostRepository,
) {
    @Transactional(readOnly = true)
    fun count(): Long = postRepository.count()

    @Transactional(readOnly = true)
    fun findByKeyword(
        keyword: String,
        page: Int,
        size: Int
    ): Page<Post> {
        return postRepository.findByKeyword(
            keyword,
            PageRequest.of(page, size)
        )
    }

    @Transactional
    fun write(id: Long, author: Member, title: String, body: String): Post {
        return postRepository.save(
            Post(id, author, title, body)
        )
    }
}
