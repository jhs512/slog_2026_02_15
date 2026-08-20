package gg.slog.app.data

import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: Int,
    val createdAt: String,
    val modifiedAt: String,
    val authorId: Int,
    val authorName: String,
    val authorProfileImgUrl: String? = null,
    val title: String,
    val published: Boolean,
    val listed: Boolean,
    val likesCount: Int,
    val commentsCount: Int,
    val hitCount: Int,
    val actorHasLiked: Boolean,
)

@Serializable
data class PageableDto(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Int,
    val totalPages: Int,
    val numberOfElements: Int,
)

@Serializable
data class PostPageDto(
    val content: List<PostDto>,
    val pageable: PageableDto? = null,
)

/** 로그인 응답. 백엔드의 RsData 래퍼를 벗긴 형태다. */
@Serializable
data class MemberDto(
    val id: Int,
    val name: String,
    val profileImageUrl: String? = null,
    val isAdmin: Boolean = false,
)

@Serializable
data class LoginResult(
    val item: MemberDto,
    val apiKey: String,
    val accessToken: String,
)

@Serializable
data class RsData<T>(
    val resultCode: String,
    val msg: String,
    val data: T? = null,
)
