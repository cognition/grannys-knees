package com.aetherealmind.grannysknees.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Whether the active Material scheme is the soft-navy dark palette. */
internal val LocalUseDarkTheme = staticCompositionLocalOf { false }

/**
 * Personal ("Yours") index colour — warm amber, distinct from teal Weather.
 * Used for the personal curve and for score numbers when calibration is custom.
 */
private val PersonalIndexLight = Color(0xFFC9782D)
private val PersonalIndexDark = Color(0xFFFFC56E)

/** Amber used for the personal index line and personalised score numerals. */
@Composable
internal fun personalIndexColour(): Color {
    return if (LocalUseDarkTheme.current) PersonalIndexDark else PersonalIndexLight
}


/**
 * Locked lake-teal / soft-navy brand colours (design DE-01).
 *
 * Exposed for token contract tests so palette drift fails in CI without
 * screenshot goldens.
 */
internal object BrandColours {
    // Light · Lake teal
    val LakeTeal = Color(0xFF0E7C86)
    val LakeTealWell = Color(0xFFD9EEF0)
    val CoolPaper = Color(0xFFF4F8F9)
    val CoolPaperBright = Color(0xFFF9FCFC)
    val CoolPaperLow = Color(0xFFEEF4F5)
    val CoolContainer = Color(0xFFE7F1F3)
    val CoolContainerHigh = Color(0xFFD5E6EA)
    val CoolContainerHighest = Color(0xFFC5DCE2)
    val CoolDim = Color(0xFFC8D8DC)
    val Ink = Color(0xFF1C2A30)
    val InkMuted = Color(0xFF5E7380)
    val CoolLine = Color(0xFFC5D5DA)
    val Rough = Color(0xFFC24E2D)
    val RoughWell = Color(0xFFF8E4E1)
    val MildTeal = Color(0xFF2F6F78)
    /** Mid-band (Noticeable) well — cool slate with ink for clear pill contrast. */
    val NoticeableWell = Color(0xFFCFE3E8)
    val NoticeableOn = Color(0xFF1A3A42)

    // Dark · Soft navy with mint-teal accents
    val SoftNavy = Color(0xFF152033)
    val SoftNavyLowest = Color(0xFF101828)
    val SoftNavyLow = Color(0xFF18253A)
    val SoftNavySurface = Color(0xFF1B2A42)
    val SoftNavyContainer = Color(0xFF1E2C44)
    val SoftNavyRaised = Color(0xFF2A4A5C)
    val SoftNavyHigh = Color(0xFF334F66)
    val SoftNavyHighest = Color(0xFF3D5C78)
    val SoftNavyBright = Color(0xFF3A516E)
    val MintTeal = Color(0xFF7FE0E8)
    val MintOn = Color(0xFF0F2A38)
    val NightInk = Color(0xFFE8F0F8)
    val NightMuted = Color(0xFF8FA3BC)
    val NightLine = Color(0xFF3A4F68)
    val RoughNight = Color(0xFFFFC4BC)
    val RoughNightWell = Color(0xFF5A2E2A)
    /** Mid-band (Noticeable) night well — lifted slate with bright ink. */
    val NoticeableNightWell = Color(0xFF3D5F72)
    val NoticeableNightOn = Color(0xFFE5F6FA)
}

/** Friendly corner used by compact journal and about controls. */
internal val GrannyCorner = RoundedCornerShape(12.dp)

private val LightColours = lightColorScheme(
    primary = BrandColours.LakeTeal,
    onPrimary = BrandColours.CoolPaper,
    primaryContainer = BrandColours.LakeTealWell,
    onPrimaryContainer = BrandColours.Ink,
    secondary = BrandColours.Rough,
    onSecondary = BrandColours.CoolPaper,
    secondaryContainer = BrandColours.RoughWell,
    onSecondaryContainer = BrandColours.Rough,
    tertiary = BrandColours.MildTeal,
    onTertiary = BrandColours.CoolPaper,
    tertiaryContainer = BrandColours.NoticeableWell,
    onTertiaryContainer = BrandColours.NoticeableOn,
    background = BrandColours.CoolPaper,
    onBackground = BrandColours.Ink,
    surface = BrandColours.CoolPaper,
    onSurface = BrandColours.Ink,
    surfaceVariant = BrandColours.CoolContainer,
    onSurfaceVariant = BrandColours.InkMuted,
    outline = BrandColours.InkMuted,
    outlineVariant = BrandColours.CoolLine,
    surfaceTint = Color.Transparent,
    surfaceBright = BrandColours.CoolPaperBright,
    surfaceContainerLowest = BrandColours.CoolPaperBright,
    surfaceContainerLow = BrandColours.CoolPaperLow,
    surfaceContainer = BrandColours.CoolContainer,
    surfaceContainerHigh = BrandColours.CoolContainerHigh,
    surfaceContainerHighest = BrandColours.CoolContainerHighest,
    surfaceDim = BrandColours.CoolDim,
)

private val DarkColours = darkColorScheme(
    primary = BrandColours.MintTeal,
    onPrimary = BrandColours.MintOn,
    primaryContainer = BrandColours.SoftNavyRaised,
    onPrimaryContainer = BrandColours.NightInk,
    secondary = BrandColours.RoughNight,
    onSecondary = BrandColours.RoughNightWell,
    secondaryContainer = BrandColours.RoughNightWell,
    onSecondaryContainer = BrandColours.RoughNight,
    tertiary = Color(0xFF8FCBD4),
    onTertiary = BrandColours.MintOn,
    tertiaryContainer = BrandColours.NoticeableNightWell,
    onTertiaryContainer = BrandColours.NoticeableNightOn,
    background = BrandColours.SoftNavy,
    onBackground = BrandColours.NightInk,
    surface = BrandColours.SoftNavySurface,
    onSurface = BrandColours.NightInk,
    surfaceVariant = BrandColours.SoftNavyContainer,
    onSurfaceVariant = BrandColours.NightMuted,
    outline = BrandColours.NightMuted,
    outlineVariant = BrandColours.NightLine,
    surfaceTint = Color.Transparent,
    surfaceBright = BrandColours.SoftNavyBright,
    surfaceContainerLowest = BrandColours.SoftNavyLowest,
    surfaceContainerLow = BrandColours.SoftNavyLow,
    surfaceContainer = BrandColours.SoftNavyContainer,
    surfaceContainerHigh = BrandColours.SoftNavyHigh,
    surfaceContainerHighest = BrandColours.SoftNavyHighest,
    surfaceDim = BrandColours.SoftNavyLowest,
)

private val GrannysKneesType = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
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
        fontSize = 15.sp,
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
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
internal fun GrannysKneesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalUseDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColours else LightColours,
            typography = GrannysKneesType,
            shapes = GrannysKneesShapes,
            content = content,
        )
    }
}

