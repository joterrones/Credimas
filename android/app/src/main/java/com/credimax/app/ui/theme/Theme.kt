package com.credimax.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = TealDeep,
    onPrimary = Color.White,
    primaryContainer = TealSoft,
    onPrimaryContainer = TealDark,
    secondary = AmberAlert,
    onSecondary = Color.White,
    secondaryContainer = AmberSoft,
    onSecondaryContainer = Color(0xFF78350F),
    background = SlateBg,
    onBackground = SlateText,
    surface = SlateCard,
    onSurface = SlateText,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateMuted,
    error = Danger,
    errorContainer = DangerSoft,
    outline = Color(0xFFCBD5E1),
)

@Composable
fun CredimaxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content,
    )
}
