package com.shortsblocker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
fun ShortsBlockerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
