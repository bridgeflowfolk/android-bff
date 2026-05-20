package com.bridgeflowfolk.bff.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Palette ──────────────────────────────────────────────────────────────────

object BffColors {
    val ForestGreen        = Color(0xFF2D5A27)
    val SageGreen          = Color(0xFF5E8B57)
    val WarmBeige          = Color(0xFFF5F0E8)
    val LightBeige         = Color(0xFFFAF7F2)
    val BrownText          = Color(0xFF3C2A1E)
    val CoralAccent        = Color(0xFFD4845A)
    val SkyBlue            = Color(0xFF87CEEB)
    val White              = Color(0xFFFFFFFF)
    val DividerColor       = Color(0xFFE0D8CE)
    val SurfaceVar         = Color(0xFFEDE5D8)
    val PrimaryCont        = Color(0xFFB8D4B3)
    // Conteneur secondaire : beige chaud — utilisé par FilledTonalButton
    val SecondaryCont      = Color(0xFFEADFD2)
    val OnSecondaryCont    = Color(0xFF3C2A1E)
    val OutlineVar         = Color(0xFFCCC4B8)
}

// ─── Schéma de couleurs ───────────────────────────────────────────────────────

private val BffColorScheme = lightColorScheme(
    primary              = BffColors.ForestGreen,
    onPrimary            = BffColors.White,
    primaryContainer     = BffColors.PrimaryCont,
    onPrimaryContainer   = BffColors.BrownText,
    secondary            = BffColors.SageGreen,
    onSecondary          = BffColors.White,
    // FilledTonalButton utilise secondaryContainer → beige chaud sobre
    secondaryContainer   = BffColors.SecondaryCont,
    onSecondaryContainer = BffColors.OnSecondaryCont,
    tertiary             = BffColors.CoralAccent,
    onTertiary           = BffColors.White,
    background           = BffColors.WarmBeige,
    onBackground         = BffColors.BrownText,
    surface              = BffColors.LightBeige,
    onSurface            = BffColors.BrownText,
    surfaceVariant       = BffColors.SurfaceVar,
    onSurfaceVariant     = Color(0xFF5C4F44),
    outline              = BffColors.DividerColor,
    outlineVariant       = BffColors.OutlineVar,
    error                = Color(0xFFBA1A1A),
    onError              = BffColors.White,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002)
)

// ─── Typographie ──────────────────────────────────────────────────────────────

private val BffTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge   = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium  = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium   = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall    = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium  = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall   = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp)
)

// ─── Formes ───────────────────────────────────────────────────────────────────

private val BffShapes = Shapes(
    small  = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(12.dp),
    large  = RoundedCornerShape(24.dp)
)

// ─── Thème principal ──────────────────────────────────────────────────────────

@Composable
fun BffTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BffColorScheme,
        typography  = BffTypography,
        shapes      = BffShapes,
        content     = content
    )
}
