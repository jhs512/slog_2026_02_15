package com.back.boundedContexts.post.app

import com.back.boundedContexts.member.domain.Member
import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.out.PostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostFacade(
    private val postRepository: PostRepository,
) {
    @Transactional(readOnly = true)
    fun count(): Long = postRepository.count()

    @Transactional
    fun write(id: Long, author: Member, title: String, body: String): Post {
        return postRepository.save(
            Post(id, author, title, body)
        )
    }
}
