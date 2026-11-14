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

val PureBlack = Color(0xFF000000)
val SurfaceZinc = Color(0xFF0F0F12)
val BorderZinc = Color(0xFF222226)
val PureWhite = Color(0xFFF4F4F6)
val MutedGray = Color(0xFF71717A)

private val KansoColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = MutedGray,
    onSecondary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = SurfaceZinc,
    onSurface = PureWhite,
    surfaceVariant = SurfaceZinc,
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
