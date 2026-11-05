package com.music.bitchord.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.music.bitchord.data.lyrics.LyricsTranslation
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.LearningStats
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The DJ: a short spoken break between songs, in the phone's own text-to-speech
 * voice. No account, no server and no cost: the voice runs on the phone. The
 * only network use is the app's existing translator, to say the lines in the
 * learning language.
 *
 * Every few songs it can say the next song, teach a phrase from the song that
 * just ended, and mention the learning streak. The music ducks under the voice
 * through audio focus, which the player already answers.
 */
object LyreloDj {
    private const val TAG = "LyreloDj"
    /** Talks on the first song, then every this many songs. */
    private const val TALK_EVERY = 3
    private const val ENGLISH = "en"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var appContext: Context
    private var tts: TextToSpeech? = null
    private var ready = false
    private var focusRequest: AudioFocusRequest? = null

    private var lastSongId: String? = null
    private var songsSinceTalk = TALK_EVERY
    private var streakMentionedOn: LocalDate? = null
    private var doneMentionedOn: LocalDate? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Called as each sample of playback arrives; acts only when the song changes. */
    fun onPlaying(song: Song) {
        if (song.videoId == lastSongId) return
        val previous = lastSongId
        lastSongId = song.videoId
        if (!this::appContext.isInitialized) return
        // A DJ session does its own talking.
        if (DjSession.onSongStarted(song)) return
        if (!AppSettings.djEnabled.value) return
        // A loop or a slowed song is someone studying; don't talk over them.
        if (LearnPlayback.isActive) return
        songsSinceTalk++
        if (songsSinceTalk < TALK_EVERY) return
        songsSinceTalk = 0
        scope.launch { talk(song, previous) }
    }

    private suspend fun talk(song: Song, previousId: String?) {
        announce("This is ${song.title}, by ${song.artist}.", teachFrom = previousId)
    }

    /**
     * Says [english] (translated as the settings ask), after the streak and a
     * phrase from [teachFrom] when there is one. Shown as captions on the player.
     */
    suspend fun announce(english: String, teachFrom: String? = null) {
        if (!this::appContext.isInitialized) return
        val native = AppSettings.translationLanguage.value.ifBlank { Locale.getDefault().language }
        val learning = AppSettings.learningLanguage.value
        val speech = if (AppSettings.djSpeaksLearningLanguage.value && learning.isNotBlank()) learning else native

        val lines = mutableListOf<Pair<String, String>>()
        streakLine()?.let { lines += say(it, speech) }
        teachFrom?.let { LearnPlayback.phraseFor(it) }?.let { phrase ->
            lines += say("Here's a phrase from that song.", speech)
            lines += phrase.original to phrase.sourceLanguage.ifBlank { learning.ifBlank { speech } }
            lines += say("It means", speech)
            lines += phrase.translated to native
        }
        lines += say(english, speech)
        DjPresence.update { it.copy(speaking = true, caption = lines.joinToString(" ") { line -> line.first }) }
        speak(lines)
    }

    private fun streakLine(): String? {
        val stats = LearningStats.snapshot.value
        val today = LocalDate.now()
        return when {
            stats.todayQualified && doneMentionedOn != today -> {
                doneMentionedOn = today
                streakMentionedOn = today
                "Nice work, that's your learning done for today. Your streak is ${stats.streak} days."
            }
            stats.streak > 0 && streakMentionedOn != today -> {
                streakMentionedOn = today
                "You're on a ${stats.streak} day learning streak. Let's keep it going."
            }
            else -> null
        }
    }

    /** An English line, translated into [language] when that isn't English. */
    private suspend fun say(english: String, language: String): Pair<String, String> {
        if (language.substringBefore('-').equals(ENGLISH, ignoreCase = true)) return english to ENGLISH
        val translated = runCatching { LyricsTranslation.translateText(english, language) }.getOrNull()
        return if (translated.isNullOrBlank()) english to ENGLISH else translated to language
    }

    private suspend fun speak(lines: List<Pair<String, String>>) {
        val engine = engine() ?: run {
            DjPresence.update { it.copy(speaking = false) }
            return
        }
        if (lines.isEmpty()) return
        requestDuck()
        // A touch lower and unhurried reads as a person talking rather than a
        // machine reading; short pauses between lines do the rest.
        engine.setPitch(DJ_PITCH)
        engine.setSpeechRate(DJ_RATE)
        lines.forEachIndexed { index, (text, language) ->
            val locale = Locale.forLanguageTag(language.replace("iw", "he").replace("jw", "jv"))
            val voice = voiceFor(engine, locale)
            if (voice != null) {
                engine.voice = voice
            } else {
                val available = engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
                engine.language = if (available) locale else Locale.ENGLISH
            }
            if (index > 0) engine.playSilentUtterance(LINE_PAUSE_MS, TextToSpeech.QUEUE_ADD, "dj-pause-$index")
            val id = if (index == lines.lastIndex) "dj-last" else "dj-$index"
            engine.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        }
    }

    private val chosenVoices = HashMap<String, android.speech.tts.Voice?>()

    /**
     * The best male voice the phone has for [locale]: a known male voice of the
     * engine if there is one, then the highest quality, preferring the network
     * voices, which sound far more natural than the offline ones.
     */
    private fun voiceFor(engine: TextToSpeech, locale: Locale): android.speech.tts.Voice? =
        chosenVoices.getOrPut(locale.toLanguageTag()) {
            val candidates = runCatching { engine.voices }.getOrNull().orEmpty().filter { voice ->
                voice.locale.language == locale.language &&
                    !voice.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
            }
            fun score(voice: android.speech.tts.Voice): Int {
                val name = voice.name.lowercase(Locale.ROOT)
                val male = MALE_VOICE_HINTS.any { name.contains(it) } ||
                    (name.contains("male") && !name.contains("female"))
                val sameCountry = locale.country.isBlank() || voice.locale.country == locale.country
                return (if (male) 1_000 else 0) + voice.quality +
                    (if (voice.isNetworkConnectionRequired) 50 else 0) + (if (sameCountry) 20 else 0)
            }
            candidates.maxByOrNull(::score).also { Log.i(TAG, "voice for $locale: ${it?.name}") }
        }

    private suspend fun engine(): TextToSpeech? {
        tts?.takeIf { ready }?.let { return it }
        return withContext(Dispatchers.Main) {
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                var created: TextToSpeech? = null
                created = TextToSpeech(appContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ready = true
                        created?.setAudioAttributes(SPEECH)
                        created?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) = Unit
                            override fun onDone(utteranceId: String?) {
                                if (utteranceId == "dj-last") finished()
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) = finished()
                        })
                        tts = created
                        if (continuation.isActive) continuation.resumeWith(Result.success(created))
                    } else {
                        Log.w(TAG, "text-to-speech unavailable: $status")
                        if (continuation.isActive) continuation.resumeWith(Result.success(null))
                    }
                }
            }
        }
    }

    private fun finished() {
        releaseDuck()
        DjPresence.update { it.copy(speaking = false) }
    }

    private fun requestDuck() {
        val audio = appContext.getSystemService(AudioManager::class.java) ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(SPEECH)
            .build()
        focusRequest = request
        audio.requestAudioFocus(request)
    }

    private fun releaseDuck() {
        val audio = appContext.getSystemService(AudioManager::class.java) ?: return
        focusRequest?.let(audio::abandonAudioFocusRequest)
        focusRequest = null
    }

    private const val DJ_PITCH = 0.92f
    private const val DJ_RATE = 0.98f
    private const val LINE_PAUSE_MS = 350L

    /**
     * Google's text-to-speech names its voices by code rather than by gender.
     * These are its male ones, across the languages Lyrelo is most used with.
     */
    private val MALE_VOICE_HINTS = listOf(
        "-iol-", "-iom-", "-tpd-", "-gbb-", "-gbd-", "-rjs-", "-aud-", "-end-",
        "-eed-", "-esd-", "-frd-", "-frb-", "-deg-", "-rud-", "-ruf-", "-itd-", "-itc-",
        "-jac-", "-jad-", "-koc-", "-kod-", "-ptd-", "-ccd-", "-cce-", "-tre-", "-trf-",
    )

    private val SPEECH: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
}
