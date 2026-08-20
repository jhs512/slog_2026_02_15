package gg.slog.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

actual val platformName: String = "iOS"

@Composable
actual fun DocumentWebView(url: String, modifier: Modifier) {
    UIKitView(
        modifier = modifier,
        factory = { WKWebView(frame = platform.CoreGraphics.CGRectZero.readValue(), configuration = WKWebViewConfiguration()) },
        update = { webView ->
            NSURL.URLWithString(url)?.let { webView.loadRequest(NSURLRequest(it)) }
        },
    )
}

/**
 * TODO(카카오): 카카오 iOS SDK(KakaoSDKUser) 연동.
 *   CocoaPods 로 KakaoSDKUser 를 붙이고 UserApi.shared.loginWithKakaoTalk 결과를 넘긴다.
 */
actual suspend fun kakaoLogin(): KakaoLoginResult =
    KakaoLoginResult.Failed("카카오 SDK 미연동 (네이티브 앱 키 등록 필요)")
