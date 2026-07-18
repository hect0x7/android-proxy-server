package com.hect0x7.proxy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Teal = Color(0xFF008577)
private val TealBright = Color(0xFF39BFAE)
private val DarkColors = darkColorScheme(
    primary = TealBright,
    onPrimary = Color(0xFF002F2A),
    primaryContainer = Color(0xFF00695C),
    onPrimaryContainer = Color.White,
    background = Color(0xFF15191A),
    onBackground = Color(0xFFE5E9E8),
    surface = Color(0xFF1D2223),
    onSurface = Color(0xFFE5E9E8),
    surfaceVariant = Color(0xFF272D2E),
    onSurfaceVariant = Color(0xFFB8C1BF),
    outline = Color(0xFF485251),
)

private val ProxyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F5EF),
    onPrimaryContainer = Color(0xFF003731),
    background = Color(0xFFF7F9F9),
    onBackground = Color(0xFF1B1F1F),
    surface = Color.White,
    onSurface = Color(0xFF1B1F1F),
    surfaceVariant = Color(0xFFE8EEEC),
    onSurfaceVariant = Color(0xFF4B5553),
    outline = Color(0xFF7B8684),
)

@Composable
fun ProxyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = ProxyShapes,
        content = content,
    )
}
