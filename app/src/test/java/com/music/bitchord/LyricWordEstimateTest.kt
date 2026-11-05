package com.music.bitchord

import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricWord
import com.music.bitchord.data.settings.LyricStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.music.bitchord.ui.player.lyricLinesForStyleTest as styled

class LyricWordEstimateTest {
    private val plain = listOf(
        LyricLine(timeMs = 1_000, text = "short muchlongerword"),
        LyricLine(timeMs = 5_000, text = "next"),
    )

    @Test
    fun estimatesCoverTheLineByLengthAndAreMarked() {
        val line = styled(plain, LyricStyle.KARAOKE).first()
        assertTrue(line.wordTimingEstimated)
        assertEquals(1_000L, line.words.first().startMs)
        assertEquals(line.words.first().endMs, line.words.last().startMs)
        assertEquals(5_000L, line.words.last().endMs)
        assertTrue(
            line.words[1].endMs - line.words[1].startMs > line.words[0].endMs - line.words[0].startMs,
        )
    }

    @Test
    fun realWordTimingIsKept() {
        val real = listOf(
            LyricLine(
                timeMs = 1_000,
                text = "hello there",
                words = listOf(LyricWord(1_000, 1_500, "hello"), LyricWord(1_500, 2_500, "there")),
            ),
        )
        val line = styled(real, LyricStyle.KARAOKE).single()
        assertFalse(line.wordTimingEstimated)
        assertEquals(real.single().words, line.words)
    }

    @Test
    fun wordByWordLightsEachWordQuicklyAtItsOwnStart() {
        val line = styled(plain, LyricStyle.WORD_BY_WORD).first()
        assertEquals(1_000L, line.words.first().startMs)
        assertTrue(line.words.all { it.endMs - it.startMs <= 90 })
    }

    @Test
    fun lineStyleLightsWholeLinesButKeepsWhenTheyEnd() {
        val real = LyricLine(
            timeMs = 1_000,
            text = "hello there",
            words = listOf(LyricWord(1_000, 1_500, "hello"), LyricWord(1_500, 2_500, "there")),
        )
        val line = styled(listOf(real), LyricStyle.LINE).single()
        assertTrue(line.words.isEmpty())
        assertEquals(2_500L, line.endMs)
    }

    @Test
    fun unsyncedLyricsAreLeftAlone() {
        val unsynced = listOf(LyricLine(timeMs = 0, text = "no timing here"))
        assertEquals(unsynced, styled(unsynced, LyricStyle.KARAOKE))
    }
}
