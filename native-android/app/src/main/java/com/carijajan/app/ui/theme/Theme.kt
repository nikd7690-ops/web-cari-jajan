package com.carijajan.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Emerald = Color(0xFF059669)
private val EmeraldDark = Color(0xFF047857)
private val EmeraldLight = Color(0xFFD1FAE5)

private val CariJajanColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldDark,
    onSecondary = Color.White,
    background = Color(0xFFF4F4F5),
    onBackground = Color(0xFF18181B),
    surface = Color.White,
    onSurface = Color(0xFF18181B)
)

@Composable
fun CariJajanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CariJajanColors,
        content = content
    )
}
