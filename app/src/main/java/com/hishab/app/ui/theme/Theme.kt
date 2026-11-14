@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hishab.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hishab.app.domain.ThemeMode

data class HishabExtraColors(
    val surface2: Color, val muted: Color, val border: Color, val primarySoft: Color,
    val gold: Color, val goldSoft: Color, val goldInk: Color,
    val rose: Color, val roseSoft: Color, val blue: Color, val blueSoft: Color
)
val LocalExtraColors = staticCompositionLocalOf {
    HishabExtraColors(Surface2Light, MutedLight, BorderLight, PrimarySoftLight, GoldLight, GoldSoftLight, GoldInkLight, RoseLight, RoseSoftLight, BlueLight, BlueSoftLight)
}
object HishabTheme { val colors: HishabExtraColors @Composable get() = LocalExtraColors.current }

private val LightScheme = lightColorScheme(
    primary = PrimaryLight, onPrimary = OnPrimaryLight, primaryContainer = PrimarySoftLight, onPrimaryContainer = InkLight,
    background = PaperLight, onBackground = InkLight, surface = SurfaceLight, onSurface = InkLight,
    surfaceVariant = Surface2Light, onSurfaceVariant = MutedLight, outline = BorderLight, outlineVariant = BorderLight,
    error = RoseLight, onError = Color.White, errorContainer = RoseSoftLight, onErrorContainer = RoseLight
)
private val DarkScheme = darkColorScheme(
    primary = PrimaryDark, onPrimary = OnPrimaryDark, primaryContainer = PrimarySoftDark, onPrimaryContainer = InkDark,
    background = PaperDark, onBackground = InkDark, surface = SurfaceDark, onSurface = InkDark,
    surfaceVariant = Surface2Dark, onSurfaceVariant = MutedDark, outline = BorderDark, outlineVariant = BorderDark,
    error = RoseDark, onError = Color(0xFF3E1515), errorContainer = RoseSoftDark, onErrorContainer = RoseDark
)

@Composable
fun HishabTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.LIGHT -> false; ThemeMode.DARK -> true; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val extra = if (dark) HishabExtraColors(Surface2Dark, MutedDark, BorderDark, PrimarySoftDark, GoldDark, GoldSoftDark, GoldInkDark, RoseDark, RoseSoftDark, BlueDark, BlueSoftDark)
    else HishabExtraColors(Surface2Light, MutedLight, BorderLight, PrimarySoftLight, GoldLight, GoldSoftLight, GoldInkLight, RoseLight, RoseSoftLight, BlueLight, BlueSoftLight)
    androidx.compose.runtime.CompositionLocalProvider(LocalExtraColors provides extra) {
        MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = HishabTypography, content = content)
    }
}
