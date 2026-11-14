package com.example.oeimdapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

@Immutable
data class OeiExtendedColors(
    val isDark: Boolean,
    val appBackground: Color,
    val elevatedSurface: Color,
    val subtleSurface: Color,
    val subtleSurfaceAlt: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primaryAction: Color,
    val onPrimaryAction: Color,
    // Kartu Habis (0)
    val habisBorder: Color,
    val habisPhotoBg: Color,
    val habisCardBg: Color,
    val habisBadgeBg: Color,
    val habisBadgeText: Color,
    val habisStepperBtnBg: Color,
    val habisStepperBtnText: Color,
    val habisCounterBg: Color,
    val habisCounterText: Color,
    // Kartu Kurang (0 < x < T)
    val kurangBorder: Color,
    val kurangPhotoBg: Color,
    val kurangCardBg: Color,
    val kurangBadgeBg: Color,
    val kurangBadgeText: Color,
    val kurangStepperBtnBg: Color,
    val kurangStepperBtnText: Color,
    val kurangCounterBg: Color,
    val kurangCounterText: Color,
    // Kartu Penuh (x >= T)
    val penuhBorder: Color,
    val penuhPhotoBg: Color,
    val penuhCardBg: Color,
    val penuhBadgeBg: Color,
    val penuhBadgeText: Color,
    val penuhStepperBtnBg: Color,
    val penuhStepperBtnText: Color,
    val penuhCounterBg: Color,
    val penuhCounterText: Color,
    // Kartu ALIAS-000 & Lencana Best
    val zeroCardBorder: Color,
    val zeroPlaceholderBg: Color,
    val bestBadgeBg: Color,
    val bestBadgeText: Color
)

private val LightExtendedColors = OeiExtendedColors(
    isDark = false,
    appBackground = LightAppBackground,
    elevatedSurface = LightElevatedSurface,
    subtleSurface = LightSubtleSurface,
    subtleSurfaceAlt = LightSubtleSurfaceAlt,
    border = LightBorder,
    borderStrong = LightBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    primaryAction = LightPrimaryAction,
    onPrimaryAction = LightOnPrimaryAction,
    habisBorder = LightHabisBorder,
    habisPhotoBg = LightHabisPhotoBg,
    habisCardBg = LightHabisCardBg,
    habisBadgeBg = LightHabisBadgeBg,
    habisBadgeText = LightHabisBadgeText,
    habisStepperBtnBg = LightHabisStepperBtnBg,
    habisStepperBtnText = LightHabisStepperBtnText,
    habisCounterBg = LightHabisCounterBg,
    habisCounterText = LightHabisCounterText,
    kurangBorder = LightKurangBorder,
    kurangPhotoBg = LightKurangPhotoBg,
    kurangCardBg = LightKurangCardBg,
    kurangBadgeBg = LightKurangBadgeBg,
    kurangBadgeText = LightKurangBadgeText,
    kurangStepperBtnBg = LightKurangStepperBtnBg,
    kurangStepperBtnText = LightKurangStepperBtnText,
    kurangCounterBg = LightKurangCounterBg,
    kurangCounterText = LightKurangCounterText,
    penuhBorder = LightPenuhBorder,
    penuhPhotoBg = LightPenuhPhotoBg,
    penuhCardBg = LightPenuhCardBg,
    penuhBadgeBg = LightPenuhBadgeBg,
    penuhBadgeText = LightPenuhBadgeText,
    penuhStepperBtnBg = LightPenuhStepperBtnBg,
    penuhStepperBtnText = LightPenuhStepperBtnText,
    penuhCounterBg = LightPenuhCounterBg,
    penuhCounterText = LightPenuhCounterText,
    zeroCardBorder = LightZeroCardBorder,
    zeroPlaceholderBg = LightZeroPlaceholderBg,
    bestBadgeBg = BestBadgeBg,
    bestBadgeText = BestBadgeText
)

private val DarkExtendedColors = OeiExtendedColors(
    isDark = true,
    appBackground = DarkAppBackground,
    elevatedSurface = DarkElevatedSurface,
    subtleSurface = DarkSubtleSurface,
    subtleSurfaceAlt = DarkSubtleSurfaceAlt,
    border = DarkBorder,
    borderStrong = DarkBorderStrong,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    primaryAction = DarkPrimaryAction,
    onPrimaryAction = DarkOnPrimaryAction,
    habisBorder = DarkHabisBorder,
    habisPhotoBg = DarkHabisCardBg,
    habisCardBg = DarkHabisCardBg,
    habisBadgeBg = DarkHabisBadgeBg,
    habisBadgeText = DarkHabisBadgeText,
    habisStepperBtnBg = DarkHabisStepperBtnBg,
    habisStepperBtnText = DarkHabisStepperBtnText,
    habisCounterBg = DarkHabisCounterBg,
    habisCounterText = DarkHabisCounterText,
    kurangBorder = DarkKurangBorder,
    kurangPhotoBg = DarkKurangCardBg,
    kurangCardBg = DarkKurangCardBg,
    kurangBadgeBg = DarkKurangBadgeBg,
    kurangBadgeText = DarkKurangBadgeText,
    kurangStepperBtnBg = DarkKurangStepperBtnBg,
    kurangStepperBtnText = DarkKurangStepperBtnText,
    kurangCounterBg = DarkKurangCounterBg,
    kurangCounterText = DarkKurangCounterText,
    penuhBorder = DarkPenuhBorder,
    penuhPhotoBg = DarkPenuhCardBg,
    penuhCardBg = DarkPenuhCardBg,
    penuhBadgeBg = DarkPenuhBadgeBg,
    penuhBadgeText = DarkPenuhBadgeText,
    penuhStepperBtnBg = DarkPenuhStepperBtnBg,
    penuhStepperBtnText = DarkPenuhStepperBtnText,
    penuhCounterBg = DarkPenuhCounterBg,
    penuhCounterText = DarkPenuhCounterText,
    zeroCardBorder = DarkZeroCardBorder,
    zeroPlaceholderBg = DarkZeroPlaceholderBg,
    bestBadgeBg = BestBadgeBg,
    bestBadgeText = BestBadgeText
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = LightPrimaryAction,
    onPrimary = LightOnPrimaryAction,
    background = LightAppBackground,
    onBackground = LightTextPrimary,
    surface = LightElevatedSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSubtleSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = LightHabisBorder
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkOnPrimaryAction,
    background = DarkAppBackground,
    onBackground = DarkTextPrimary,
    surface = DarkElevatedSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSubtleSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderStrong,
    error = DarkHabisBorder
)

val LocalOeiColors = staticCompositionLocalOf { LightExtendedColors }

object OeiTheme {
    val colors: OeiExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOeiColors.current
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun OeiAppTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val materialColors = if (darkTheme) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalOeiColors provides extendedColors,
        LocalDensity provides Density(density = currentDensity.density, fontScale = 1.0f)
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = OeiTypography,
            content = content
        )
    }
}