package com.carijajan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palet Warna Identitas Logo Cari Jajan ────────────────────────────────────
// Marun pekat khas "CARQ" / "CARI" & outline kaca pembesar
val BrandMaroon = Color(0xFF500F11)
val BrandMaroonDark = Color(0xFF38080A)
val BrandMaroonLight = Color(0xFF751C20)
val BrandMaroonContainer = Color(0xFFF9EAEB)

// Oranye hangat & menggugah selera khas "JAJAN" & isi kaca pembesar
val BrandOrange = Color(0xFFEA681E)
val BrandOrangeDark = Color(0xFFC74F0C)
val BrandOrangeLight = Color(0xFFFFEDD5)
val BrandOrangeSurface = Color(0xFFFFF7ED)

// Status & Aksen
val BrandSuccess = Color(0xFF16A34A)
val BrandSuccessLight = Color(0xFFDCFCE7)
val BrandError = Color(0xFFDC2626)
val BrandErrorLight = Color(0xFFFEE2E2)
val BrandWarning = Color(0xFFD97706)
val BrandWarningLight = Color(0xFFFEF3C7)

// Neutral Hangat (Warm Minimalist)
val WarmBg = Color(0xFFFAF7F5)
val WarmSurface = Color(0xFFFFFFFF)
val WarmBorder = Color(0xFFF0E5DF)
val WarmText = Color(0xFF1C1917)
val WarmTextSecondary = Color(0xFF78716C)
val WarmTextTertiary = Color(0xFFA8A29E)

private val CariJajanColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeLight,
    onPrimaryContainer = BrandOrangeDark,
    secondary = BrandMaroon,
    onSecondary = Color.White,
    secondaryContainer = BrandMaroonContainer,
    onSecondaryContainer = BrandMaroon,
    tertiary = BrandMaroonLight,
    onTertiary = Color.White,
    background = WarmBg,
    onBackground = WarmText,
    surface = WarmSurface,
    onSurface = WarmText,
    surfaceVariant = BrandOrangeSurface,
    onSurfaceVariant = WarmTextSecondary,
    outline = WarmBorder,
    error = BrandError,
    onError = Color.White,
    errorContainer = BrandErrorLight,
    onErrorContainer = BrandError
)

@Composable
fun CariJajanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CariJajanColors,
        content = content
    )
}

