package com.back.boundedContexts.post.domain.postExtensions

import com.back.boundedContexts.post.out.PostAttrRepository
import com.back.boundedContexts.post.out.PostCommentRepository
import com.back.boundedContexts.post.out.PostLikeRepository

lateinit var postAttrRepository: PostAttrRepository
lateinit var postCommentRepository: PostCommentRepository
lateinit var postLikeRepository: PostLikeRepository

fun isPostReposInitialized(): Boolean = ::postCommentRepository.isInitialized
