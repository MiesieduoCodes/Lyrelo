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

    /** Back to normal playback. */
    fun turnOff() {
        _speed.value = 1f
        _pitchSemitones.value = 0
        _loop.value = null
    }
}
