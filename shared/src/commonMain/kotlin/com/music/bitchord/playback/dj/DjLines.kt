package com.music.bitchord.playback.dj

import kotlin.random.Random

/** The moments the DJ talks at. Each has its own set of line variants. */
enum class DjMoment { INTRO, NEXT_SONG, PHRASE_INTRO, PHRASE_MEANING, STREAK, STREAK_DONE, VIBE_CHANGE, CHANGE_UP, OUTRO }

/**
 * Picks the DJ's lines so it never sounds like a loop: never the same line
 * twice in a row, and no line again within a session until every other
 * variant for that moment has had its turn.
 *
 * Plain logic with an injectable [random], so it is unit-tested without a phone.
 */
class DjLinePicker(private val random: Random = Random.Default) {
    private val used = HashMap<DjMoment, MutableSet<Int>>()
    private val last = HashMap<DjMoment, Int>()

    /** An index into [count] variants for [moment]. */
    fun pick(moment: DjMoment, count: Int): Int {
        require(count > 0) { "no variants for $moment" }
        if (count == 1) return 0
        val usedHere = used.getOrPut(moment) { mutableSetOf() }
        if (usedHere.size >= count) {
            // Every variant has been said once: start a new round, but still
            // never repeat the one just said.
            usedHere.clear()
        }
        val previous = last[moment]
        val fresh = (0 until count).filter { it !in usedHere && it != previous }
        val choice = fresh.random(random)
        usedHere += choice
        last[moment] = choice
        return choice
    }

    fun reset() {
        used.clear()
        last.clear()
    }
}

/** How often the DJ talks between songs. */
enum class DjChattiness(val minGap: Int, val maxGap: Int) {
    CHATTY(1, 2),
    NORMAL(2, 4),
    QUIET(4, 6),
}

/**
 * Counts songs between the DJ's breaks. [onSongStarted] returns true when it
 * is time to talk; the next gap is drawn from the chattiness range.
 */
class DjTalkSchedule(
    private var chattiness: DjChattiness = DjChattiness.NORMAL,
    private val random: Random = Random.Default,
) {
    private var songsUntilTalk = 0

    /** The first song of a session is always announced. */
    fun reset(talkOnNext: Boolean = true) {
        songsUntilTalk = if (talkOnNext) 0 else nextGap()
    }

    fun setChattiness(value: DjChattiness) {
        if (value == chattiness) return
        chattiness = value
        songsUntilTalk = songsUntilTalk.coerceAtMost(value.maxGap)
    }

    fun onSongStarted(): Boolean {
        if (songsUntilTalk <= 0) {
            songsUntilTalk = nextGap()
            return true
        }
        songsUntilTalk--
        if (songsUntilTalk <= 0) {
            songsUntilTalk = nextGap()
            return true
        }
        return false
    }

    private fun nextGap() = random.nextInt(chattiness.minGap, chattiness.maxGap + 1)
}

/**
 * The moods a DJ mix moves through, in a shuffled order that doesn't revisit a
 * mood until all have played, and never repeats the one playing now.
 */
class DjVibeCycle(private val moods: List<String>, private val random: Random = Random.Default) {
    init {
        require(moods.isNotEmpty())
    }

    private val queue = ArrayDeque<String>()
    var current: String? = null
        private set

    /** Jumps to a specific mood, e.g. one the listener picked. */
    fun start(mood: String?) {
        queue.clear()
        current = mood
    }

    fun next(): String {
        if (queue.isEmpty()) {
            queue.addAll(moods.shuffled(random).filter { it != current })
            if (queue.isEmpty()) queue.addAll(moods)
        }
        val mood = queue.removeFirst()
        current = mood
        return mood
    }
}
