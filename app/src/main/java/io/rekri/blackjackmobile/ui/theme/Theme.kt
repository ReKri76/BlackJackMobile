// SPDX-License-Identifier: GPL-3.0-only
package io.rekri.blackjackmobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    secondary = ResultWin,
    background = TableGreen,
    surface = DarkSurface,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC2C8C2),
    surfaceContainer = DarkSurfaceContainer
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAccent,
    onPrimary = Color.Black,
    background = TableGreen,
    surface = Color(0xFFFBF8F1),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun BlackJackMobileTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}