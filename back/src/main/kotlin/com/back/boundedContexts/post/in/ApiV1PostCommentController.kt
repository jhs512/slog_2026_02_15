package com.back.boundedContexts.post.`in`

import com.back.boundedContexts.post.app.PostFacade
import com.back.boundedContexts.post.domain.postExtensions.checkActorCanDelete
import com.back.boundedContexts.post.domain.postExtensions.checkActorCanModify
import com.back.boundedContexts.post.domain.postExtensions.checkActorCanRead
import com.back.boundedContexts.post.domain.postExtensions.findCommentById
import com.back.boundedContexts.post.domain.postExtensions.getCheckActorCanDeleteRs
import com.back.boundedContexts.post.domain.postExtensions.getCheckActorCanModifyRs
import com.back.boundedContexts.post.domain.postExtensions.getComments
import com.back.boundedContexts.post.dto.PostCommentDto
import com.back.global.dto.RsData
import com.back.global.web.app.Rq
import com.back.standard.extensions.getOrThrow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.validation.annotation.Validated
import jakarta.validation.constraints.Size
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("/post/api/v1/posts/{postId}/comments")
@Tag(name = "ApiV1PostCommentController", description = "API 댓글 컨트롤러")
@SecurityRequirement(name = "bearerAuth")
class ApiV1PostCommentController(
    private val postFacade: PostFacade,
    private val rq: Rq
) {
    private val actor
        get() = rq.actor

    private fun makePostCommentDto(postComment: com.back.boundedContexts.post.domain.PostComment): PostCommentDto {
        val actor = rq.actorOrNull
        return PostCommentDto(postComment).apply {
            actorCanModify = postComment.getCheckActorCanModifyRs(actor).isSuccess
            actorCanDelete = postComment.getCheckActorCanDeleteRs(actor).isSuccess
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "다건 조회")
    fun getItems(
        @PathVariable @Positive postId: Int
    ): List<PostCommentDto> {
        val post = postFacade.findById(postId).getOrThrow()
        post.checkActorCanRead(rq.actorOrNull)

        return postFacade
            .getComments(post)
            .map { makePostCommentDto(it) }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "단건 조회")
    fun getItem(
        @PathVariable @Positive postId: Int,
        @PathVariable @Positive id: Int
    ): PostCommentDto {
        val post = postFacade.findById(postId).getOrThrow()
        post.checkActorCanRead(rq.actorOrNull)

        val postComment = postFacade.findCommentById(post, id).getOrThrow()

        return makePostCommentDto(postComment)
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "삭제")
    fun delete(
        @PathVariable @Positive postId: Int,
        @PathVariable @Positive id: Int
    ): RsData<Void> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = postFacade.findCommentById(post, id).getOrThrow()

        val actor = rq.actor
        postComment.checkActorCanDelete(actor)

        postFacade.deleteComment(post, postComment, actor)

        return RsData(
            "200-1",
            "${id}번 댓글이 삭제되었습니다."
        )
    }

    data class PostCommentModifyRequest(
        @field:NotBlank
        @field:Size(min = 2, max = 100)
        val content: String
    )

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "수정")
    fun modify(
        @PathVariable @Positive postId: Int,
        @PathVariable @Positive id: Int,
        @Valid @RequestBody reqBody: PostCommentModifyRequest
    ): RsData<Void> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = postFacade.findCommentById(post, id).getOrThrow()

        val actor = rq.actor
        postComment.checkActorCanModify(actor)

        postFacade.modifyComment(postComment, actor, reqBody.content)

        return RsData(
            "200-1",
            "${id}번 댓글이 수정되었습니다."
        )
    }

    data class PostCommentWriteRequest(
        @field:NotBlank
        @field:Size(min = 2, max = 100)
        val content: String
    )

    @PostMapping
    @Transactional
    @Operation(summary = "작성")
    fun write(
        @PathVariable @Positive postId: Int,
        @Valid @RequestBody reqBody: PostCommentWriteRequest
    ): RsData<PostCommentDto> {
        val post = postFacade.findById(postId).getOrThrow()

        val postComment = postFacade.writeComment(actor, post, reqBody.content)

        return RsData(
            "201-1",
            "${postComment.id}번 댓글이 작성되었습니다.",
            makePostCommentDto(postComment)
        )
    }
}
