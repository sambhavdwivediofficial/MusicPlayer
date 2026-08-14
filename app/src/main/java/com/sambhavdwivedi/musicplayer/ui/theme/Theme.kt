package com.sambhavdwivedi.musicplayer.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
