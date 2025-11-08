// File: app/src/main/java/com/example/quizmaster/ui/theme/Theme.kt
package com.example.quizmaster.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define our new light color scheme using the colors from Color.kt
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueLight,
    onPrimaryContainer = Color.Black,
    secondary = AccentTeal,
    onSecondary = Color.White,
    error = IncorrectRed,
    onError = Color.White,
    background = AppBackground,
    onBackground = Color.Black,
    surface = CardBackground,
    onSurface = Color.Black
)

@Composable
fun QuizMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // We'll ignore this for now
    content: @Composable () -> Unit
) {
    // We are forcing Light Theme for this design
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Set status bar color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // This comes from your Typography.kt file
        content = content
    )
}