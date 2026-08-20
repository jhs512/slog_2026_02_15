package gg.slog.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
        var loginNotice by remember { mutableStateOf<String?>(null) }
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

        Surface(Modifier.fillMaxSize()) {
            val current = selected
            if (current != null) {
                PostDetailScreen(post = current, onBack = { selected = null })
            } else {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(title = { Text("슬로그") })
                    Button(
                        onClick = {
                            scope.launch {
                                loginNotice = when (val r = kakaoLogin()) {
                                    is KakaoLoginResult.Success -> {
                                        val res = api.loginWithKakao(r.accessToken)
                                        res.data?.item?.name?.let { "$it 님 환영합니다" } ?: res.msg
                                    }
                                    KakaoLoginResult.Cancelled -> "로그인을 취소했습니다"
                                    KakaoLoginResult.HandledByRedirect -> null
                                    is KakaoLoginResult.Failed -> "로그인 실패: ${r.message}"
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) { Text("카카오 로그인") }

                    loginNotice?.let {
                        Text(it, Modifier.fillMaxWidth().padding(16.dp))
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
