package com.kanso.focus.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val PureBlack = Color(0xFF07080C)
val SurfaceZinc = Color(0xFF12141C)
val SurfaceElevated = Color(0xFF1A1D28)
val BorderZinc = Color(0xFF282D3E)
val PureWhite = Color(0xFFF8FAFC)
val MutedGray = Color(0xFF94A3B8)

val AccentIndigo = Color(0xFF6366F1)
val AccentViolet = Color(0xFF8B5CF6)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentCoral = Color(0xFFEF4444)
val AccentCyan = Color(0xFF06B6D4)

private val KansoColorScheme = darkColorScheme(
    primary = AccentIndigo,
    onPrimary = PureWhite,
    secondary = AccentEmerald,
    onSecondary = PureBlack,
    tertiary = AccentViolet,
    background = PureBlack,
    onBackground = PureWhite,
    surface = SurfaceZinc,
    onSurface = PureWhite,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = MutedGray,
    outline = BorderZinc
)

@Composable
fun KansoMinimalTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = PureBlack.toArgb()
                window.navigationBarColor = PureBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = KansoColorScheme,
        content = content
    )
}
