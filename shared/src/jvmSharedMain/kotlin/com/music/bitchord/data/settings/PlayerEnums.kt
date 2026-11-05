package com.music.bitchord.data.settings

/**
 * Stream bitrate ceiling on the YouTube fallback path — MEDIUM, HIGH and
 * LOSSLESS all mean "whatever the best available Opus format is" there; what
 * actually tells them apart is which other sources are allowed to answer
 * *before* YouTube gets asked. That part is [permits], and the rungs read:
 *
 * - [LOSSLESS] — the user's own addons and JioSaavn both asked.
 * - [HIGH] — the addons skipped, JioSaavn asked.
 * - [MEDIUM] and [LOW] — both skipped; YouTube's own Opus ladder is all there
 *   is, capped at [maxKbps].
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps · smallest download", "29 MB/hr"),
    MEDIUM(Int.MAX_VALUE, "Medium", "Best available · ~171 kbps Opus", "77 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "JioSaavn up to 320kbps, YouTube fallback", "144 MB/hr"),
    LOSSLESS(Int.MAX_VALUE, "Lossless", "Your addons + JioSaavn, bit-exact where available", "300+ MB/hr"),
    ;
}

/** The surface that was last open inside the expanded player. */
enum class LastPlayerScreen {
    MAIN,
    LYRICS,
    QUEUE,
}

/** The layout of the full player. Classic is the player as it has always been. */
enum class PlayerStyle { CLASSIC, IMMERSIVE, VINYL, MINIMAL, LYRICS_FIRST }

/** What fills the player behind its artwork and controls. */
enum class PlayerBackground { ARTWORK_COLORS, BLUR, SOLID_DARK, ANIMATED_GRADIENT }

/** How far the artwork's corners are rounded, in dp. Vinyl is always round. */
enum class ArtworkCorner(val dp: Int) { NONE(0), SMALL(8), MEDIUM(20), LARGE(36) }

/** How the lyric panel marks what is being sung. */
enum class LyricStyle { LINE, GLOW, KARAOKE, WORD_BY_WORD }

enum class LyricFontSize(val scale: Float) { SMALL(0.8f), MEDIUM(1f), LARGE(1.2f), HUGE(1.4f) }

enum class LyricFont { DEFAULT, SANS, SERIF, MONO }

enum class LyricAlign { LEFT, CENTER }

enum class TranslationSize(val scale: Float) { SMALL(0.8f), MEDIUM(1f), LARGE(1.25f) }

/** The translation line's color, or null for the player's own tint. */
enum class TranslationColor(val argb: Long?) {
    DEFAULT(null),
    WHITE(0xFFFFFFFF),
    AMBER(0xFFFFC857),
    MINT(0xFF7CE7C3),
    PINK(0xFFFF9BCB),
}
