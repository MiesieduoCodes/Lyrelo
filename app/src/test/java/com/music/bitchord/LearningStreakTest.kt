package com.music.bitchord

import com.music.bitchord.data.stats.LearningStreak
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStreakTest {
    // A Thursday, so the week it's in started on Monday the 2nd.
    private val today = LocalDate.of(2026, 11, 5)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test
    fun countsConsecutiveDaysIncludingToday() {
        assertEquals(3, LearningStreak.compute(days(0, 1, 2), today).current)
    }

    @Test
    fun todayNotYetDoneDoesNotBreakIt() {
        val result = LearningStreak.compute(days(1, 2), today)
        assertEquals(2, result.current)
        assertFalse(result.freezeUsedThisWeek)
    }

    @Test
    fun oneMissedDayAWeekIsFrozen() {
        val result = LearningStreak.compute(days(0, 2, 3), today)
        assertEquals(3, result.current)
        assertTrue(result.freezeUsedThisWeek)
    }

    @Test
    fun aSecondMissInTheSameWeekEndsIt() {
        assertEquals(1, LearningStreak.compute(days(0, 2, 3).minus(today.minusDays(2)) + today.minusDays(3), today).current)
        assertEquals(1, LearningStreak.compute(days(0, 3), today).current)
    }

    @Test
    fun eachWeekHasItsOwnFreeze() {
        // Missed Wednesday this week and Saturday the week before.
        val result = LearningStreak.compute(days(0, 1, 3, 4, 5, 7, 8), today)
        assertEquals(7, result.current)
    }

    @Test
    fun nothingRecordedIsZero() {
        assertEquals(0, LearningStreak.compute(emptySet(), today).current)
        assertEquals(0, LearningStreak.compute(days(5), today).current)
    }
}
