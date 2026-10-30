package com.example.skylight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val ObsidianBlack = Color(0xFF030509)
val DeepSpaceSurface = Color(0xFF0B101E)
val ElevatedCockpitSurface = Color(0xFF131B30)
val LuminousIce = Color(0xFFE8ECFF)
val MutedSlate = Color(0xFF94A3B8)
val GridLineBlue = Color(0xFF28344E)
val TelemetryCyan = Color(0xFF38BDF8)
val NebulaViolet = Color(0xFF9B7ECF)
val AltitudeAmber = Color(0xFFFF8A3D)
val AltitudeGold = Color(0xFFFFC65C)
val AltitudeTeal = Color(0xFF78E0C4)
val AltitudeSkyBlue = Color(0xFF6EB2FF)
val AltitudePeriwinkle = Color(0xFF9696FF)
val EmergencyCoral = Color(0xFFFF5A47)
val EmeraldStatus = Color(0xFF34D399)

private val SkylightDarkColorScheme = darkColorScheme(
    primary = TelemetryCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = Color(0xFF0C2D48),
    onPrimaryContainer = LuminousIce,
    secondary = NebulaViolet,
    onSecondary = ObsidianBlack,
    secondaryContainer = Color(0xFF261C3D),
    onSecondaryContainer = LuminousIce,
    tertiary = AltitudeGold,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = LuminousIce,
    surface = DeepSpaceSurface,
    onSurface = LuminousIce,
    surfaceVariant = ElevatedCockpitSurface,
    onSurfaceVariant = MutedSlate,
    error = EmergencyCoral,
    onError = ObsidianBlack,
    outline = GridLineBlue
)

val SkylightTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)

@Composable
fun SkylightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkylightDarkColorScheme,
        typography = SkylightTypography,
        content = content
    )
}
