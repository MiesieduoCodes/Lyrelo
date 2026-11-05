package com.music.bitchord.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricWord
import com.music.bitchord.data.settings.LyricFont
import com.music.bitchord.data.settings.LyricStyle
import com.music.bitchord.playback.PlaybackPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** How long a word takes to light in Word by word: quick enough to read as a switch. */
private const val WORD_LIGHT_MS = 90L

/** The most an estimate lets one character take, so a long instrumental gap isn't swept across. */
private const val ESTIMATE_MAX_MS_PER_CHAR = 450L
private const val ESTIMATE_MIN_MS = 800L

/**
 * The lyric sheet as a [style] wants it drawn, line for line.
 *
 * Only what is on screen changes. The result is never stored or cached, so an
 * estimate can't outlive the sheet it was made for: when a source with real word
 * timing is found, that sheet arrives here with real words and is drawn as it is.
 */
internal fun styledLyricLines(lines: List<LyricLine>, style: LyricStyle): List<LyricLine> {
    if (lines.none { it.timeMs > 0L }) return lines
    return when (style) {
        LyricStyle.LINE, LyricStyle.GLOW -> lines.map(::wholeLine)
        LyricStyle.KARAOKE -> lines.mapIndexed { index, line ->
            withWordTimings(line, lines.getOrNull(index + 1))
        }
        LyricStyle.WORD_BY_WORD -> lines.mapIndexed { index, line ->
            val timed = withWordTimings(line, lines.getOrNull(index + 1))
            timed.copy(words = timed.words.map(::lightAtOnce))
        }
    }
}

/** The line with its word timings set aside, so that it lights all at once. */
private fun wholeLine(line: LyricLine): LyricLine {
    if (line.words.isEmpty() && line.background == null) return line
    return line.copy(
        words = emptyList(),
        // The words were what said when the line ended.
        sungUntilMs = line.sungUntilMs ?: line.endMs,
        timingSource = null,
        background = line.background?.let(::wholeLine),
    )
}

private fun withWordTimings(line: LyricLine, next: LyricLine?): LyricLine {
    if (line.isWordSynced || line.isGap) return line
    val words = estimatedWords(line, next)
    return if (words.isEmpty()) line else line.copy(words = words, wordTimingEstimated = true)
}

private fun lightAtOnce(word: LyricWord): LyricWord = word.copy(
    endMs = minOf(word.endMs, word.startMs + WORD_LIGHT_MS),
    syllables = emptyList(),
)

/** Spreads the line's time across its words by length. */
internal fun estimatedWords(line: LyricLine, next: LyricLine?): List<LyricWord> {
    val tokens = line.text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()
    val available = (line.sungUntilMs ?: next?.timeMs ?: (line.timeMs + ESTIMATE_MIN_MS)) - line.timeMs
    val characters = tokens.sumOf { it.length }
    val duration = available
        .coerceAtMost(characters * ESTIMATE_MAX_MS_PER_CHAR + ESTIMATE_MIN_MS)
        .coerceAtLeast(ESTIMATE_MIN_MS)
    val weight = tokens.sumOf { it.length + 1 }.toDouble()
    var start = line.timeMs
    return tokens.mapIndexed { index, token ->
        val length = (duration * ((token.length + 1) / weight)).toLong()
        val end = if (index == tokens.lastIndex) line.timeMs + duration else start + length
        LyricWord(startMs = start, endMs = end, text = token).also { start = end }
    }
}

internal fun LyricFont.family(): FontFamily? = when (this) {
    LyricFont.DEFAULT -> null
    LyricFont.SANS -> FontFamily.SansSerif
    LyricFont.SERIF -> FontFamily.Serif
    LyricFont.MONO -> FontFamily.Monospace
}

private const val PREVIEW_LINE_MS = 3_200L
private const val PREVIEW_TICK_MS = 100L

private val PreviewLyrics = listOf(
    "Bailando bajo la lluvia" to "Dancing in the rain",
    "Tu voz me lleva a casa" to "Your voice carries me home",
    "Cantamos hasta el amanecer" to "We sing until dawn",
    "Y el mundo se detiene" to "And the world stands still",
)

/**
 * The real lyric panel, on a short sample verse with its own clock, so the
 * preview is exactly what the player will draw with the current settings.
 */
@Composable
fun LyricStylePreview(modifier: Modifier = Modifier) {
    val position = remember { PlaybackPosition() }
    val lines = remember {
        PreviewLyrics.mapIndexed { index, (original, _) ->
            LyricLine(timeMs = PREVIEW_START_MS + index * PREVIEW_LINE_MS, text = original)
        }
    }
    val translations = remember {
        PreviewLyrics.mapIndexed { index, (_, translated) ->
            LyricLine(timeMs = PREVIEW_START_MS + index * PREVIEW_LINE_MS, text = translated)
        }
    }
    LaunchedEffect(Unit) {
        val loopMs = PREVIEW_START_MS + PreviewLyrics.size * PREVIEW_LINE_MS
        var at = PREVIEW_START_MS
        while (isActive) {
            position.report(at)
            delay(PREVIEW_TICK_MS)
            at += PREVIEW_TICK_MS
            if (at >= loopMs) {
                at = PREVIEW_START_MS
                position.seeks++
            }
        }
    }
    CompositionLocalProvider(LocalPlayerTint provides Color(0xFFC4B5FD)) {
        Box(modifier) {
            LyricsPanel(
                lines = lines,
                subLines = translations,
                trackKey = "lyric-style-preview",
                playhead = rememberLyricPlayhead(position),
                looking = false,
                isPlaying = true,
                onSeekToLine = {},
                controlsOpen = true,
                onRevealControls = {},
                onHideControls = {},
                centerFocus = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val PREVIEW_START_MS = 1_000L

/** [styledLyricLines] for tests in another module. */
fun lyricLinesForStyleTest(lines: List<LyricLine>, style: LyricStyle): List<LyricLine> =
    styledLyricLines(lines, style)
