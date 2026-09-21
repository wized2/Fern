package com.endroid.fern.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.endroid.fern.data.ThemeMode

/** Soft leaf light — high surface contrast, calm greens. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B7A3D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F0C8),
    onPrimaryContainer = Color(0xFF003919),
    secondary = Color(0xFF4A6354),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8D6),
    onSecondaryContainer = Color(0xFF072116),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBFE9F4),
    onTertiaryContainer = Color(0xFF001F27),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF161D17),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF161D17),
    surfaceVariant = Color(0xFFD9E6DB),
    onSurfaceVariant = Color(0xFF3E4A40),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F6EE),
    surfaceContainer = Color(0xFFEAF1E8),
    surfaceContainerHigh = Color(0xFFE4ECE2),
    surfaceContainerHighest = Color(0xFFDEE6DC),
    outline = Color(0xFF6E7A70),
    outlineVariant = Color(0xFFBDCABF)
)

/** Deep forest dark — OLED-friendly near-black with mint accents. */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD9A5),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00522A),
    onPrimaryContainer = Color(0xFFA8F0BD),
    secondary = Color(0xFFB1CCBA),
    onSecondary = Color(0xFF1D3529),
    secondaryContainer = Color(0xFF334C3E),
    onSecondaryContainer = Color(0xFFCDE9D5),
    tertiary = Color(0xFFA3CDD8),
    onTertiary = Color(0xFF04363F),
    tertiaryContainer = Color(0xFF214C56),
    onTertiaryContainer = Color(0xFFBFE9F4),
    background = Color(0xFF080B09),
    onBackground = Color(0xFFE2EAE3),
    surface = Color(0xFF080B09),
    onSurface = Color(0xFFDEE5DB),
    surfaceVariant = Color(0xFF3E4A40),
    onSurfaceVariant = Color(0xFFBDCABF),
    surfaceContainerLowest = Color(0xFF060906),
    surfaceContainerLow = Color(0xFF131A14),
    surfaceContainer = Color(0xFF171E18),
    surfaceContainerHigh = Color(0xFF212822),
    surfaceContainerHighest = Color(0xFF2C332D),
    outline = Color(0xFF88948A),
    outlineVariant = Color(0xFF3E4A40)
)

private val FernTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun FernTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = FernTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(22.dp)
        ),
        content = content
    )
}
