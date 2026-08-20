package gg.slog.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 플랫폼 이름 (디버깅·분기용) */
expect val platformName: String

/**
 * 글 본문을 표시하는 웹뷰.
 * 목록·네비게이션은 Compose 로 만들고 본문만 웹으로 띄운다.
 */
@Composable
expect fun DocumentWebView(url: String, modifier: Modifier = Modifier)

/** 카카오 로그인 결과 */
sealed interface KakaoLoginResult {
    data class Success(val accessToken: String) : KakaoLoginResult
    data object Cancelled : KakaoLoginResult
    data class Failed(val message: String) : KakaoLoginResult
    /** 네이티브 SDK 가 없는 플랫폼에서 웹 리다이렉트로 처리했음을 뜻한다 */
    data object HandledByRedirect : KakaoLoginResult
}

/**
 * 네이티브 카카오 로그인.
 * Android/iOS 는 카카오 SDK, Web 은 기존 OAuth 리다이렉트로 폴백한다.
 */
expect suspend fun kakaoLogin(): KakaoLoginResult
