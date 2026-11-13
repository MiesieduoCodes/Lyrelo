package com.music.bitchord.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.music.bitchord.R
import com.music.bitchord.data.settings.AccentChoice
import com.music.bitchord.data.settings.AppFont

// Apple Music's signature red, kept for Replay's rank badge, which wants that
// specific red whatever the theme.
val AccentRed = Color(0xFFFA2D48)

/*
 * Lyrelo's type: Geologica for display and headings, Onest for everything
 * people read at length. Both are variable fonts under the SIL Open Font
 * License (licenses/fonts/), and both cover Latin, Cyrillic and Kazakh, so a
 * learner's language never falls back to another face mid-word.
 *
 * The app's code names weights by the step they sit at in the scale (W800 for
 * a page title, W700 for a card title), a habit from the font it used before.
 * Drawn literally, those steps are heavy in these faces, so each family draws
 * them a little lighter, which keeps the hierarchy and loses the shouting.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int, drawn: (Int) -> Int) = FontFamily(
    listOf(300, 400, 500, 600, 700, 800, 900).map { step ->
        Font(
            resId = resId,
            weight = FontWeight(step),
            variationSettings = FontVariation.Settings(FontVariation.weight(drawn(step))),
        )
    },
)

val Geologica = variableFamily(R.font.lyrelo_geologica) { step ->
    when {
        step >= 800 -> 700
        step >= 700 -> 620
        step >= 600 -> 560
        else -> step
    }
}

val Onest = variableFamily(R.font.lyrelo_onest) { step ->
    when {
        step >= 800 -> 700
        step >= 700 -> 640
        step >= 600 -> 580
        else -> step
    }
}

private val LyreloTypography = Typography(
    displayLarge = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W800, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.6).sp),
    displayMedium = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W800, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W700, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W800, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W700, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = Geologica, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W600, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.05.sp),
    bodyLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
    bodySmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W600, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
)

/** The same scale in the phone's own font, for people who read that more easily. */
private val SystemTypography = Typography(
    displayLarge = LyreloTypography.displayLarge.copy(fontFamily = FontFamily.Default, letterSpacing = (-0.3).sp),
    displayMedium = LyreloTypography.displayMedium.copy(fontFamily = FontFamily.Default),
    displaySmall = LyreloTypography.displaySmall.copy(fontFamily = FontFamily.Default),
    headlineLarge = LyreloTypography.headlineLarge.copy(fontFamily = FontFamily.Default),
    headlineMedium = LyreloTypography.headlineMedium.copy(fontFamily = FontFamily.Default),
    headlineSmall = LyreloTypography.headlineSmall.copy(fontFamily = FontFamily.Default),
    titleLarge = LyreloTypography.titleLarge.copy(fontFamily = FontFamily.Default),
    titleMedium = LyreloTypography.titleMedium.copy(fontFamily = FontFamily.Default),
    titleSmall = LyreloTypography.titleSmall.copy(fontFamily = FontFamily.Default),
    bodyLarge = LyreloTypography.bodyLarge.copy(fontFamily = FontFamily.Default),
    bodyMedium = LyreloTypography.bodyMedium.copy(fontFamily = FontFamily.Default),
    bodySmall = LyreloTypography.bodySmall.copy(fontFamily = FontFamily.Default),
    labelLarge = LyreloTypography.labelLarge.copy(fontFamily = FontFamily.Default),
    labelMedium = LyreloTypography.labelMedium.copy(fontFamily = FontFamily.Default),
    labelSmall = LyreloTypography.labelSmall.copy(fontFamily = FontFamily.Default),
)

/** An accent's two gradient ends, before they are toned for the page they sit on. */
private fun AccentChoice.seed(): Pair<Color, Color> = when (this) {
    AccentChoice.LYRELO, AccentChoice.DYNAMIC, AccentChoice.MATERIAL_YOU -> LyreloBrand.Violet to LyreloBrand.Pink
    AccentChoice.VIOLET -> Color(0xFF8B5CF6) to Color(0xFF6366F1)
    AccentChoice.PINK -> Color(0xFFEC4899) to Color(0xFFF43F5E)
    AccentChoice.OCEAN -> Color(0xFF0EA5E9) to Color(0xFF6366F1)
    AccentChoice.MINT -> Color(0xFF10B981) to Color(0xFF06B6D4)
    AccentChoice.SUNSET -> Color(0xFFF97316) to Color(0xFFEC4899)
    AccentChoice.GOLD -> Color(0xFFEAB308) to Color(0xFFF97316)
    AccentChoice.CRIMSON -> Color(0xFFE11D48) to Color(0xFFBE123C)
}

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * [this] moved toward white on a dark page, or toward black on a light one,
 * just far enough to read as text on [page] (4.5:1). Any accent, preset or
 * taken from artwork, comes out legible without a hand-tuned shade for each.
 */
private fun Color.legibleOn(page: Color, ratio: Float = 4.5f): Color {
    val toward = if (page.luminance() < 0.5f) Color.White else Color.Black
    var step = 0f
    var tone = this
    while (contrast(tone, page) < ratio && step < 1f) {
        step += 0.05f
        tone = lerp(this, toward, step)
    }
    return tone
}

private fun lyreloScheme(dark: Boolean, pureBlack: Boolean, accent: Color): ColorScheme = if (dark) {
    val page = if (pureBlack) Color.Black else LyreloBrand.Night
    val primary = accent.legibleOn(page)
    darkColorScheme(
        primary = primary,
        onPrimary = if (primary.luminance() > 0.4f) LyreloBrand.Night else Color.White,
        primaryContainer = accent.copy(alpha = 0.24f).compositeOn(page),
        onPrimaryContainer = Color.White,
        secondary = primary,
        onSecondary = LyreloBrand.Night,
        tertiary = LyreloBrand.Pink.legibleOn(page),
        background = page,
        onBackground = Color.White,
        surface = if (pureBlack) Color(0xFF0B0812) else Color(0xFF1A1130),
        onSurface = Color.White,
        surfaceVariant = if (pureBlack) Color(0xFF17121F) else Color(0xFF251A3D),
        onSurfaceVariant = Color(0xFFB8AECF),
        surfaceContainer = if (pureBlack) Color(0xFF0B0812) else Color(0xFF1A1130),
        surfaceContainerHigh = if (pureBlack) Color(0xFF17121F) else Color(0xFF251A3D),
        outline = Color(0xFF3A2E56),
        outlineVariant = Color(0xFF2B2142),
    )
} else {
    val page = Color.White
    val primary = accent.legibleOn(page)
    lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.14f).compositeOn(page),
        onPrimaryContainer = LyreloBrand.Night,
        secondary = primary,
        onSecondary = Color.White,
        tertiary = LyreloBrand.Pink.legibleOn(page),
        background = page,
        onBackground = Color(0xFF120B20),
        surface = LyreloBrand.Mist,
        onSurface = Color(0xFF120B20),
        surfaceVariant = LyreloBrand.MistRaised,
        onSurfaceVariant = Color(0xFF5E5672),
        surfaceContainer = LyreloBrand.Mist,
        surfaceContainerHigh = LyreloBrand.MistRaised,
        outline = Color(0xFFE0DAEE),
        outlineVariant = Color(0xFFEAE5F4),
    )
}

private fun Color.compositeOn(under: Color): Color = Color(
    red = red * alpha + under.red * (1 - alpha),
    green = green * alpha + under.green * (1 - alpha),
    blue = blue * alpha + under.blue * (1 - alpha),
)

/**
 * Lyrelo's theme: Material's colours and type plus [LyreloColors].
 *
 * @param accent which accent the app is drawn in.
 * @param artworkTint the playing song's colour, for [AccentChoice.DYNAMIC].
 * @param pureBlack paints the dark theme's page true black (OLED).
 */
@Composable
fun LyreloAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.DEFAULT,
    accent: AccentChoice = AccentChoice.LYRELO,
    pureBlack: Boolean = false,
    artworkTint: Color? = null,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val (seedStart, seedEnd) = accent.seed()
    val scheme = remember(darkTheme, pureBlack, accent, artworkTint) {
        when {
            accent == AccentChoice.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (darkTheme && pureBlack) dynamic.copy(background = Color.Black, surface = Color(0xFF0B0B0B)) else dynamic
            }
            accent == AccentChoice.DYNAMIC && artworkTint != null -> lyreloScheme(darkTheme, pureBlack, artworkTint)
            else -> lyreloScheme(darkTheme, pureBlack, seedStart)
        }
    }
    val lyrelo = remember(scheme, seedEnd, darkTheme) {
        LyreloColors.of(scheme.primary, seedEnd.legibleOn(scheme.background, ratio = 3f), darkTheme)
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = when (font) {
            AppFont.DEFAULT -> LyreloTypography
            AppFont.LEGACY -> SystemTypography
        },
    ) {
        CompositionLocalProvider(
            LocalLyreloColors provides lyrelo,
            LocalReduceMotion provides reduceMotion,
            content = content,
        )
    }
}

/** Kept so previews and older call sites keep compiling; draws [LyreloAppTheme]. */
@Composable
fun BitChordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.DEFAULT,
    content: @Composable () -> Unit,
) = LyreloAppTheme(darkTheme = darkTheme, font = font, content = content)

/**
 * Draws the status and navigation bar glyphs dark or light.
 *
 * `enableEdgeToEdge()` decides this from the *system* dark-mode setting, which
 * is the wrong input the moment the in-app theme disagrees with it: Light theme
 * on a phone in dark mode left white icons on a white bar, invisible. The bars
 * have to follow the content the app is actually painting. The player supplies
 * its own stable light-icon value and paints contrast behind it; every other
 * surface follows the theme. Hence a parameter rather than reading it here.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findWindow(view) ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
