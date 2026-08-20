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

actual suspend fun kakaoLogin(): KakaoLoginResult = kakaoLoginAndroid()
