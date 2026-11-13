package com.music.bitchord.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Lyrelo's design tokens. One place for the values the screens draw with, so
 * a colour, a gap or a corner means the same thing everywhere.
 *
 * Material's [androidx.compose.material3.ColorScheme] still carries the
 * surface and text colours. These add what Material has no slot for: the
 * brand gradient, the Home tiles, the learning flame and the dark overlay
 * the player draws its controls on.
 */
object LyreloBrand {
    val Violet = Color(0xFF8B5CF6)
    val Pink = Color(0xFFEC4899)
    /** The dark theme's page colour: a violet-tinted night, not grey. */
    val Night = Color(0xFF140C24)
    val NightRaised = Color(0xFF1D1433)
    val NightHigh = Color(0xFF281C44)
    val Mist = Color(0xFFF7F5FC)
    val MistRaised = Color(0xFFEFEBF8)
    val Flame = Color(0xFFFF7A1A)
    val Cyan = Color(0xFF22D3EE)
}

/** Gaps, on a 4dp grid. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** The page's side margin, shared by every tab so headings line up. */
    val gutter = 16.dp
}

/** Corner radii. Pills use [androidx.compose.foundation.shape.CircleShape]. */
object Radius {
    val sm = 8.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 28.dp
    val small = RoundedCornerShape(sm)
    val medium = RoundedCornerShape(md)
    val large = RoundedCornerShape(lg)
    val xlarge = RoundedCornerShape(xl)
}

/** Shadow elevations, for the few surfaces that float. */
object Elevation {
    val none = 0.dp
    val low = 1.dp
    val mid = 4.dp
    val high = 8.dp
}

/** Sizes that are about hands, not looks. */
object Touch {
    /** The smallest thing a finger should be asked to hit (WCAG 2.5.5, Material). */
    val min = 48.dp
    /** A small icon drawn inside a [min] target. */
    val icon = 20.dp
}

/** Motion durations, in milliseconds. */
object Motion {
    const val quick = 150
    const val standard = 250
    const val emphasized = 400
}

/** A Home tile's gradient. */
@Immutable
data class TileColors(val start: Color, val end: Color) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
}

/**
 * The colours Material has no slot for. Built by the app's theme from the
 * accent and light/dark choice, and read with [LyreloTheme.colors].
 */
@Immutable
data class LyreloColors(
    /** The accent: the brand violet, a preset, or one taken from artwork. */
    val accent: Color,
    /** The other end of the accent gradient. */
    val accentEnd: Color,
    val onAccent: Color,
    val tileDj: TileColors,
    val tileContinue: TileColors,
    val tilePractice: TileColors,
    val tileMoods: TileColors,
    /** The learning streak's flame and its pill behind it. */
    val flame: Color,
    val flameContainer: Color,
    /** A dark glass drawn over artwork, and the text on it. */
    val overlay: Color,
    val overlayStrong: Color,
    val onOverlay: Color,
    val onOverlayMuted: Color,
    val isDark: Boolean,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accent, accentEnd))

    companion object {
        /**
         * The tiles are dark enough that white text, and the subtitle at 92%,
         * stay above 4.5:1 across the whole gradient in both themes.
         */
        fun of(accent: Color, accentEnd: Color, dark: Boolean) = LyreloColors(
            accent = accent,
            accentEnd = accentEnd,
            onAccent = Color.White,
            tileDj = TileColors(Color(0xFF2A1250), Color(0xFF7A2A5C)),
            tileContinue = TileColors(Color(0xFF0B5F75), Color(0xFF134E63)),
            tilePractice = TileColors(Color(0xFF9A3F0A), Color(0xFF7C2D12)),
            tileMoods = TileColors(Color(0xFF136B34), Color(0xFF14532D)),
            flame = LyreloBrand.Flame,
            flameContainer = LyreloBrand.Flame.copy(alpha = if (dark) 0.18f else 0.14f),
            overlay = Color.Black.copy(alpha = 0.6f),
            overlayStrong = Color.Black.copy(alpha = 0.78f),
            onOverlay = Color.White,
            onOverlayMuted = Color.White.copy(alpha = 0.78f),
            isDark = dark,
        )

        val Default = of(LyreloBrand.Violet, LyreloBrand.Pink, dark = true)
    }
}

val LocalLyreloColors = staticCompositionLocalOf { LyreloColors.Default }

/** Whether to hold still: the "Reduce animation" preference. */
val LocalReduceMotion = staticCompositionLocalOf { false }

object LyreloTheme {
    val colors: LyreloColors
        @Composable @ReadOnlyComposable get() = LocalLyreloColors.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
}

/**
 * The playing song's artwork, for the Dynamic accent. Set by the app as the
 * song changes; read where the theme is built, which sits above the player.
 */
object ThemeArtwork {
    val url = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
}
