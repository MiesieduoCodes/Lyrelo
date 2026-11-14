package com.music.bitchord.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.music.bitchord.R
import com.music.bitchord.data.model.QueueTier
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.QueueCoordinator.asQueueEntry
import kotlinx.coroutines.guava.await

/** What the DJ mix starts from. */
enum class DjVibe(
    /** The word YouTube Music's mood list is searched for; null for the non-mood vibes. */
    val moodKey: String?,
    val label: Int,
) {
    FAVORITES(null, R.string.dj_vibe_favorites),
    CHILL("chill", R.string.dj_vibe_chill),
    ROMANCE("romance", R.string.dj_vibe_romance),
    FEEL_GOOD("feel good", R.string.dj_vibe_feel_good),
    ENERGY("energy", R.string.dj_vibe_energy),
    PARTY("party", R.string.dj_vibe_party),
    FOCUS("focus", R.string.dj_vibe_focus),
    LEARN(null, R.string.dj_vibe_learn),
    ;

    companion object {
        /** The moods a mix moves through, in the order of `dj_mood_words`. */
        val MOODS = listOf(CHILL, ROMANCE, FEEL_GOOD, ENERGY, PARTY, FOCUS)
    }
}

/**
 * The DJ's own line to the playback service: a [MediaController] it opens when
 * a mix starts and closes when the mix ends.
 *
 * It used to borrow the activity's controller, so a mix outlived the screen
 * that could play it: close or recreate the activity and the DJ kept its badge
 * with nothing behind it. Talking to the service directly, it hears every song
 * change with the app in the background and goes when the service does.
 */
class DjPlayerLink private constructor(private val controller: MediaController) : DjSession.Host {

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let { DjSession.onSongStarted(it.toSong()) }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            // A call, a pause, headphones pulled: the DJ stops mid-sentence
            // rather than talking into silence or out of the phone's speaker.
            // (Not isPlaying: that also dips between songs, right when the DJ
            // starts talking.)
            if (!playWhenReady) LyreloDj.hush()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Playback stopped or ran out: the mix is over.
            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                DjSession.stop()
            }
        }
    }

    init {
        controller.addListener(listener)
    }

    override suspend fun play(songs: List<Song>) {
        controller.playSongs(songs, 0)
        controller.play()
    }

    override suspend fun queueNext(songs: List<Song>) {
        controller.addMediaItems(
            controller.currentMediaItemIndex + 1,
            songs.map { it.asQueueEntry(QueueTier.USER_QUEUE).toMediaItem() },
        )
    }

    override suspend fun playNow(songs: List<Song>) {
        val at = controller.currentMediaItemIndex + 1
        controller.addMediaItems(at, songs.map { it.asQueueEntry(QueueTier.USER_QUEUE).toMediaItem() })
        // Straight to it by index, so shuffle can't pick a different song.
        controller.seekTo(at, 0L)
        controller.play()
    }

    override fun release() {
        controller.removeListener(listener)
        controller.release()
    }

    companion object {
        suspend fun connect(context: Context): DjPlayerLink {
            val app = context.applicationContext
            val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
            return DjPlayerLink(MediaController.Builder(app, token).buildAsync().await())
        }
    }
}
