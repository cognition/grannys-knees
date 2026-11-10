package ca.aeo3.grannysknees.ui.theme

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


// Light · Lake teal
private val LakeTeal = Color(0xFF0E7C86)
private val LakeTealWell = Color(0xFFD9EEF0)
private val CoolPaper = Color(0xFFF4F8F9)
private val CoolPaperBright = Color(0xFFF9FCFC)
private val CoolPaperLow = Color(0xFFEEF4F5)
private val CoolContainer = Color(0xFFE7F1F3)
private val CoolContainerHigh = Color(0xFFD5E6EA)
private val CoolContainerHighest = Color(0xFFC5DCE2)
private val CoolDim = Color(0xFFC8D8DC)
private val Ink = Color(0xFF1C2A30)
private val InkMuted = Color(0xFF5E7380)
private val CoolLine = Color(0xFFC5D5DA)
private val Rough = Color(0xFFC24E2D)
private val RoughWell = Color(0xFFF8E4E1)
private val MildTeal = Color(0xFF2F6F78)
/** Mid-band (Noticeable) well — cool slate with ink for clear pill contrast. */
private val NoticeableWell = Color(0xFFCFE3E8)
private val NoticeableOn = Color(0xFF1A3A42)

// Dark · Soft navy with mint-teal accents
private val SoftNavy = Color(0xFF152033)
private val SoftNavyLowest = Color(0xFF101828)
private val SoftNavyLow = Color(0xFF18253A)
private val SoftNavySurface = Color(0xFF1B2A42)
private val SoftNavyContainer = Color(0xFF1E2C44)
private val SoftNavyRaised = Color(0xFF2A4A5C)
private val SoftNavyHigh = Color(0xFF334F66)
private val SoftNavyHighest = Color(0xFF3D5C78)
private val SoftNavyBright = Color(0xFF3A516E)
private val MintTeal = Color(0xFF7FE0E8)
private val MintOn = Color(0xFF0F2A38)
private val NightInk = Color(0xFFE8F0F8)
private val NightMuted = Color(0xFF8FA3BC)
private val NightLine = Color(0xFF3A4F68)
private val RoughNight = Color(0xFFFFC4BC)
private val RoughNightWell = Color(0xFF5A2E2A)
/** Mid-band (Noticeable) night well — lifted slate with bright ink. */
private val NoticeableNightWell = Color(0xFF3D5F72)
private val NoticeableNightOn = Color(0xFFE5F6FA)

/** Friendly corner used by compact journal and about controls. */
internal val GrannyCorner = RoundedCornerShape(12.dp)

private val LightColours = lightColorScheme(
    primary = LakeTeal,
    onPrimary = CoolPaper,
    primaryContainer = LakeTealWell,
    onPrimaryContainer = Ink,
    secondary = Rough,
    onSecondary = CoolPaper,
    secondaryContainer = RoughWell,
    onSecondaryContainer = Rough,
    tertiary = MildTeal,
    onTertiary = CoolPaper,
    tertiaryContainer = NoticeableWell,
    onTertiaryContainer = NoticeableOn,
    background = CoolPaper,
    onBackground = Ink,
    surface = CoolPaper,
    onSurface = Ink,
    surfaceVariant = CoolContainer,
    onSurfaceVariant = InkMuted,
    outline = InkMuted,
    outlineVariant = CoolLine,
    surfaceTint = Color.Transparent,
    surfaceBright = CoolPaperBright,
    surfaceContainerLowest = CoolPaperBright,
    surfaceContainerLow = CoolPaperLow,
    surfaceContainer = CoolContainer,
    surfaceContainerHigh = CoolContainerHigh,
    surfaceContainerHighest = CoolContainerHighest,
    surfaceDim = CoolDim,
)

private val DarkColours = darkColorScheme(
    primary = MintTeal,
    onPrimary = MintOn,
    primaryContainer = SoftNavyRaised,
    onPrimaryContainer = NightInk,
    secondary = RoughNight,
    onSecondary = RoughNightWell,
    secondaryContainer = RoughNightWell,
    onSecondaryContainer = RoughNight,
    tertiary = Color(0xFF8FCBD4),
    onTertiary = MintOn,
    tertiaryContainer = NoticeableNightWell,
    onTertiaryContainer = NoticeableNightOn,
    background = SoftNavy,
    onBackground = NightInk,
    surface = SoftNavySurface,
    onSurface = NightInk,
    surfaceVariant = SoftNavyContainer,
    onSurfaceVariant = NightMuted,
    outline = NightMuted,
    outlineVariant = NightLine,
    surfaceTint = Color.Transparent,
    surfaceBright = SoftNavyBright,
    surfaceContainerLowest = SoftNavyLowest,
    surfaceContainerLow = SoftNavyLow,
    surfaceContainer = SoftNavyContainer,
    surfaceContainerHigh = SoftNavyHigh,
    surfaceContainerHighest = SoftNavyHighest,
    surfaceDim = SoftNavyLowest,
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

