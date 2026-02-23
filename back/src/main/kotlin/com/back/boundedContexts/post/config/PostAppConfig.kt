package com.back.boundedContexts.post.config

import com.back.boundedContexts.post.domain.postExtensions.isPostReposInitialized
import com.back.boundedContexts.post.domain.postExtensions.postAttrRepository
import com.back.boundedContexts.post.domain.postExtensions.postCommentRepository
import com.back.boundedContexts.post.domain.postExtensions.postLikeRepository
import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration

@Configuration
class PostAppConfig(
    attrRepository: PostAttrRepository,
    likeRepository: PostLikeRepository,
    commentRepository: PostCommentRepository,
    applicationContext: ApplicationContext,
) {
    init {
        // WebServer 컨텍스트(RANDOM_PORT)는 이미 초기화된 경우 덮어쓰지 않는다.
        // 테스트 시 여러 Spring 컨텍스트가 전역 레포지토리를 덮어쓰는 문제를 방지한다.
        if (applicationContext !is WebServerApplicationContext || !isPostReposInitialized()) {
            postAttrRepository = attrRepository
            postLikeRepository = likeRepository
            postCommentRepository = commentRepository
        }
    }
}
