package gg.slog.app.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import gg.slog.app.data.SlogUrls
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement

actual val platformName: String = "Web(Wasm)"

/**
 * Compose for Web 은 캔버스에 그리므로 iframe 을 컴포저블 트리 안에 넣을 수 없다.
 * 캔버스 위에 겹쳐 놓고 컴포저블 좌표에 맞춰 위치를 잡는다.
 */
@Composable
actual fun DocumentWebView(url: String, modifier: Modifier) {
    val iframe = remember { document.createElement("iframe") as HTMLIFrameElement }

    DisposableEffect(url) {
        iframe.src = url
        iframe.style.position = "fixed"
        iframe.style.border = "none"
        iframe.style.zIndex = "10"
        document.body?.appendChild(iframe)
        onDispose { iframe.remove() }
    }

    Box(
        modifier.fillMaxSize().onGloballyPositioned { coords ->
            val p = coords.positionInWindow()
            val s = coords.size
            val dpr = window.devicePixelRatio
            iframe.style.left = "${p.x / dpr}px"
            iframe.style.top = "${p.y / dpr}px"
            iframe.style.width = "${s.width / dpr}px"
            iframe.style.height = "${s.height / dpr}px"
        }
    ) {}
}

/** Web 에는 카카오 네이티브 SDK 가 없다. 기존 OAuth 리다이렉트로 넘긴다. */
actual suspend fun kakaoLogin(): KakaoLoginResult {
    window.location.href = SlogUrls.kakaoRedirectLogin(window.location.origin)
    return KakaoLoginResult.HandledByRedirect
}
