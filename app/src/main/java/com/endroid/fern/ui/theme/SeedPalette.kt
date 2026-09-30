package com.endroid.fern.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Default Fern leaf green seed. */
val DefaultSeedColor = Color(0xFF1B7A3D)

/**
 * Builds a full Material 3 tonal palette from a single seed color.
 * Lightweight (no extra deps): HSV hue + relative luminance roles.
 */
fun colorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val (h, s, v) = seed.toHsv()
    fun tone(hh: Float, ss: Float, vv: Float) = hsv(hh, ss.coerceIn(0f, 1f), vv.coerceIn(0f, 1f))
    val secondaryHue = (h + 28f) % 360f
    val tertiaryHue = (h + 160f) % 360f

    return if (!dark) {
        val primary = tone(h, (s * 0.92f).coerceAtLeast(0.35f), (v * 0.72f).coerceIn(0.35f, 0.72f))
        val onPrimary = if (relativeLuminance(primary) > 0.45f) Color(0xFF111411) else Color.White
        val primaryContainer = tone(h, (s * 0.45f).coerceIn(0.15f, 0.55f), 0.90f)
        val onPrimaryContainer = tone(h, 0.55f, 0.18f)
        val secondary = tone(secondaryHue, 0.22f, 0.38f)
        val secondaryContainer = tone(secondaryHue, 0.20f, 0.90f)
        val tertiary = tone(tertiaryHue, 0.28f, 0.40f)
        val tertiaryContainer = tone(tertiaryHue, 0.28f, 0.90f)
        val bg = tone(h, 0.08f, 0.97f)
        val surface = bg
        val surfaceVariant = tone(h, 0.10f, 0.88f)
        val onSurface = Color(0xFF161D17)
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = tone(secondaryHue, 0.35f, 0.16f),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tone(tertiaryHue, 0.40f, 0.14f),
            background = bg,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = tone(h, 0.12f, 0.32f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = tone(h, 0.06f, 0.95f),
            surfaceContainer = tone(h, 0.07f, 0.92f),
            surfaceContainerHigh = tone(h, 0.08f, 0.89f),
            surfaceContainerHighest = tone(h, 0.09f, 0.86f),
            outline = tone(h, 0.08f, 0.48f),
            outlineVariant = tone(h, 0.08f, 0.75f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    } else {
        val primary = tone(h, (s * 0.55f).coerceIn(0.25f, 0.65f), 0.80f)
        val onPrimary = tone(h, 0.50f, 0.14f)
        val primaryContainer = tone(h, (s * 0.55f).coerceIn(0.25f, 0.70f), 0.28f)
        val onPrimaryContainer = tone(h, 0.40f, 0.88f)
        val secondary = tone(secondaryHue, 0.22f, 0.78f)
        val secondaryContainer = tone(secondaryHue, 0.22f, 0.28f)
        val tertiary = tone(tertiaryHue, 0.28f, 0.78f)
        val tertiaryContainer = tone(tertiaryHue, 0.28f, 0.28f)
        val bg = Color(0xFF080B09)
        val surface = Color(0xFF080B09)
        val onSurface = Color(0xFFE2EAE3)
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = tone(secondaryHue, 0.25f, 0.16f),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = tone(secondaryHue, 0.20f, 0.88f),
            tertiary = tertiary,
            onTertiary = tone(tertiaryHue, 0.30f, 0.14f),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = tone(tertiaryHue, 0.25f, 0.90f),
            background = bg,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = tone(h, 0.10f, 0.22f),
            onSurfaceVariant = tone(h, 0.08f, 0.75f),
            surfaceContainerLowest = Color(0xFF050705),
            surfaceContainerLow = Color(0xFF101512),
            surfaceContainer = Color(0xFF151A17),
            surfaceContainerHigh = Color(0xFF1A201C),
            surfaceContainerHighest = Color(0xFF202622),
            outline = tone(h, 0.08f, 0.50f),
            outlineVariant = tone(h, 0.10f, 0.30f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    }
}


private fun relativeLuminance(c: Color): Float {
    fun lin(x: Float): Float =
        if (x <= 0.04045f) x / 12.92f else ((x + 0.055f) / 1.055f).toDouble().let { Math.pow(it, 2.4) }.toFloat()
    return 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
}

fun Color.toArgbCompat(): Int = toArgb()

private fun Color.toHsv(): FloatArray {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> (60f * ((g - b) / d) + 360f) % 360f
        max == g -> (60f * ((b - r) / d) + 120f) % 360f
        else -> (60f * ((r - g) / d) + 240f) % 360f
    }
    val s = if (max == 0f) 0f else d / max
    return floatArrayOf(h, s, max)
}

private fun hsv(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m)
}

/** Preset seeds shown in the color picker. */
val PresetSeedColors = listOf(
    Color(0xFF1B7A3D), // Fern green
    Color(0xFF1565C0), // Blue
    Color(0xFF6A1B9A), // Purple
    Color(0xFFC62828), // Red
    Color(0xFFEF6C00), // Orange
    Color(0xFF00838F), // Teal
    Color(0xFFAD1457), // Pink
    Color(0xFF4527A0), // Deep purple
    Color(0xFF2E7D32), // Green
    Color(0xFF37474F), // Blue grey
    Color(0xFF00897B), // Teal 2
    Color(0xFFF9A825)  // Amber
)
