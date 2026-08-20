package gg.slog.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gg.slog.app.data.PostDto

@Composable
fun PostListScreen(
    posts: List<PostDto>,
    loading: Boolean,
    error: String?,
    onSelect: (PostDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            error != null -> Text(
                text = error,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            posts.isEmpty() -> Text(
                text = "글이 없습니다.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(posts, key = { it.id }) { post ->
                    PostRow(post) { onSelect(post) }
                }
            }
        }
    }
}

@Composable
private fun PostRow(post: PostDto, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(post.title, style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(post.authorName, style = MaterialTheme.typography.bodySmall)
                Text("조회 ${post.hitCount}", style = MaterialTheme.typography.bodySmall)
                Text("댓글 ${post.commentsCount}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
