package com.shortsblocker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.shortsblocker.app.data.AppThemeMode

private val LightColors =
    lightColorScheme(
        primary = IndigoPrimaryLight,
        onPrimary = IndigoOnPrimaryLight,
        primaryContainer = IndigoContainerLight,
        onPrimaryContainer = IndigoOnContainerLight,
        secondary = TealSecondaryLight,
        onSecondary = TealOnSecondaryLight,
        secondaryContainer = TealContainerLight,
        onSecondaryContainer = TealOnContainerLight,
        background = SlateBackgroundLight,
        onBackground = SlateOnBackgroundLight,
        surface = SlateSurfaceLight,
        onSurface = SlateOnSurfaceLight,
        surfaceVariant = SlateSurfaceVariantLight,
        onSurfaceVariant = SlateOnSurfaceVariantLight,
        outline = SlateOutlineLight,
        outlineVariant = SlateOutlineVariantLight,
        error = RoseErrorLight,
        onError = RoseOnErrorLight,
        errorContainer = RoseErrorContainerLight,
        onErrorContainer = RoseOnErrorContainerLight,
    )

private val DarkColors =
    darkColorScheme(
        primary = IndigoPrimaryDark,
        onPrimary = IndigoOnPrimaryDark,
        primaryContainer = IndigoContainerDark,
        onPrimaryContainer = IndigoOnContainerDark,
        secondary = TealSecondaryDark,
        onSecondary = TealOnSecondaryDark,
        secondaryContainer = TealContainerDark,
        onSecondaryContainer = TealOnContainerDark,
        background = SlateBackgroundDark,
        onBackground = SlateOnBackgroundDark,
        surface = SlateSurfaceDark,
        onSurface = SlateOnSurfaceDark,
        surfaceVariant = SlateSurfaceVariantDark,
        onSurfaceVariant = SlateOnSurfaceVariantDark,
        outline = SlateOutlineDark,
        outlineVariant = SlateOutlineVariantDark,
        error = RoseErrorDark,
        onError = RoseOnErrorDark,
        errorContainer = RoseErrorContainerDark,
        onErrorContainer = RoseOnErrorContainerDark,
    )

@Composable
fun ShortsBlockerTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark =
        when (themeMode) {
            AppThemeMode.SYSTEM -> systemInDark
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }

    val colors = if (isDark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
