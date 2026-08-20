package com.back.global.security.config.oauth2.app

import com.back.global.exception.app.BusinessException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 네이티브 앱(Android/iOS)이 카카오 SDK 로 받은 액세스 토큰을 검증하고
 * 프로필을 가져온다. 웹 리다이렉트 로그인(CustomOAuth2UserService)과 달리
 * 토큰을 클라이언트가 들고 오므로 **우리 앱이 발급한 토큰인지 반드시 확인**해야 한다.
 */
@Component
class KakaoApiClient(
    @param:Value("\${custom.kakao.appId:}") private val expectedAppId: String,
) {
    private val client: RestClient = RestClient.builder()
        .baseUrl("https://kapi.kakao.com")
        .build()

    data class KakaoProfile(
        val oauthUserId: String,
        val nickname: String,
        val profileImgUrl: String?,
    )

    private data class TokenInfo(val id: Long?, val appId: Long?)

    fun fetchProfile(accessToken: String): KakaoProfile {
        verifyIssuedForThisApp(accessToken)

        @Suppress("UNCHECKED_CAST")
        val me = client.get()
            .uri("/v2/user/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body(Map::class.java) as? Map<String, Any>
            ?: throw BusinessException("401-3", "카카오 프로필을 가져오지 못했습니다.")

        val id = me["id"]?.toString()
            ?: throw BusinessException("401-3", "카카오 프로필에 id가 없습니다.")

        @Suppress("UNCHECKED_CAST")
        val props = me["properties"] as? Map<String, Any> ?: emptyMap()

        return KakaoProfile(
            oauthUserId = id,
            nickname = props["nickname"] as? String ?: "카카오사용자",
            profileImgUrl = props["profile_image"] as? String,
        )
    }

    /**
     * 다른 카카오 앱에서 발급된 토큰으로 로그인되는 것을 막는다.
     * appId 를 설정하지 않으면 검증할 수 없으므로 막는다(fail-closed).
     */
    private fun verifyIssuedForThisApp(accessToken: String) {
        if (expectedAppId.isBlank()) {
            throw BusinessException("500-1", "카카오 앱 ID가 설정되지 않아 네이티브 로그인을 처리할 수 없습니다.")
        }

        @Suppress("UNCHECKED_CAST")
        val info = runCatching {
            client.get()
                .uri("/v1/user/access_token_info")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body(Map::class.java) as? Map<String, Any>
        }.getOrNull() ?: throw BusinessException("401-2", "유효하지 않은 카카오 토큰입니다.")

        val appId = info["app_id"]?.toString()
        if (appId != expectedAppId) {
            throw BusinessException("401-2", "다른 앱에서 발급된 카카오 토큰입니다.")
        }
    }
}
