package com.music.bitchord.playback.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.music.bitchord.data.settings.AppSettings
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

/**
 * A downloadable model package from sherpa-onnx's official releases. Size and
 * SHA-256 are pinned: a download that doesn't match both is thrown away.
 *
 * Licences (see NOTICE): Kokoro-82M, Apache 2.0. Piper voices: the model is
 * MIT; ru_RU-dmitri was trained on a CC0 dataset, kk_KZ-issai on CC-BY-4.0
 * (credit IS2AI). Not shipped: ru_RU-irina (dataset licence unknown),
 * ru_RU-ruslan and ru_RU-denis (non-commercial).
 */
enum class VoicePack(
    val archive: String,
    val bytes: Long,
    val sha256: String,
    val kind: Kind,
    val modelFile: String,
) {
    KOKORO(
        archive = "kokoro-int8-multi-lang-v1_0",
        bytes = 132_353_410,
        sha256 = "72c347230107ef8a31098ec4d468a605244beef5f1ed00db97efa13518800653",
        kind = Kind.KOKORO,
        modelFile = "model.int8.onnx",
    ),
    RU_DMITRI(
        archive = "vits-piper-ru_RU-dmitri-medium-int8",
        bytes = 21_129_441,
        sha256 = "7636793307f634ce54c6e65528a91a61683114f1a6635a08caf64ba6c54e6a63",
        kind = Kind.VITS,
        modelFile = "ru_RU-dmitri-medium.onnx",
    ),
    KK_ISSAI(
        archive = "vits-piper-kk_KZ-issai-high-int8",
        bytes = 35_999_225,
        sha256 = "72b80fa2077d3371f42b957287bc89a70ad4abf1201aef9e01d0650a94aaa13e",
        kind = Kind.VITS,
        modelFile = "kk_KZ-issai-high.onnx",
    ),
    ;

    enum class Kind { KOKORO, VITS }

    val url get() = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$archive.tar.bz2"
    val megabytes get() = (bytes / 1_000_000L).toInt()
}

/** One voice the DJ can use, inside a [VoicePack]. */
enum class NaturalVoice(val pack: VoicePack, val speakerId: Int, val language: String, val female: Boolean) {
    // Kokoro v1.0 speaker ids, from sherpa-onnx's model docs.
    AF_HEART(VoicePack.KOKORO, 3, "en", female = true),
    AF_BELLA(VoicePack.KOKORO, 2, "en", female = true),
    AM_MICHAEL(VoicePack.KOKORO, 16, "en", female = false),
    AM_FENRIR(VoicePack.KOKORO, 14, "en", female = false),
    RU_DMITRI(VoicePack.RU_DMITRI, 0, "ru", female = false),
    KK_ISSAI(VoicePack.KK_ISSAI, 0, "kk", female = false),
    ;

    companion object {
        val DEFAULT = AF_HEART
        val ENGLISH = listOf(AF_HEART, AF_BELLA, AM_MICHAEL, AM_FENRIR)
    }
}

/** Downloads, stores and runs the natural voices. */
object NaturalVoices {
    private const val TAG = "NaturalVoices"

    sealed interface PackState {
        data object NotInstalled : PackState
        data class Downloading(val fraction: Float) : PackState
        data object Installing : PackState
        data object Installed : PackState
        data class Failed(val message: String) : PackState
    }

    private val states = VoicePack.entries.associateWith { MutableStateFlow<PackState>(PackState.NotInstalled) }
    private var checked = false

    fun state(pack: VoicePack): StateFlow<PackState> = states.getValue(pack).asStateFlow()

    private val downloadScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private val downloads = HashMap<VoicePack, kotlinx.coroutines.Job>()

    /** Starts [pack] downloading in the background; it carries on when the screen that asked closes. */
    fun startDownload(context: Context, pack: VoicePack) {
        if (downloads[pack]?.isActive == true) return
        val app = context.applicationContext
        downloads[pack] = downloadScope.launch { download(app, pack) }
    }

    fun cancelDownload(pack: VoicePack) {
        downloads.remove(pack)?.cancel()
    }

    private fun root(context: Context) = File(context.filesDir, "dj-voices")
    private fun dir(context: Context, pack: VoicePack) = File(root(context), pack.archive)

    fun isInstalled(context: Context, pack: VoicePack): Boolean =
        File(dir(context, pack), pack.modelFile).isFile && File(dir(context, pack), "tokens.txt").isFile

    /** Reads what's on disk once, so the states start out right. */
    fun refresh(context: Context) {
        VoicePack.entries.forEach { pack ->
            val flow = states.getValue(pack)
            if (flow.value !is PackState.Downloading && flow.value !is PackState.Installing) {
                flow.value = if (isInstalled(context, pack)) PackState.Installed else PackState.NotInstalled
            }
        }
        checked = true
    }

    /**
     * Downloads and unpacks [pack]. Cancel the calling coroutine to cancel the
     * download; nothing half-finished is ever left where the DJ would load it.
     */
    suspend fun download(context: Context, pack: VoicePack) = withContext(Dispatchers.IO) {
        val flow = states.getValue(pack)
        val root = root(context).apply { mkdirs() }
        val partial = File(root, "${pack.archive}.tar.bz2.part")
        val staging = File(root, "${pack.archive}.staging")
        try {
            flow.value = PackState.Downloading(0f)
            val connection = (URL(pack.url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        received += read
                        flow.value = PackState.Downloading((received.toFloat() / pack.bytes).coerceIn(0f, 1f))
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            check(received == pack.bytes && hash == pack.sha256) {
                "download didn't match the published file ($received bytes)"
            }
            flow.value = PackState.Installing
            staging.deleteRecursively()
            unpack(partial, staging, pack.archive)
            val target = dir(context, pack)
            target.deleteRecursively()
            check(File(staging, pack.archive).renameTo(target)) { "couldn't move the voice into place" }
            flow.value = PackState.Installed
        } catch (error: Throwable) {
            flow.value = if (error is kotlinx.coroutines.CancellationException) {
                PackState.NotInstalled
            } else {
                Log.w(TAG, "download of ${pack.archive} failed", error)
                PackState.Failed(error.message ?: "download failed")
            }
            if (error is kotlinx.coroutines.CancellationException) throw error
        } finally {
            partial.delete()
            staging.deleteRecursively()
        }
    }

    /** Removes [pack] from the phone. */
    fun delete(context: Context, pack: VoicePack) {
        if (loaded?.pack == pack) unload()
        dir(context, pack).deleteRecursively()
        states.getValue(pack).value = PackState.NotInstalled
    }

    private fun unpack(archive: File, into: File, top: String) {
        TarArchiveInputStream(BZip2CompressorInputStream(BufferedInputStream(archive.inputStream()))).use { tar ->
            while (true) {
                val entry = tar.nextEntry ?: break
                val out = File(into, entry.name).canonicalFile
                // Nothing may land outside the folder it was unpacked into.
                check(out.path.startsWith(File(into, top).canonicalPath)) { "unexpected path ${entry.name}" }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { tar.copyTo(it) }
                }
            }
        }
    }

    // --- Running ------------------------------------------------------------------

    /** The English voice picked in Settings. */
    fun selectedEnglish(): NaturalVoice =
        runCatching { NaturalVoice.valueOf(AppSettings.djNaturalVoice.value) }.getOrDefault(NaturalVoice.DEFAULT)

    /** The installed natural voice for [language], or null to use the phone's voice. */
    fun voiceFor(context: Context, language: String): NaturalVoice? {
        if (!checked) refresh(context)
        val base = language.substringBefore('-').lowercase()
        val voice = when (base) {
            "en" -> selectedEnglish()
            "ru" -> NaturalVoice.RU_DMITRI
            "kk" -> NaturalVoice.KK_ISSAI
            else -> return null
        }
        return voice.takeIf { isInstalled(context, it.pack) }
    }

    private class Loaded(val pack: VoicePack, val tts: OfflineTts)

    @Volatile
    private var loaded: Loaded? = null

    /** The engine for [pack], loading it (and dropping any other) if needed. Slow: call off the main thread. */
    @Synchronized
    private fun engine(context: Context, pack: VoicePack): OfflineTts {
        loaded?.takeIf { it.pack == pack }?.let { return it.tts }
        unload()
        val dir = dir(context, pack).path
        val model = when (pack.kind) {
            VoicePack.Kind.KOKORO -> OfflineTtsModelConfig(
                kokoro = OfflineTtsKokoroModelConfig(
                    model = "$dir/${pack.modelFile}",
                    voices = "$dir/voices.bin",
                    tokens = "$dir/tokens.txt",
                    dataDir = "$dir/espeak-ng-data",
                    lexicon = "$dir/lexicon-us-en.txt,$dir/lexicon-zh.txt",
                ),
                numThreads = 2,
                provider = "cpu",
            )
            VoicePack.Kind.VITS -> OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$dir/${pack.modelFile}",
                    tokens = "$dir/tokens.txt",
                    dataDir = "$dir/espeak-ng-data",
                ),
                numThreads = 2,
                provider = "cpu",
            )
        }
        val tts = OfflineTts(config = OfflineTtsConfig(model = model))
        loaded = Loaded(pack, tts)
        return tts
    }

    /** Loads the voice for [language] ahead of time, so the first line has no wait. */
    suspend fun warmUp(context: Context, language: String) = withContext(Dispatchers.Default) {
        val voice = voiceFor(context, language) ?: return@withContext
        runCatching { engine(context, voice.pack) }
    }

    @Synchronized
    fun unload() {
        val old = loaded ?: return
        loaded = null
        runCatching { old.tts.release() }
    }

    /** Synthesises [text] in [voice]: mono float PCM and its sample rate. */
    suspend fun synthesize(context: Context, voice: NaturalVoice, text: String): Pair<FloatArray, Int> =
        withContext(Dispatchers.Default) {
            val tts = engine(context, voice.pack)
            val audio = tts.generate(text, voice.speakerId, SPEED)
            audio.samples to audio.sampleRate
        }

    /** A touch under normal speed: a host, not a newsreader. */
    private const val SPEED = 0.97f
}

/**
 * Speaks through a [NaturalVoice] on the media stream. While one sentence plays
 * the next is already being synthesised, so the lines follow each other
 * without a gap.
 */
class SherpaVoice(private val context: Context, private val voice: NaturalVoice) : DjVoice {
    @Volatile
    private var track: AudioTrack? = null

    override suspend fun speak(chunks: List<DjVoice.Chunk>, gapMs: Long, volume: Float) = coroutineScope {
        if (chunks.isEmpty()) return@coroutineScope
        var next = async { NaturalVoices.synthesize(context, voice, chunks.first().text) }
        for (index in chunks.indices) {
            val (samples, rate) = next.await()
            if (index + 1 < chunks.size) {
                next = async { NaturalVoices.synthesize(context, voice, chunks[index + 1].text) }
            }
            play(samples, rate, volume)
            if (index + 1 < chunks.size) delay(gapMs)
        }
    }

    private suspend fun play(samples: FloatArray, rate: Int, volume: Float) = withContext(Dispatchers.IO) {
        if (samples.isEmpty()) return@withContext
        // Over 100% is real gain on the samples, held under clipping.
        val gain = volume.coerceIn(0.5f, 1.5f)
        val pcm = if (gain == 1f) samples else FloatArray(samples.size) { (samples[it] * gain).coerceIn(-1f, 1f) }
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setSampleRate(rate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuffer = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(DJ_SPEECH_ATTRIBUTES)
            .setAudioFormat(format)
            .setBufferSizeInBytes(maxOf(minBuffer, rate))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track = audioTrack
        try {
            audioTrack.play()
            var offset = 0
            while (offset < pcm.size) {
                coroutineContext.ensureActive()
                val written = audioTrack.write(pcm, offset, minOf(4096, pcm.size - offset), AudioTrack.WRITE_BLOCKING)
                if (written <= 0) break
                offset += written
            }
            // Let the buffer drain before the next sentence or the music comes back.
            val totalFrames = pcm.size
            while (audioTrack.playbackHeadPosition < totalFrames && audioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                coroutineContext.ensureActive()
                delay(20)
            }
        } finally {
            runCatching { audioTrack.stop() }
            audioTrack.release()
            if (track === audioTrack) track = null
        }
    }

    override fun stop() {
        track?.let { runCatching { it.pause(); it.flush() } }
    }

    override fun release() {
        stop()
    }
}
