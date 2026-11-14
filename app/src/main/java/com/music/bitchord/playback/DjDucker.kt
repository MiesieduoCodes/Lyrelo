package com.music.bitchord.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Turns the music down under the DJ's voice and back up after it.
 *
 * The voice plays on the media stream at full volume, so the music has to make
 * room rather than the voice shouting over it: down to [UNDER_VOICE] in
 * [DOWN_MS], back to full in [UP_MS]. The playback service plugs its player in
 * with [attach]; with nothing attached every call is a no-op, so the DJ can
 * never leave music stuck quiet in a player that has gone away.
 */
object DjDucker {
    /** The music's level while the DJ talks: well under a voice at full volume. */
    const val UNDER_VOICE = 0.12f
    const val DOWN_MS = 300L
    const val UP_MS = 600L
    private const val STEP_MS = 20L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var setVolume: ((Float) -> Unit)? = null
    private var fade: Job? = null

    /** The level last set, so a fade starts from where the music actually is. */
    @Volatile
    var level = 1f
        private set

    fun attach(setter: (Float) -> Unit) {
        setVolume = setter
        level = 1f
    }

    fun detach() {
        fade?.cancel()
        setVolume?.invoke(1f)
        setVolume = null
        level = 1f
    }

    /** Ducks under the voice. */
    fun duck() = fadeTo(UNDER_VOICE, DOWN_MS)

    /** Brings the music back to full. */
    fun release() = fadeTo(1f, UP_MS)

    /** A song the DJ is introducing: from silence up to the level under the voice. */
    fun riseUnderVoice() {
        set(0f)
        fadeTo(UNDER_VOICE, DOWN_MS * 2)
    }

    /** Back to full at once, e.g. when the mix ends. */
    fun reset() {
        fade?.cancel()
        set(1f)
    }

    fun fadeTo(target: Float, durationMs: Long) {
        if (setVolume == null) return
        fade?.cancel()
        val from = level
        fade = scope.launch {
            val steps = (durationMs / STEP_MS).coerceAtLeast(1)
            for (step in 1..steps) {
                val t = step.toFloat() / steps
                // Smoothstep, so it moves like a fader rather than a ramp.
                set(from + (target - from) * (t * t * (3f - 2f * t)))
                delay(STEP_MS)
            }
            set(target)
        }
    }

    private fun set(value: Float) {
        level = value.coerceIn(0f, 1f)
        setVolume?.invoke(level)
    }
}
