package gg.slog.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SlogUrls {
    // 디버그 빌드에서 로컬 백엔드를 보도록 런타임에 바꿀 수 있게 둔다
    var API_BASE: String = "https://api.slog.gg"
    var WEB_BASE: String = "https://www.slog.gg"

    /** 글 본문을 웹뷰로 띄울 때 쓰는 주소 */
    fun postPage(postId: Int) = "$WEB_BASE/p/$postId"

    /** 네이티브 카카오 로그인을 못 쓰는 플랫폼(Web)용 리다이렉트 로그인 */
    fun kakaoRedirectLogin(returnTo: String) =
        "$API_BASE/oauth2/authorization/kakao?redirectUrl=$returnTo"
}

@Serializable
private data class KakaoLoginReq(val accessToken: String)

class SlogApi(
    private val client: HttpClient = defaultClient(),
    private val baseUrl: String = SlogUrls.API_BASE,
) {
    suspend fun posts(page: Int = 1, pageSize: Int = 20, kw: String = ""): PostPageDto =
        client.get("$baseUrl/post/api/v1/posts") {
            parameter("page", page)
            parameter("pageSize", pageSize)
            parameter("kw", kw)
            parameter("sort", "CREATED_AT")
        }.body()

    suspend fun me(): RsData<MemberDto> =
        client.get("$baseUrl/member/api/v1/auth/me").body()

    /**
     * 네이티브 카카오 로그인.
     * 카카오 SDK 로 받은 액세스 토큰을 백엔드에 넘겨 slog 세션으로 교환한다.
     */
    suspend fun loginWithKakao(kakaoAccessToken: String): RsData<LoginResult> =
        client.post("$baseUrl/member/api/v1/auth/social/kakao") {
            contentType(ContentType.Application.Json)
            setBody(KakaoLoginReq(kakaoAccessToken))
        }.body()

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }
}
