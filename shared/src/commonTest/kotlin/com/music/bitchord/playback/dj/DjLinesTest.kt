package com.music.bitchord.playback.dj

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DjLinesTest {

    @Test
    fun picker_neverRepeatsBackToBack() {
        repeat(20) { seed ->
            val picker = DjLinePicker(Random(seed))
            var previous = -1
            repeat(200) {
                val next = picker.pick(DjMoment.NEXT_SONG, 4)
                assertNotEquals(previous, next, "seed $seed repeated $next")
                previous = next
            }
        }
    }

    @Test
    fun picker_usesEveryVariantBeforeRepeating() {
        val picker = DjLinePicker(Random(7))
        val firstRound = List(6) { picker.pick(DjMoment.INTRO, 6) }
        assertEquals((0 until 6).toSet(), firstRound.toSet())
    }

    @Test
    fun picker_keepsMomentsSeparate() {
        val picker = DjLinePicker(Random(1))
        val intros = List(3) { picker.pick(DjMoment.INTRO, 3) }.toSet()
        val streaks = List(3) { picker.pick(DjMoment.STREAK, 3) }.toSet()
        assertEquals(setOf(0, 1, 2), intros)
        assertEquals(setOf(0, 1, 2), streaks)
    }

    @Test
    fun picker_singleVariantIsAlwaysZero() {
        val picker = DjLinePicker(Random(3))
        repeat(5) { assertEquals(0, picker.pick(DjMoment.OUTRO, 1)) }
    }

    @Test
    fun schedule_talksOnFirstSongThenWithinRange() {
        for (chattiness in DjChattiness.entries) {
            val schedule = DjTalkSchedule(chattiness, Random(11))
            schedule.reset()
            assertTrue(schedule.onSongStarted(), "$chattiness: first song is announced")
            var gap = 0
            var gaps = 0
            repeat(300) {
                gap++
                if (schedule.onSongStarted()) {
                    assertTrue(gap in chattiness.minGap..chattiness.maxGap, "$chattiness gap $gap")
                    gap = 0
                    gaps++
                }
            }
            assertTrue(gaps > 10, "$chattiness talked $gaps times")
        }
    }

    @Test
    fun schedule_quietTalksLessThanChatty() {
        fun talks(c: DjChattiness): Int {
            val s = DjTalkSchedule(c, Random(5))
            s.reset()
            return (1..120).count { s.onSongStarted() }
        }
        assertTrue(talks(DjChattiness.CHATTY) > talks(DjChattiness.NORMAL))
        assertTrue(talks(DjChattiness.NORMAL) > talks(DjChattiness.QUIET))
    }

    @Test
    fun vibes_coverAllMoodsBeforeRepeatingAndNeverStayPut() {
        val moods = listOf("chill", "romance", "feel good", "energy", "party", "focus")
        val cycle = DjVibeCycle(moods, Random(9))
        cycle.start("chill")
        val firstRound = List(5) { cycle.next() }
        assertEquals(moods.toSet() - "chill", firstRound.toSet())
        var current = cycle.current
        repeat(100) {
            val next = cycle.next()
            assertNotEquals(current, next)
            current = next
        }
    }
}
