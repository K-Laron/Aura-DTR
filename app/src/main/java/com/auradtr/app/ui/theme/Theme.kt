package com.auradtr.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    secondary = DarkSecondary,
    tertiary = DarkTertiary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onPrimary = DarkOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    secondary = LightSecondary,
    tertiary = LightTertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onPrimary = LightOnPrimary
)

@Composable
fun AuraDTRTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// Reusable Apple Glassmorphism and 3D Liquid Design system
object GlassSystem {
    // Premium liquid gradient backdrop blending Obsidian Slate and Indigo
    val liquidBackgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            androidx.compose.ui.graphics.Color(0xFF060913),
            androidx.compose.ui.graphics.Color(0xFF0F172A),
            androidx.compose.ui.graphics.Color(0xFF1E1B4B),
            androidx.compose.ui.graphics.Color(0xFF0B0F19)
        )
    )

    // Frosted Apple glass background linear gradient
    fun glassBackground(isDark: Boolean = true) = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.03f)
            )
        } else {
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f)
            )
        }
    )

    // Signature light-refraction glass borders (thin highlights and drop refracts)
    fun glassBorder(isDark: Boolean = true) = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.04f),
                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.30f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)
            )
        } else {
            listOf(
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.50f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.15f),
                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                androidx.compose.ui.graphics.Color.White.copy(alpha = 0.25f)
            )
        }
    )

    // White gloss overlay representing dynamic overhead light sheen reflecting off physical glass curves
    val glossySheen = androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.02f),
            androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

// Convenient modifier extension representing modern glassmorphic panels
fun androidx.compose.ui.Modifier.glassCard(cornerRadius: Int = 16, isDark: Boolean = true): androidx.compose.ui.Modifier = this
    .background(
        brush = GlassSystem.glassBackground(isDark),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
    )
    .border(
        width = 1.dp,
        brush = GlassSystem.glassBorder(isDark),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
    )
