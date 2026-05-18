package com.bridgeflowfolk.bff.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// ─── Palette ─────────────────────────────────────────────────────────────────
// Inspirée du logo : vert forêt, beige doux, touches corail

object BffColors {
    val ForestGreen      = Color(0xFF2D5A27)   // vert dominant logo
    val SageGreen        = Color(0xFF5E8B57)   // vert clair
    val WarmBeige        = Color(0xFFF5F0E8)   // fond principal
    val LightBeige       = Color(0xFFFAF7F2)   // surface cards
    val BrownText        = Color(0xFF3C2A1E)   // texte principal
    val CoralAccent      = Color(0xFFD4845A)   // accent chaleureux
    val SkyBlue          = Color(0xFF87CEEB)   // ciel du logo
    val White            = Color(0xFFFFFFFF)
    val DividerColor     = Color(0xFFE0D8CE)
}

private val BffColorScheme = lightColorScheme(
    primary          = BffColors.ForestGreen,
    onPrimary        = BffColors.White,
    primaryContainer = Color(0xFFB8D4B3),
    secondary        = BffColors.SageGreen,
    onSecondary      = BffColors.White,
    tertiary         = BffColors.CoralAccent,
    background       = BffColors.WarmBeige,
    onBackground     = BffColors.BrownText,
    surface          = BffColors.LightBeige,
    onSurface        = BffColors.BrownText,
    surfaceVariant   = Color(0xFFEDE5D8),
    outline          = BffColors.DividerColor,
    error            = Color(0xFFBA1A1A)
)

// ─── Typographie ─────────────────────────────────────────────────────────────
// Note : pour une app de prod, ajouter les .ttf dans res/font/
// et utiliser FontFamily(Font(R.font.cormorant_garamond_regular))

private val BffTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium  = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge    = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium   = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall   = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

// ─── Thème principal ─────────────────────────────────────────────────────────

@Composable
fun BffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BffColorScheme,
        typography  = BffTypography,
        content     = content
    )
}
