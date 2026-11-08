package com.example.grannysknees.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Paper = Color(0xFFF6E7D4)
private val PaperBright = Color(0xFFFBF3E8)
private val PaperLow = Color(0xFFF3E2CC)
private val PaperDeep = Color(0xFFE8D5BE)
private val PaperHigh = Color(0xFFE4D0B4)
private val PaperHighest = Color(0xFFDCC6A8)
private val PaperDim = Color(0xFFD4BCA0)
private val Ink = Color(0xFF1C1915)
private val InkMuted = Color(0xFF5C564C)
private val PaperLine = Color(0xFFD4C0A6)
private val GaugeRed = Color(0xFFD23A32)
private val GaugeGreen = Color(0xFF2E9B4A)

private val Night = Color(0xFF161310)
private val NightLowest = Color(0xFF100E0B)
private val NightLow = Color(0xFF1A1612)
private val NightRaised = Color(0xFF241F1A)
private val NightHigh = Color(0xFF2C261F)
private val NightHighest = Color(0xFF352E26)
private val NightBright = Color(0xFF3A322A)
private val NightInk = Color(0xFFF6E7D4)
private val NightMuted = Color(0xFFC4BBAE)
private val NightLine = Color(0xFF3A342C)

/** A short corner, closer to the drawn figure than a pill. */
internal val GrannyCorner = RoundedCornerShape(2.dp)

private val LightColours = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PaperDeep,
    onPrimaryContainer = Ink,
    secondary = GaugeRed,
    onSecondary = Paper,
    secondaryContainer = PaperDeep,
    onSecondaryContainer = Ink,
    tertiary = GaugeGreen,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
    outlineVariant = PaperLine,
    surfaceTint = Color.Transparent,
    surfaceBright = PaperBright,
    surfaceContainerLowest = PaperBright,
    surfaceContainerLow = PaperLow,
    surfaceContainer = PaperDeep,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    surfaceDim = PaperDim,
)

private val DarkColours = darkColorScheme(
    primary = NightInk,
    onPrimary = Night,
    primaryContainer = NightRaised,
    onPrimaryContainer = NightInk,
    secondary = GaugeRed,
    onSecondary = NightInk,
    secondaryContainer = NightRaised,
    onSecondaryContainer = NightInk,
    tertiary = GaugeGreen,
    onTertiary = Night,
    background = Night,
    onBackground = NightInk,
    surface = Night,
    onSurface = NightInk,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightMuted,
    outline = NightMuted,
    outlineVariant = NightLine,
    surfaceTint = Color.Transparent,
    surfaceBright = NightBright,
    surfaceContainerLowest = NightLowest,
    surfaceContainerLow = NightLow,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = NightHighest,
    surfaceDim = NightLowest,
)

private val GrannysKneesType = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val GrannysKneesShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
internal fun GrannysKneesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColours else LightColours,
        typography = GrannysKneesType,
        shapes = GrannysKneesShapes,
        content = content,
    )
}
