package com.music.bitchord.data.stats

import android.content.Context
import android.net.Uri
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.LearnPlayback
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One day of learning, keyed by its ISO date in [LearningFile.days]. */
@Serializable
data class LearningDay(
    /** Listening with translation on. */
    val translatedMs: Long = 0,
    /** Listening to songs whose lyrics were detected as the learning language. */
    val learningLanguageMs: Long = 0,
    /** Songs heard with translation on, by video id. */
    val songs: Set<String> = emptySet(),
    val linesLooped: Int = 0,
    /** The Learn panel was opened, or its speed, pitch or loop used. */
    val learnUsed: Boolean = false,
) {
    val qualifies: Boolean get() = learnUsed || translatedMs >= QUALIFYING_MS

    fun mergedWith(other: LearningDay) = LearningDay(
        translatedMs = maxOf(translatedMs, other.translatedMs),
        learningLanguageMs = maxOf(learningLanguageMs, other.learningLanguageMs),
        songs = songs + other.songs,
        linesLooped = maxOf(linesLooped, other.linesLooped),
        learnUsed = learnUsed || other.learnUsed,
    )

    companion object {
        /** Ten minutes with translation on is a day of learning. */
        const val QUALIFYING_MS = 10 * 60 * 1000L
    }
}

@Serializable
data class LearningFile(
    val app: String = APP_TAG,
    val version: Int = 1,
    val days: Map<String, LearningDay> = emptyMap(),
) {
    companion object {
        const val APP_TAG = "lyrelo-learning"
    }
}

/** What Home and Replay show. */
data class LearningSnapshot(
    val streak: Int = 0,
    val todayQualified: Boolean = false,
    val todayTranslatedMs: Long = 0,
    val freezeUsedThisWeek: Boolean = false,
    val weekLearningLanguageMs: Long = 0,
    val weekSongs: Int = 0,
    val weekLinesLooped: Int = 0,
)

/**
 * The daily streak, with one free missed day per week.
 *
 * Today doesn't break a streak until it is over. Before that, each missed day is
 * covered by its week's one freeze, as long as there is an earlier day of
 * learning for it to join up with. A second miss in the same week ends it.
 */
object LearningStreak {
    data class Result(val current: Int, val freezeUsedThisWeek: Boolean)

    fun compute(qualified: Set<LocalDate>, today: LocalDate): Result {
        val earliest = qualified.minOrNull() ?: return Result(0, false)
        var day = if (today in qualified) today else today.minusDays(1)
        var current = 0
        val frozenWeeks = mutableSetOf<LocalDate>()
        while (!day.isBefore(earliest)) {
            if (day in qualified) {
                current++
            } else {
                val week = weekOf(day)
                if (week in frozenWeeks) break
                frozenWeeks += week
            }
            day = day.minusDays(1)
        }
        return Result(current, weekOf(today) in frozenWeeks && current > 0)
    }

    fun weekOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/**
 * Learning stats, kept in one small file on the phone. No account and no server.
 *
 * Fed by [ListeningRecorder] while music plays and by the playback service when the
 * Learn controls are used. Written in the same way as [ListeningStats]: in memory
 * between samples, flushed to disk on its own IO scope.
 */
object LearningStats {
    private const val DIRECTORY = "learning"
    private const val FILE = "learning_stats.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private val lock = Any()

    private var file: File? = null
    private var days: Map<String, LearningDay> = emptyMap()
    private var dirty = false

    private val _snapshot = MutableStateFlow(LearningSnapshot())
    val snapshot: StateFlow<LearningSnapshot> = _snapshot.asStateFlow()

    fun init(context: Context) {
        val target = File(File(context.filesDir, DIRECTORY), FILE)
        file = target
        writer.launch {
            val loaded = runCatching {
                json.decodeFromString(LearningFile.serializer(), target.readText()).days
            }.getOrDefault(emptyMap())
            synchronized(lock) { days = loaded + days }
            publish()
        }
    }

    /** Adds [playedMs] of the song playing now. */
    fun recordListening(trackId: String, playedMs: Long) {
        if (playedMs <= 0L || trackId.isBlank()) return
        val report = LearnPlayback.translationFor(trackId)
        // Translation is on by default, so a song counts unless it was turned off
        // for this song or the song is already in the native language.
        val translated = report?.use != LearnPlayback.TranslationUse.OFF &&
            report?.use != LearnPlayback.TranslationUse.SAME_LANGUAGE
        if (!translated) return
        val inLearningLanguage = report?.sourceLanguage?.let { sameLanguage(it, AppSettings.learningLanguage.value) } == true
        update { day ->
            day.copy(
                translatedMs = day.translatedMs + playedMs,
                learningLanguageMs = day.learningLanguageMs + if (inLearningLanguage) playedMs else 0L,
                songs = day.songs + trackId,
            )
        }
    }

    fun markLearnUsed() {
        update { it.copy(learnUsed = true) }
        flush()
    }

    fun recordLoop(lines: Int) {
        update { it.copy(linesLooped = it.linesLooped + lines.coerceAtLeast(1), learnUsed = true) }
        flush()
    }

    private fun update(change: (LearningDay) -> LearningDay) {
        val key = LocalDate.now().toString()
        synchronized(lock) {
            days = days + (key to change(days[key] ?: LearningDay()))
            dirty = true
        }
        publish()
    }

    fun flush() {
        val target = file ?: return
        val text = synchronized(lock) {
            if (!dirty) return
            dirty = false
            json.encodeToString(LearningFile.serializer(), LearningFile(days = days))
        }
        writer.launch {
            writeLock.withLock {
                target.parentFile?.mkdirs()
                val temp = File(target.parentFile, "$FILE.tmp")
                temp.writeText(text)
                if (!temp.renameTo(target)) {
                    target.delete()
                    temp.renameTo(target)
                }
            }
        }
    }

    private fun publish() {
        val today = LocalDate.now()
        val all = synchronized(lock) { days }
        val parsed = all.mapNotNull { (key, day) -> runCatching { LocalDate.parse(key) }.getOrNull()?.let { it to day } }
        val streak = LearningStreak.compute(parsed.filter { it.second.qualifies }.map { it.first }.toSet(), today)
        val weekStart = LearningStreak.weekOf(today)
        val week = parsed.filter { !it.first.isBefore(weekStart) && !it.first.isAfter(today) }.map { it.second }
        val todayDay = all[today.toString()] ?: LearningDay()
        _snapshot.value = LearningSnapshot(
            streak = streak.current,
            todayQualified = todayDay.qualifies,
            todayTranslatedMs = todayDay.translatedMs,
            freezeUsedThisWeek = streak.freezeUsedThisWeek,
            weekLearningLanguageMs = week.sumOf { it.learningLanguageMs },
            weekSongs = week.flatMap { it.songs }.toSet().size,
            weekLinesLooped = week.sumOf { it.linesLooped },
        )
    }

    /** Writes every day to [target], a document the user picked. Returns how many days. */
    suspend fun exportTo(context: Context, target: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = synchronized(lock) { days }
            val text = json.encodeToString(LearningFile.serializer(), LearningFile(days = snapshot))
            context.contentResolver.openOutputStream(target, "wt")?.use { it.write(text.toByteArray()) }
                ?: error("Couldn't open that file for writing")
            snapshot.size
        }
    }

    /**
     * Adds the days in [source] to this phone's. A day on both keeps the larger
     * numbers, so importing the same file twice changes nothing.
     */
    suspend fun importFrom(context: Context, source: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(source)?.use { it.readBytes().decodeToString() }
                ?: error("Couldn't open that file")
            val incoming = runCatching { json.decodeFromString(LearningFile.serializer(), text) }
                .getOrElse { error("That doesn't look like a Lyrelo learning stats file") }
            require(incoming.app == LearningFile.APP_TAG) { "That doesn't look like a Lyrelo learning stats file" }
            synchronized(lock) {
                val merged = days.toMutableMap()
                incoming.days.forEach { (key, day) ->
                    merged[key] = merged[key]?.mergedWith(day) ?: day
                }
                days = merged
                dirty = true
            }
            publish()
            flush()
            incoming.days.size
        }
    }

    private fun sameLanguage(a: String, b: String): Boolean {
        fun base(tag: String) = tag.trim().lowercase().substringBefore('-').let {
            when (it) {
                "iw" -> "he"
                "jw" -> "jv"
                else -> it
            }
        }
        return a.isNotBlank() && b.isNotBlank() && base(a) == base(b)
    }
}
