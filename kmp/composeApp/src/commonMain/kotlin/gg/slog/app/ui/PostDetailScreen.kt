package gg.slog.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import gg.slog.app.data.PostDto
import gg.slog.app.data.SlogUrls
import gg.slog.app.platform.DocumentWebView

/** 목록·헤더는 Compose, 본문은 웹뷰 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(post: PostDto, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(post.title, maxLines = 1) },
            navigationIcon = { IconButton(onClick = onBack) { Text("←") } },
        )
        DocumentWebView(
            url = SlogUrls.postPage(post.id),
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}
