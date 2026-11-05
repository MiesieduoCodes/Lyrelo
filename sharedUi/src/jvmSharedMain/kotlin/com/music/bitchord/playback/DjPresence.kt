package com.music.bitchord.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the player shows of a Lyrelo DJ session: whether one is running, and
 * what the DJ is saying right now. Written by the app's DJ, read by the player.
 */
object DjPresence {
    data class State(val active: Boolean = false, val speaking: Boolean = false, val caption: String? = null)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    /** Set by the app: ends the session when the player's DJ badge is closed. */
    @Volatile
    var onStop: (() -> Unit)? = null

    /** Set by the app: the badge's "Change the vibe" button. */
    @Volatile
    var onChangeVibe: (() -> Unit)? = null

    fun update(change: (State) -> State) {
        _state.value = change(_state.value)
    }
}
