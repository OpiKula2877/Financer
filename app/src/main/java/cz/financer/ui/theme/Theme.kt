package cz.financer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3F5),
    onPrimaryContainer = Color(0xFF0B1F38),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCEFEA),
    onSecondaryContainer = Color(0xFF052E2A),
    tertiary = Color(0xFFB7791F),
    background = Color(0xFFF6F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EAF0),
    outline = Color(0xFF8A94A6),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7EE),
    onPrimary = Color(0xFF0B1F38),
    primaryContainer = Color(0xFF244769),
    onPrimaryContainer = Color(0xFFD6E3F5),
    secondary = Color(0xFF7FD6CB),
    onSecondary = Color(0xFF052E2A),
    secondaryContainer = Color(0xFF0E4F49),
    onSecondaryContainer = Color(0xFFCCEFEA),
    tertiary = Color(0xFFE7B769),
    background = Color(0xFF0F1720),
    surface = Color(0xFF162230),
    surfaceVariant = Color(0xFF223246),
    outline = Color(0xFF7D8AA0),
    error = Color(0xFFF2B8B5),
)

@Composable
fun FinancerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
