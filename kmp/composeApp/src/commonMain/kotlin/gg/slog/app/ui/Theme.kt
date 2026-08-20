package gg.slog.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import slogapp.composeapp.generated.resources.Res
import slogapp.composeapp.generated.resources.pretendard_bold
import slogapp.composeapp.generated.resources.pretendard_regular

/**
 * Compose for Web 은 캔버스에 직접 글자를 그리므로 시스템 폰트를 쓰지 않는다.
 * 한글 글리프가 있는 폰트를 직접 넣지 않으면 전부 두부(□)로 보인다.
 */
@Composable
fun slogFontFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
)

@Composable
fun SlogTheme(content: @Composable () -> Unit) {
    val family = slogFontFamily()
    val d = Typography()
    val typography = Typography(
        displayLarge = d.displayLarge.copy(fontFamily = family),
        displayMedium = d.displayMedium.copy(fontFamily = family),
        displaySmall = d.displaySmall.copy(fontFamily = family),
        headlineLarge = d.headlineLarge.copy(fontFamily = family),
        headlineMedium = d.headlineMedium.copy(fontFamily = family),
        headlineSmall = d.headlineSmall.copy(fontFamily = family),
        titleLarge = d.titleLarge.copy(fontFamily = family),
        titleMedium = d.titleMedium.copy(fontFamily = family),
        titleSmall = d.titleSmall.copy(fontFamily = family),
        bodyLarge = d.bodyLarge.copy(fontFamily = family),
        bodyMedium = d.bodyMedium.copy(fontFamily = family),
        bodySmall = d.bodySmall.copy(fontFamily = family),
        labelLarge = d.labelLarge.copy(fontFamily = family),
        labelMedium = d.labelMedium.copy(fontFamily = family),
        labelSmall = d.labelSmall.copy(fontFamily = family),
    )
    MaterialTheme(typography = typography, content = content)
}
