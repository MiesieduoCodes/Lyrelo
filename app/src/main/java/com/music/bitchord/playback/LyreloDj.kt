package com.music.bitchord.playback

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.music.bitchord.R
import com.music.bitchord.data.lyrics.LyricsTranslation
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.LearningStats
import com.music.bitchord.playback.dj.DjLinePicker
import com.music.bitchord.playback.dj.DjMoment
import com.music.bitchord.playback.dj.DjTalkSchedule
import com.music.bitchord.playback.voice.DjVoice
import com.music.bitchord.playback.voice.DjVoices
import com.music.bitchord.playback.voice.NaturalVoices
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The DJ's voice: short spoken breaks between songs.
 *
 * Lines come from `strings.xml` (`dj_line_*`), a different variant each time
 * ([DjLinePicker]), in the language the DJ speaks. Where that language has no
 * translation yet, the English line is machine-translated as a fallback.
 * Speech goes out on the media stream at full volume while [DjDucker] turns the
 * music down under it, so the DJ is clearly louder than the song.
 *
 * Outside a DJ mix it talks every few songs (Settings → Learning → Lyrelo DJ),
 * as often as the chattiness setting asks; inside one, [DjSession] decides.
 */
object LyreloDj {
    private const val TAG = "LyreloDj"
    private const val ENGLISH = "en"
    /** A voice left unused this long is released. */
    private const val IDLE_RELEASE_MS = 90_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var appContext: Context
    private val picker = DjLinePicker()
    private val schedule = DjTalkSchedule()
    private var speaking: Job? = null
    private var idleRelease: Job? = null

    private var lastSongId: String? = null
    private var streakMentionedOn: LocalDate? = null
    private var doneMentionedOn: LocalDate? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        schedule.reset(talkOnNext = true)
    }

    private val ready get() = this::appContext.isInitialized

    /** A fresh mix: lines may be said again, the streak mentioned again, the voice loaded early. */
    fun newSession() {
        picker.reset()
        streakMentionedOn = null
        if (ready) scope.launch { NaturalVoices.warmUp(appContext, speechLanguage) }
    }

    /** A short sample line in the picked voice, for Settings. */
    suspend fun preview() {
        if (!ready) return
        say(listOf(Spoken(appContext.resources.getStringArray(R.array.dj_line_next_song)[0].format("Despacito", "Luis Fonsi"), "en")), duck = true)
    }

    /** Called as each sample of playback arrives; acts only when the song changes. */
    fun onPlaying(song: Song) {
        if (song.videoId == lastSongId) return
        val previous = lastSongId
        lastSongId = song.videoId
        if (!ready) return
        // A DJ mix does its own talking, driven by the player itself.
        if (DjSession.active) return
        if (!AppSettings.djEnabled.value) return
        // A loop or a slowed song is someone studying; don't talk over them.
        if (LearnPlayback.isActive) return
        schedule.setChattiness(AppSettings.djChattiness.value)
        if (!schedule.onSongStarted()) return
        scope.launch { say(buildList {
            addAll(streak())
            previous?.let { addAll(phraseLesson(it)) }
            add(line(DjMoment.NEXT_SONG, R.array.dj_line_next_song, song.title, song.artist))
        }, duck = true) }
    }

    // --- What a DJ mix asks for ----------------------------------------------

    suspend fun greet(name: String, song: Song) {
        if (!ready) return
        val listener = name.ifBlank { appContext.getString(R.string.dj_listener_fallback) }
        say(buildList {
            add(line(DjMoment.INTRO, R.array.dj_line_intro, listener, song.title, song.artist))
            addAll(streak())
        }, duck = false)
    }

    suspend fun vibeChange(previous: Song?, mood: DjVibe, song: Song) {
        if (!ready) return
        say(buildList {
            add(line(DjMoment.VIBE_CHANGE, R.array.dj_line_vibe_change, previous?.title.orEmpty(), moodWord(mood)))
            previous?.let { addAll(phraseLesson(it.videoId)) }
            add(line(DjMoment.NEXT_SONG, R.array.dj_line_next_song, song.title, song.artist))
        }, duck = false)
    }

    suspend fun changeUp(mood: DjVibe, song: Song) {
        if (!ready) return
        say(listOf(
            line(DjMoment.CHANGE_UP, R.array.dj_line_change_up, moodWord(mood)),
            line(DjMoment.NEXT_SONG, R.array.dj_line_next_song, song.title, song.artist),
        ), duck = false)
    }

    /** A break inside a mix between mood changes: a phrase, then what's playing. */
    suspend fun between(previous: Song?, song: Song) {
        if (!ready) return
        say(buildList {
            addAll(streak())
            previous?.let { addAll(phraseLesson(it.videoId)) }
            add(line(DjMoment.NEXT_SONG, R.array.dj_line_next_song, song.title, song.artist))
        }, duck = true)
    }

    /** Stops talking now and lets the music back up. */
    fun hush() {
        speaking?.cancel()
        speaking = null
        DjVoices.stop()
        DjDucker.release()
        DjPresence.update { it.copy(speaking = false) }
    }

    /** Lets go of the voice engine and the memory it holds. */
    fun release() {
        hush()
        DjVoices.release()
    }

    // --- Lines -----------------------------------------------------------------

    /** One thing to say, and the language to say it in. */
    data class Spoken(val text: String, val language: String)

    private val nativeLanguage: String
        get() = AppSettings.translationLanguage.value.ifBlank { Locale.getDefault().language }

    /** The language the DJ talks in: the learning language, unless that's turned off. */
    private val speechLanguage: String
        get() {
            val learning = AppSettings.learningLanguage.value
            return if (AppSettings.djSpeaksLearningLanguage.value && learning.isNotBlank()) learning else nativeLanguage
        }

    private suspend fun line(moment: DjMoment, array: Int, vararg args: Any): Spoken =
        lineIn(speechLanguage, moment, array, *args)

    private suspend fun lineIn(language: String, moment: DjMoment, array: Int, vararg args: Any): Spoken {
        val base = appContext.resources.getStringArray(array)
        val index = picker.pick(moment, base.size)
        val english = base[index].format(*args)
        if (language.substringBefore('-').equals(ENGLISH, ignoreCase = true)) return Spoken(english, ENGLISH)
        // The resources in that language, if a translator has done them.
        val localized = localizedArray(language, array)
        if (localized != null && localized.size == base.size && !localized.contentEquals(base)) {
            return Spoken(localized[index].format(*args), language)
        }
        // No translation yet: fall back to the machine translator.
        val translated = runCatching { LyricsTranslation.translateText(english, language) }.getOrNull()
        return if (translated.isNullOrBlank()) Spoken(english, ENGLISH) else Spoken(translated, language)
    }

    private fun localizedArray(language: String, array: Int): Array<String>? = runCatching {
        val config = Configuration(appContext.resources.configuration)
        config.setLocale(Locale.forLanguageTag(language))
        appContext.createConfigurationContext(config).resources.getStringArray(array)
    }.getOrNull()

    private fun moodWord(mood: DjVibe): String {
        val words = appContext.resources.getStringArray(R.array.dj_mood_words)
        return words.getOrNull(DjVibe.MOODS.indexOf(mood)) ?: mood.moodKey.orEmpty()
    }

    private suspend fun streak(): List<Spoken> {
        val stats = LearningStats.snapshot.value
        val today = LocalDate.now()
        return when {
            stats.todayQualified && doneMentionedOn != today -> {
                doneMentionedOn = today
                streakMentionedOn = today
                listOf(line(DjMoment.STREAK_DONE, R.array.dj_line_streak_done, stats.streak))
            }
            stats.streak > 0 && streakMentionedOn != today -> {
                streakMentionedOn = today
                listOf(line(DjMoment.STREAK, R.array.dj_line_streak, stats.streak))
            }
            else -> emptyList()
        }
    }

    /**
     * A phrase from [songId]: said in the song's own language (in that
     * language's voice where one is installed), then what it means in the
     * listener's native language. Nothing if the song had no usable line.
     */
    private suspend fun phraseLesson(songId: String): List<Spoken> {
        val phrase = LearnPlayback.phraseFor(songId) ?: return emptyList()
        val native = nativeLanguage
        val songLanguage = phrase.sourceLanguage.ifBlank { AppSettings.learningLanguage.value.ifBlank { speechLanguage } }
        return listOf(
            lineIn(native, DjMoment.PHRASE_INTRO, R.array.dj_line_phrase_intro),
            Spoken(phrase.original, songLanguage),
            lineIn(native, DjMoment.PHRASE_MEANING, R.array.dj_line_phrase_meaning),
            Spoken(phrase.translated, native),
        )
    }

    // --- Speaking ----------------------------------------------------------------

    /**
     * Says [lines], each split into its sentences with a short gap between,
     * and shows them as captions. With [duck] the music goes down first; a mix
     * has already faded its new song in low, so it only needs bringing back.
     * Any failure leaves the music playing at full volume.
     */
    private suspend fun say(lines: List<Spoken>, duck: Boolean) {
        if (lines.isEmpty()) return
        speaking?.cancel()
        idleRelease?.cancel()
        val chunks = lines.flatMap { spoken ->
            spoken.text.split(SENTENCE_END).map { it.trim() }.filter { it.isNotEmpty() }
                .map { DjVoice.Chunk(it, spoken.language) }
        }
        val job = scope.launch {
            try {
                if (duck) DjDucker.duck()
                DjPresence.update { it.copy(speaking = true, caption = lines.joinToString(" ") { l -> l.text }, notice = false) }
                DjVoices.speak(appContext, chunks, gapMs = CHUNK_GAP_MS, volume = AppSettings.djVoiceVolume.value)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w(TAG, "speech failed: ${error.message}")
            } finally {
                DjDucker.release()
                DjPresence.update { it.copy(speaking = false) }
                scheduleIdleRelease()
            }
        }
        speaking = job
        job.join()
    }

    private fun scheduleIdleRelease() {
        idleRelease?.cancel()
        idleRelease = scope.launch {
            delay(IDLE_RELEASE_MS)
            if (!DjSession.active) DjVoices.release()
        }
    }

    private const val CHUNK_GAP_MS = 250L
    private val SENTENCE_END = Regex("(?<=[.!?…。！？])\\s+")
}
