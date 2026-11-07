package com.example.kisha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.kisha.allowance.AllowanceStatus

/** Header colours for one allowance state: the full-bleed [container], text on it, and the chip. */
@Immutable
data class StatusColors(val container: Color, val content: Color, val chip: Color, val onChip: Color)

/** Kisha tokens beyond Material's colour roles. */
@Immutable
data class KishaColors(
    val onTrack: StatusColors,
    val low: StatusColors,
    val over: StatusColors,
    /** The pocket coin. */
    val coin: Color,
    val onCoin: Color,
) {
    fun status(status: AllowanceStatus): StatusColors = when (status) {
        AllowanceStatus.ON_TRACK -> onTrack
        AllowanceStatus.LOW -> low
        AllowanceStatus.OVER -> over
    }
}

private val LocalKishaColors = staticCompositionLocalOf { KishaLightExtras }

/** Kisha's own tokens, read like MaterialTheme: `KishaTheme.colors`, `KishaTheme.heroNumber`. */
object KishaTheme {
    val colors: KishaColors
        @Composable get() = LocalKishaColors.current

    /** The giant number on Home (and the setup preview). */
    val heroNumber = TextStyle(fontSize = 96.sp, lineHeight = 96.sp, fontWeight = FontWeight.Black, letterSpacing = (-3).sp)

    /** Shrinks the hero number to fit its width (long negatives, 200% font scale). */
    val heroAutoSize = TextAutoSize.StepBased(minFontSize = 36.sp, maxFontSize = 96.sp, stepSize = 2.sp)

    /** Big figures on the sheet under the header. */
    val figure = TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
}

/** UI-1: Material 3 Expressive with the expressive motion scheme and Kisha's fixed palette. */
@Composable
fun KishaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalKishaColors provides if (dark) KishaDarkExtras else KishaLightExtras) {
        MaterialExpressiveTheme(
            colorScheme = if (dark) KishaDarkScheme else KishaLightScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography(),
            content = content,
        )
    }
}
