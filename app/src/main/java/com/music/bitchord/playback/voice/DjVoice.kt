package com.music.bitchord.playback.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** A voice the DJ can speak with. */
interface DjVoice {
    /** One sentence to say, in [language] (a BCP-47 tag). */
    data class Chunk(val text: String, val language: String)

    /**
     * Says [chunks] in order with [gapMs] of silence between them, at [volume]
     * (0.5–1.5 of full). Returns once the last one has been said; cancelling
     * the caller stops the speech.
     */
    suspend fun speak(chunks: List<Chunk>, gapMs: Long, volume: Float)

    fun stop()

    fun release()
}

/** Speech on the media stream: the same volume the music uses, not the Assistant's. */
internal val DJ_SPEECH_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    .build()

/**
 * Which voice says what. Each sentence goes to the natural voice for its
 * language when one is downloaded, otherwise to the phone's text-to-speech, so
 * an English DJ can say a Spanish phrase in a Spanish voice.
 */
object DjVoices {
    private var android: AndroidTtsVoice? = null
    private val natural = HashMap<NaturalVoice, SherpaVoice>()
    private val active = mutableSetOf<DjVoice>()

    /**
     * Says [chunks] in order, each in the best voice for its language. Any one
     * voice failing is skipped rather than ending the break.
     */
    suspend fun speak(context: Context, chunks: List<DjVoice.Chunk>, gapMs: Long, volume: Float) {
        // Consecutive sentences for the same voice go together, so it can
        // synthesise the next while the current one plays.
        val runs = mutableListOf<Pair<DjVoice, MutableList<DjVoice.Chunk>>>()
        for (chunk in chunks) {
            val voice = voiceFor(context, chunk.language) ?: continue
            if (runs.lastOrNull()?.first === voice) runs.last().second += chunk else runs += voice to mutableListOf(chunk)
        }
        runs.forEachIndexed { index, (voice, run) ->
            if (index > 0) kotlinx.coroutines.delay(gapMs)
            active += voice
            try {
                voice.speak(run, gapMs, volume)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Throwable) {
                Log.w("DjVoices", "a voice failed: ${error.message}")
            } finally {
                active -= voice
            }
        }
    }

    private suspend fun voiceFor(context: Context, language: String): DjVoice? {
        NaturalVoices.voiceFor(context, language)?.let { voice ->
            return natural.getOrPut(voice) { SherpaVoice(context.applicationContext, voice) }
        }
        android?.let { return it }
        return AndroidTtsVoice.create(context)?.also { android = it }
    }

    /** Stops whatever is being said. */
    fun stop() {
        active.toList().forEach { it.stop() }
        android?.stop()
    }

    /** Frees every engine: the phone's TTS and the loaded neural model. */
    fun release() {
        stop()
        android?.release()
        android = null
        natural.clear()
        NaturalVoices.unload()
    }
}

/**
 * The phone's own text-to-speech, at its best installed voice for each
 * language: very-high quality first, then network voices, which sound far more
 * natural than the offline ones.
 */
class AndroidTtsVoice private constructor(private val tts: TextToSpeech) : DjVoice {
    private val ids = AtomicInteger()
    private val waiting = HashMap<String, CompletableDeferred<Unit>>()
    private val chosen = HashMap<String, Voice?>()

    init {
        tts.setAudioAttributes(DJ_SPEECH_ATTRIBUTES)
        tts.setSpeechRate(RATE)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
        })
    }

    private fun finish(id: String?) {
        synchronized(waiting) { waiting.remove(id) }?.complete(Unit)
    }

    override suspend fun speak(chunks: List<DjVoice.Chunk>, gapMs: Long, volume: Float) {
        if (chunks.isEmpty()) return
        val params = Bundle().apply {
            // Android TTS can't go past full; louder than 100% needs the natural voice.
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
        }
        var last: CompletableDeferred<Unit>? = null
        withContext(Dispatchers.Main) {
            chunks.forEachIndexed { index, chunk ->
                applyLanguage(Locale.forLanguageTag(normalize(chunk.language)))
                if (index > 0) tts.playSilentUtterance(gapMs, TextToSpeech.QUEUE_ADD, "gap-${ids.incrementAndGet()}")
                val id = "dj-${ids.incrementAndGet()}"
                val done = CompletableDeferred<Unit>()
                synchronized(waiting) { waiting[id] = done }
                if (tts.speak(chunk.text, TextToSpeech.QUEUE_ADD, params, id) != TextToSpeech.SUCCESS) {
                    finish(id)
                }
                last = done
            }
        }
        try {
            last?.await()
        } finally {
            if (last?.isCompleted != true) stop()
        }
    }

    private fun applyLanguage(locale: Locale) {
        val voice = chosen.getOrPut(locale.toLanguageTag()) { bestVoice(locale) }
        if (voice != null) {
            tts.voice = voice
        } else {
            tts.language = if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) locale else Locale.ENGLISH
        }
    }

    private fun bestVoice(locale: Locale): Voice? {
        val candidates = runCatching { tts.voices }.getOrNull().orEmpty().filter {
            it.locale.language == locale.language &&
                !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        fun score(voice: Voice): Int {
            val sameCountry = locale.country.isBlank() || voice.locale.country == locale.country
            return voice.quality * 10 + (if (voice.isNetworkConnectionRequired) 50 else 0) + (if (sameCountry) 20 else 0)
        }
        return candidates.maxByOrNull(::score).also { Log.i(TAG, "voice for $locale: ${it?.name} (q=${it?.quality})") }
    }

    override fun stop() {
        runCatching { tts.stop() }
        val pending = synchronized(waiting) { waiting.values.toList().also { waiting.clear() } }
        pending.forEach { it.complete(Unit) }
    }

    override fun release() {
        stop()
        runCatching { tts.shutdown() }
    }

    companion object {
        private const val TAG = "AndroidTtsVoice"
        /** A touch under normal speed: unhurried, like a host, not a reader. */
        private const val RATE = 0.97f

        private fun normalize(tag: String) = tag.replace("iw", "he").replace("jw", "jv")

        suspend fun create(context: Context): AndroidTtsVoice? = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                var engine: TextToSpeech? = null
                engine = TextToSpeech(context.applicationContext) { status ->
                    if (!continuation.isActive) return@TextToSpeech
                    if (status == TextToSpeech.SUCCESS && engine != null) {
                        continuation.resumeWith(Result.success(AndroidTtsVoice(engine!!)))
                    } else {
                        Log.w(TAG, "text-to-speech unavailable: $status")
                        runCatching { engine?.shutdown() }
                        continuation.resumeWith(Result.success(null))
                    }
                }
                continuation.invokeOnCancellation { runCatching { engine?.shutdown() } }
            }
        }
    }
}
