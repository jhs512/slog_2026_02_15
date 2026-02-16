package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.dto.PostSearchDto
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/post/api/v1/posts")
class PostApiController(
    private val postFacade: PostFacade,
) {
    @GetMapping
    fun search(
        @RequestParam("kw") keyword: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam("pageSize", defaultValue = "10") size: Int,
    ): Page<PostSearchDto> {
        val safePage = if (page > 1) page - 1 else 0
        val safeSize = when {
            size <= 0 -> 10
            size > 100 -> 100
            else -> size
        }

        val posts = postFacade.findByKeyword(
            keyword,
            safePage,
            safeSize
        )

        return posts.map { PostSearchDto.from(it) }
    }
}
