package com.islandos.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object IslandColors {
    val OledBlack = Color(0xFF000000)
    val SurfaceDark = Color(0xFF0C0E13)
    val SurfaceElevated = Color(0xFF151821)
    val HairlineBorder = Color(0xFF232734)
    val TextPrimary = Color(0xFFF4F6FA)
    val TextSecondary = Color(0xFF949CAE)
    val AccentBlue = Color(0xFF3B82F6)
    val AccentEmerald = Color(0xFF10B981)
    val AccentAmber = Color(0xFFF59E0B)
    val AccentRose = Color(0xFFF43F5E)
}

private val DarkScheme = darkColorScheme(
    primary = IslandColors.AccentBlue,
    onPrimary = Color.White,
    secondary = IslandColors.AccentEmerald,
    onSecondary = Color.Black,
    background = IslandColors.OledBlack,
    onBackground = IslandColors.TextPrimary,
    surface = IslandColors.SurfaceDark,
    onSurface = IslandColors.TextPrimary,
    surfaceVariant = IslandColors.SurfaceElevated,
    onSurfaceVariant = IslandColors.TextSecondary,
    outline = IslandColors.HairlineBorder,
    error = IslandColors.AccentRose
)

private val IslandTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

@Composable
fun IslandOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = IslandTypography,
        content = content
    )
}
