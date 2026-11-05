package com.music.bitchord.playback

import android.util.Log
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.ReplayPeriod
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * A Lyrelo DJ session, started from Home or Library: a mix that opens on what
 * the listener plays most, then every five to ten songs changes mood using
 * YouTube Music's mood playlists, with the DJ talking over each change.
 */
object DjSession {
    /** How the session reaches the player, which lives with the activity. */
    interface Host {
        suspend fun play(songs: List<Song>)
        suspend fun queueNext(songs: List<Song>)
    }

    private const val TAG = "DjSession"
    private val MOODS = listOf("chill", "romance", "feel good", "energy", "party", "focus")
    private const val MOOD_SONGS = 8

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var host: Host? = null
    private var job: Job? = null
    private var listenerName = ""
    private val known = mutableSetOf<String>()
    private var songsUntilChange = 0
    private var moodIndex = 0
    private var preparing = false
    private var lastSong: Song? = null

    /** The first song of the next mood, and what to say when it starts. */
    private var pending: Pending? = null

    private data class Pending(val firstId: String, val mood: String)

    val active: Boolean get() = host != null

    fun start(host: Host, name: String) {
        stop()
        this.host = host
        listenerName = name
        moodIndex = Random.nextInt(MOODS.size)
        songsUntilChange = nextGap()
        DjPresence.onStop = ::stop
        DjPresence.update { DjPresence.State(active = true, caption = "Lyrelo DJ is picking your mix…") }
        job = scope.launch {
            val songs = openingMix()
            if (songs.isEmpty()) {
                DjPresence.update { it.copy(caption = "Couldn't reach YouTube Music. Try again in a moment.") }
                return@launch
            }
            known += songs.map { it.videoId }
            host.play(songs)
            val first = songs.first()
            val greeting = "Hey ${listenerName.ifBlank { "there" }}, it's your Lyrelo DJ. " +
                "Let's kick off the vibe with some of your favorites. First up, ${first.title}, by ${first.artist}."
            LyreloDj.announce(greeting)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        host = null
        known.clear()
        pending = null
        preparing = false
        DjPresence.onStop = null
        DjPresence.update { DjPresence.State() }
    }

    /** Each sample of playback, once the song changes. Returns false when the session isn't running. */
    fun onSongStarted(song: Song): Boolean {
        val host = host ?: return false
        // Anything the listener picked themselves ends the mix.
        if (song.videoId !in known) {
            stop()
            return false
        }
        val previous = lastSong
        lastSong = song
        pending?.takeIf { it.firstId == song.videoId }?.let { change ->
            pending = null
            val intro = buildString {
                if (previous != null) append("That was ${previous.title}, by ${previous.artist}. ")
                append("Let's switch to something ${change.mood}. ")
                append("First up, ${song.title}, by ${song.artist}.")
            }
            scope.launch { LyreloDj.announce(intro, teachFrom = previous?.videoId) }
            return true
        }
        songsUntilChange--
        if (songsUntilChange <= 0 && !preparing) {
            preparing = true
            songsUntilChange = nextGap()
            scope.launch { prepareNextMood(host) }
        }
        return true
    }

    private suspend fun prepareNextMood(host: Host) {
        try {
            moodIndex = (moodIndex + 1) % MOODS.size
            val mood = MOODS[moodIndex]
            val songs = moodSongs(mood)
            if (songs.isEmpty() || this.host !== host) return
            known += songs.map { it.videoId }
            pending = Pending(songs.first().videoId, mood)
            host.queueNext(songs)
        } catch (error: Throwable) {
            Log.w(TAG, "couldn't line up the next mood: ${error.message}")
        } finally {
            preparing = false
        }
    }

    private suspend fun openingMix(): List<Song> {
        val favorites = runCatching { ListeningStats.summary(ReplayPeriod.ALL_TIME).songs.take(5).map { it.song } }
            .getOrDefault(emptyList())
        val seed = favorites.randomOrNull()
        if (seed != null) {
            val radio = YtMusicRepository.radio(seed.videoId).getOrNull().orEmpty()
            if (radio.isNotEmpty()) return (listOf(seed) + radio.filter { it.videoId != seed.videoId }).take(40)
        }
        return YtMusicRepository.quickPicks().getOrNull().orEmpty().shuffled().take(30)
    }

    private suspend fun moodSongs(mood: String): List<Song> {
        val sections = YtMusicRepository.moodAndGenres().getOrNull().orEmpty()
        val match = sections.flatMap { it.items }
            .firstOrNull { it.title.lowercase(Locale.ROOT).contains(mood) } ?: return emptyList()
        val shelves = YtMusicRepository.moodGenreShelves(match.browseId, match.params).getOrNull().orEmpty()
        val playlist = shelves.flatMap { it.items }.mapNotNull { it.browseId }.shuffled().firstOrNull()
            ?: return emptyList()
        return YtMusicRepository.allSongs(playlist).getOrNull().orEmpty()
            .filter { it.videoId !in known }
            .shuffled()
            .take(MOOD_SONGS)
    }

    private fun nextGap() = Random.nextInt(5, 11)

    /** The name the DJ greets the listener by. */
    fun displayName(nickname: String, accountName: String?): String =
        nickname.ifBlank { accountName.orEmpty() }.trim().substringBefore(' ')
}
