package com.music.bitchord.playback

import android.content.Context
import android.util.Log
import com.music.bitchord.R
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.stats.ListeningStats
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.playback.dj.DjTalkSchedule
import com.music.bitchord.playback.dj.DjVibeCycle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A Lyrelo DJ mix: it starts from a vibe the listener picks, then every five
 * to ten songs moves to a new mood from YouTube Music's mood playlists, with
 * the DJ talking over each change and, every few songs between, naming what's
 * next and teaching a phrase.
 *
 * It reaches the player through a [Host] it opens itself ([DjPlayerLink]), not
 * through the activity, so it carries on with the screen off and ends when
 * playback does. Every way a mix can fail to start ends in a caption and a
 * clean stop, never in a badge that says "picking" with nothing behind it.
 */
object DjSession {
    /** How the session reaches the player. */
    interface Host {
        suspend fun play(songs: List<Song>)
        suspend fun queueNext(songs: List<Song>)
        /** Puts [songs] next and moves straight to the first of them. */
        suspend fun playNow(songs: List<Song>)
        fun release()
    }

    private const val TAG = "DjSession"
    private const val MOOD_SONGS = 8
    /** Longest a mix may spend getting its first songs before it gives up and says so. */
    private const val START_TIMEOUT_MS = 15_000L
    /** How long a status caption stays up before the badge goes. */
    private const val NOTICE_MS = 6_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var appContext: Context? = null
    private var host: Host? = null
    private var job: Job? = null
    private var listenerName = ""
    private val known = mutableSetOf<String>()
    private var songsUntilChange = 0
    private var preparing = false
    private var lastSong: Song? = null
    private var vibes = DjVibeCycle(DjVibe.MOODS.map { it.name })
    private val talk = DjTalkSchedule()

    /**
     * The next mood's songs, and what to say when one of them starts. Any of
     * them, not just the first: with shuffle on, the player may start another.
     */
    private var pending: Pending? = null

    private data class Pending(val songIds: Set<String>, val mood: DjVibe, val changeUp: Boolean)

    /** Said over the first song, once it actually starts. */
    private var greetingPending = false

    val active: Boolean get() = job != null || host != null

    /** Starts a mix from [vibe]. Music starts as soon as the first songs are fetched. */
    fun start(context: Context, name: String, vibe: DjVibe) {
        stop(announce = false)
        val app = context.applicationContext
        appContext = app
        listenerName = name
        songsUntilChange = nextGap()
        vibes = DjVibeCycle(DjVibe.MOODS.map { it.name })
        vibes.start(vibe.takeIf { it.moodKey != null }?.name)
        talk.setChattiness(AppSettings.djChattiness.value)
        talk.reset(talkOnNext = false)
        LyreloDj.newSession()
        DjPresence.onStop = { stop() }
        DjPresence.onChangeVibe = ::changeVibe
        notice(app.getString(R.string.dj_caption_picking))
        job = scope.launch {
            val link = runCatching { DjPlayerLink.connect(app) }.getOrElse {
                Log.w(TAG, "couldn't reach the player: ${it.message}")
                fail(R.string.dj_caption_offline)
                return@launch
            }
            host = link
            if (vibe == DjVibe.LEARN && AppSettings.learningLanguage.value.isBlank()) {
                fail(R.string.dj_caption_no_learning_language)
                return@launch
            }
            val songs = withTimeoutOrNull(START_TIMEOUT_MS) {
                runCatching { openingMix(vibe) }.getOrDefault(emptyList())
            }.orEmpty()
            if (songs.isEmpty()) {
                fail(R.string.dj_caption_offline)
                return@launch
            }
            known += songs.map { it.videoId }
            greetingPending = true
            link.play(songs)
        }
    }

    /**
     * Ends the mix. With [announce], the DJ says a short goodbye and the
     * caption hands the queue back; either way the music is left at full volume.
     */
    fun stop(announce: Boolean = false) {
        val wasActive = active
        job?.cancel()
        job = null
        host?.release()
        host = null
        known.clear()
        pending = null
        preparing = false
        greetingPending = false
        lastSong = null
        DjPresence.onStop = null
        DjPresence.onChangeVibe = null
        DjDucker.reset()
        // Lets go of the voice engines: a loaded neural voice holds a few hundred MB.
        LyreloDj.release()
        if (announce && wasActive) {
            val context = appContext
            val caption = context?.getString(R.string.dj_caption_handed_back)
            DjPresence.update { DjPresence.State(active = true, caption = caption, notice = true) }
            scope.launch {
                delay(NOTICE_MS)
                if (!active) DjPresence.update { DjPresence.State() }
            }
        } else {
            DjPresence.update { DjPresence.State() }
        }
    }

    /**
     * Called by the player the moment a song starts. Returns false when the
     * session isn't running.
     */
    fun onSongStarted(song: Song): Boolean {
        val host = host ?: return false
        if (song.videoId == lastSong?.videoId) return true
        // Still fetching the mix: whatever the old queue does meanwhile isn't
        // the listener walking away from it.
        if (known.isEmpty()) return true
        // Something the listener picked themselves: the DJ steps aside.
        if (song.videoId !in known) {
            stop(announce = true)
            return false
        }
        val previous = lastSong
        lastSong = song
        // The first song is playing: the "picking" status has done its job.
        DjPresence.update { if (it.notice) it.copy(caption = null, notice = false) else it }
        if (greetingPending) {
            greetingPending = false
            DjDucker.riseUnderVoice()
            scope.launch { LyreloDj.greet(listenerName, song) }
            return true
        }
        pending?.takeIf { song.videoId in it.songIds }?.let { change ->
            pending = null
            DjDucker.riseUnderVoice()
            scope.launch {
                if (change.changeUp) {
                    LyreloDj.changeUp(change.mood, song)
                } else {
                    LyreloDj.vibeChange(previous, change.mood, song)
                }
            }
            return true
        }
        songsUntilChange--
        if (songsUntilChange <= 0 && !preparing) {
            preparing = true
            songsUntilChange = nextGap()
            scope.launch { prepareNextMood(host) }
        } else if (talk.onSongStarted()) {
            scope.launch { LyreloDj.between(previous, song) }
        }
        return true
    }

    private suspend fun prepareNextMood(host: Host) {
        try {
            val mood = DjVibe.valueOf(vibes.next())
            val songs = moodSongs(mood)
            if (songs.isEmpty() || this.host !== host) return
            known += songs.map { it.videoId }
            pending = Pending(songs.map { it.videoId }.toSet(), mood, changeUp = false)
            host.queueNext(songs)
        } catch (error: Throwable) {
            Log.w(TAG, "couldn't line up the next mood: ${error.message}")
        } finally {
            preparing = false
        }
    }

    /** "Change vibe": a new mood right now, with the DJ saying so. */
    fun changeVibe() {
        val host = host ?: return
        if (preparing) return
        preparing = true
        songsUntilChange = nextGap()
        appContext?.let { notice(it.getString(R.string.dj_caption_changing)) }
        scope.launch {
            try {
                val mood = DjVibe.valueOf(vibes.next())
                val songs = moodSongs(mood)
                if (this@DjSession.host !== host) return@launch
                if (songs.isEmpty()) {
                    appContext?.let { notice(it.getString(R.string.dj_caption_offline)) }
                    delay(NOTICE_MS)
                    DjPresence.update { if (it.notice) it.copy(caption = null, notice = false) else it }
                    return@launch
                }
                known += songs.map { it.videoId }
                pending = Pending(songs.map { it.videoId }.toSet(), mood, changeUp = true)
                host.playNow(songs)
            } catch (error: Throwable) {
                Log.w(TAG, "couldn't change the vibe: ${error.message}")
            } finally {
                preparing = false
            }
        }
    }

    private fun notice(text: String) {
        DjPresence.update { it.copy(active = true, caption = text, notice = true) }
    }

    private suspend fun fail(message: Int) {
        val context = appContext ?: return
        host?.release()
        host = null
        DjPresence.update { DjPresence.State(active = true, caption = context.getString(message), notice = true) }
        delay(NOTICE_MS)
        job = null
        DjPresence.onStop = null
        DjPresence.onChangeVibe = null
        DjPresence.update { DjPresence.State() }
    }

    private suspend fun openingMix(vibe: DjVibe): List<Song> = when {
        vibe == DjVibe.LEARN -> learnSongs()
        vibe.moodKey != null -> moodSongs(vibe).ifEmpty { favoritesMix() }
        else -> favoritesMix()
    }

    private suspend fun favoritesMix(): List<Song> {
        val favorites = runCatching { ListeningStats.summary(ReplayPeriod.ALL_TIME).songs.take(5).map { it.song } }
            .getOrDefault(emptyList())
        val seed = favorites.randomOrNull()
        if (seed != null) {
            val radio = YtMusicRepository.radio(seed.videoId).getOrNull().orEmpty()
            if (radio.isNotEmpty()) return (listOf(seed) + radio.filter { it.videoId != seed.videoId }).take(40)
        }
        return YtMusicRepository.quickPicks().getOrNull().orEmpty().shuffled().take(30)
    }

    /** Popular songs in the learning language, the same search Home's Practice tile uses. */
    private suspend fun learnSongs(): List<Song> {
        val learning = AppSettings.learningLanguage.value
        if (learning.isBlank()) return emptyList()
        val name = com.music.bitchord.data.lyrics.translationLanguageName(learning, Locale.ENGLISH)
        return YtMusicRepository.search("popular $name songs", SearchFilter.SONGS)
            .getOrNull().orEmpty()
            .mapNotNull { (it as? SearchResult.Track)?.song ?: (it as? SearchResult.TopTrack)?.song }
            .shuffled()
            .take(30)
    }

    private suspend fun moodSongs(mood: DjVibe): List<Song> {
        val key = mood.moodKey ?: return emptyList()
        val sections = YtMusicRepository.moodAndGenres().getOrNull().orEmpty()
        val match = sections.flatMap { it.items }
            .firstOrNull { it.title.lowercase(Locale.ROOT).contains(key) } ?: return emptyList()
        val shelves = YtMusicRepository.moodGenreShelves(match.browseId, match.params).getOrNull().orEmpty()
        val playlist = shelves.flatMap { it.items }.mapNotNull { it.browseId }.shuffled().firstOrNull()
            ?: return emptyList()
        return YtMusicRepository.allSongs(playlist).getOrNull().orEmpty()
            .filter { it.videoId !in known }
            .shuffled()
            .take(MOOD_SONGS)
    }

    private fun nextGap() = kotlin.random.Random.nextInt(5, 11)

    /** The name the DJ greets the listener by. */
    fun displayName(nickname: String, accountName: String?): String =
        nickname.ifBlank { accountName.orEmpty() }.trim().substringBefore(' ')
}
