package gg.slog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gg.slog.app.data.PostDto
import gg.slog.app.data.SlogApi
import gg.slog.app.platform.KakaoLoginResult
import gg.slog.app.platform.kakaoLogin
import gg.slog.app.ui.PostDetailScreen
import gg.slog.app.ui.PostListScreen
import gg.slog.app.ui.SlogTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(api: SlogApi = remember { SlogApi() }) {
    SlogTheme {
        var posts by remember { mutableStateOf<List<PostDto>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var selected by remember { mutableStateOf<PostDto?>(null) }
        var notice by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            try {
                posts = api.posts(page = 1, pageSize = 20).content
            } catch (t: Throwable) {
                error = "글을 불러오지 못했습니다: ${t.message}"
            } finally {
                loading = false
            }
        }

        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val current = selected
            if (current != null) {
                PostDetailScreen(post = current, onBack = { selected = null })
            } else {
                Column(
                    Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    SlogHeader(
                        onLogin = {
                            scope.launch {
                                notice = when (val r = kakaoLogin()) {
                                    is KakaoLoginResult.Success -> runCatching {
                                        val res = api.loginWithKakao(r.accessToken)
                                        res.data?.item?.name?.let { "$it 님 환영합니다" } ?: res.msg
                                    }.getOrElse { "로그인 실패: ${it.message}" }
                                    KakaoLoginResult.Cancelled -> null
                                    KakaoLoginResult.HandledByRedirect -> null
                                    is KakaoLoginResult.Failed -> "로그인 실패: ${r.message}"
                                }
                            }
                        }
                    )

                    notice?.let {
                        Text(
                            it,
                            Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    PostListScreen(
                        posts = posts,
                        loading = loading,
                        error = error,
                        onSelect = { selected = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** front 의 sticky 헤더( 로고 좌측 · 액션 우측 )와 같은 구성 */
@Composable
private fun SlogHeader(onLogin: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "슬로그",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 14.dp, vertical = 6.dp
                ),
                modifier = Modifier.height(36.dp),
            ) {
                Text("카카오 로그인", style = MaterialTheme.typography.labelLarge)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background))
    }
}
