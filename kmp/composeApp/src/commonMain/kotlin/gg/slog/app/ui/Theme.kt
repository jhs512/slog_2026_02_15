package gg.slog.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import slogapp.composeapp.generated.resources.Res
import slogapp.composeapp.generated.resources.pretendard_bold
import slogapp.composeapp.generated.resources.pretendard_regular

// front/src/app/globals.css 의 shadcn neutral 토큰을 그대로 옮긴 값.
// CSS 는 oklch 로 적혀 있어 sRGB 로 변환해 넣었다.
private object SlogColors {
    val LightBackground = Color(0xFFFFFFFF)
    val LightForeground = Color(0xFF0A0A0A)
    val LightCard = Color(0xFFFFFFFF)
    val LightPrimary = Color(0xFF171717)
    val LightPrimaryForeground = Color(0xFFFAFAFA)
    val LightMuted = Color(0xFFF5F5F5)
    val LightMutedForeground = Color(0xFF737373)
    val LightBorder = Color(0xFFE5E5E5)
    val LightDestructive = Color(0xFFE7000B)

    val DarkBackground = Color(0xFF0A0A0A)
    val DarkForeground = Color(0xFFFAFAFA)
    val DarkCard = Color(0xFF171717)
    val DarkPrimary = Color(0xFFE5E5E5)
    val DarkPrimaryForeground = Color(0xFF171717)
    val DarkMuted = Color(0xFF262626)
    val DarkMutedForeground = Color(0xFFA1A1A1)
    val DarkBorder = Color(0xFF2E2E2E)
    val DarkDestructive = Color(0xFFFF6467)
}

private val LightScheme = lightColorScheme(
    primary = SlogColors.LightPrimary,
    onPrimary = SlogColors.LightPrimaryForeground,
    background = SlogColors.LightBackground,
    onBackground = SlogColors.LightForeground,
    surface = SlogColors.LightCard,
    onSurface = SlogColors.LightForeground,
    surfaceVariant = SlogColors.LightMuted,
    onSurfaceVariant = SlogColors.LightMutedForeground,
    outline = SlogColors.LightBorder,
    outlineVariant = SlogColors.LightBorder,
    error = SlogColors.LightDestructive,
    secondaryContainer = SlogColors.LightMuted,
    onSecondaryContainer = SlogColors.LightForeground,
)

private val DarkScheme = darkColorScheme(
    primary = SlogColors.DarkPrimary,
    onPrimary = SlogColors.DarkPrimaryForeground,
    background = SlogColors.DarkBackground,
    onBackground = SlogColors.DarkForeground,
    surface = SlogColors.DarkCard,
    onSurface = SlogColors.DarkForeground,
    surfaceVariant = SlogColors.DarkMuted,
    onSurfaceVariant = SlogColors.DarkMutedForeground,
    outline = SlogColors.DarkBorder,
    outlineVariant = SlogColors.DarkBorder,
    error = SlogColors.DarkDestructive,
    secondaryContainer = SlogColors.DarkMuted,
    onSecondaryContainer = SlogColors.DarkForeground,
)

/**
 * Compose for Web 은 캔버스에 글자를 직접 그려 시스템 폰트를 쓰지 않는다.
 * 웹과 같은 Pretendard 를 번들해 세 플랫폼의 글꼴을 맞춘다.
 */
@Composable
fun slogFontFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
)

@Composable
fun SlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
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
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = typography,
        content = content,
    )
}
