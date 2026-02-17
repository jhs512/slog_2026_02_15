package com.back.boundedContexts.post.config

import com.back.boundedContexts.post.domain.Post
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository
import org.springframework.context.annotation.Configuration

@Configuration
class PostAppConfig(
    postLikeRepository: PostLikeRepository,
    postAttrRepository: PostAttrRepository,
    postCommentRepository: PostCommentRepository,
) {
    init {
        Post.postLikeRepository_ = postLikeRepository
        Post.postAttrRepository_ = postAttrRepository
        Post.postCommentRepository_ = postCommentRepository
    }
}
