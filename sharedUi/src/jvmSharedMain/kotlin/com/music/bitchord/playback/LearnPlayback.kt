package com.music.bitchord.playback

import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Learn controls' state: speed, pitch and a repeating section.
 *
 * Set from the lyric view and acted on by the playback service, so a loop keeps
 * running with the screen off. Nothing here is saved, and the service clears it
 * all when the song changes.
 */
object LearnPlayback {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 1.5f
    const val MIN_PITCH = -6
    const val MAX_PITCH = 6
    private const val SPEED_STEPS_PER_UNIT = 20f

    /** Times in the song, and the lyric lines they came from. */
    data class Loop(val startMs: Long, val endMs: Long, val firstLine: Int, val lastLine: Int)

    private val _speed = MutableStateFlow(1f)
    val speed = _speed.asStateFlow()

    private val _pitchSemitones = MutableStateFlow(0)
    val pitchSemitones = _pitchSemitones.asStateFlow()

    private val _loop = MutableStateFlow<Loop?>(null)
    val loop = _loop.asStateFlow()

    val isActive: Boolean
        get() = _speed.value != 1f || _pitchSemitones.value != 0 || _loop.value != null

    /** Snaps to the nearest 0.05, so a slider drag lands exactly on 1x. */
    fun setSpeed(value: Float) {
        val snapped = (value * SPEED_STEPS_PER_UNIT).roundToInt() / SPEED_STEPS_PER_UNIT
        _speed.value = snapped.coerceIn(MIN_SPEED, MAX_SPEED)
    }

    fun setPitch(semitones: Int) {
        _pitchSemitones.value = semitones.coerceIn(MIN_PITCH, MAX_PITCH)
    }

    fun setLoop(loop: Loop?) {
        _loop.value = loop
    }

    enum class TranslationUse { ON, OFF, SAME_LANGUAGE }

    /** What the lyrics view knows about translation for one song, for the learning stats. */
    data class TranslationReport(val trackId: String, val use: TranslationUse, val sourceLanguage: String?)

    private val _translation = MutableStateFlow<TranslationReport?>(null)

    fun reportTranslation(report: TranslationReport) {
        _translation.value = report
    }

    /** The latest report, if it is about [trackId]. */
    fun translationFor(trackId: String): TranslationReport? = _translation.value?.takeIf { it.trackId == trackId }

    /** A short line from a translated song, with its meaning, for the DJ to teach. */
    data class Phrase(val trackId: String, val original: String, val translated: String, val sourceLanguage: String)

    private val phrases = object : LinkedHashMap<String, Phrase>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Phrase>) = size > 8
    }

    @Synchronized
    fun reportPhrase(phrase: Phrase) {
        phrases[phrase.trackId] = phrase
    }

    @Synchronized
    fun phraseFor(trackId: String): Phrase? = phrases[trackId]

    private val _panelOpens = MutableStateFlow(0)
    /** Counts each time the Learn panel opens. */
    val panelOpens = _panelOpens.asStateFlow()

    fun notePanelOpened() {
        _panelOpens.value++
    }

    /** Back to normal playback. */
    fun turnOff() {
        _speed.value = 1f
        _pitchSemitones.value = 0
        _loop.value = null
    }
}
