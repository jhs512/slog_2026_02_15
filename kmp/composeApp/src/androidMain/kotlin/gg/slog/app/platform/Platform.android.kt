package gg.slog.app.platform

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

actual val platformName: String = "Android"

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun DocumentWebView(url: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
            }
        },
        update = { it.loadUrl(url) },
    )
}

/**
 * TODO(카카오): 카카오 SDK(com.kakao.sdk:v2-user) 연동.
 *   UserApiClient.instance.loginWithKakaoTalk / loginWithKakaoAccount 로 토큰을 받아
 *   KakaoLoginResult.Success(token) 로 돌려준다.
 *   SDK 를 붙이려면 네이티브 앱 키와 리다이렉트 스킴 등록이 필요하다.
 */
actual suspend fun kakaoLogin(): KakaoLoginResult =
    KakaoLoginResult.Failed("카카오 SDK 미연동 (네이티브 앱 키 등록 필요)")
