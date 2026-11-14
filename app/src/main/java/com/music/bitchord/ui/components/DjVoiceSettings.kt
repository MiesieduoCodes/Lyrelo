package com.music.bitchord.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.LyreloDj
import com.music.bitchord.playback.dj.DjChattiness
import com.music.bitchord.playback.voice.NaturalVoice
import com.music.bitchord.playback.voice.NaturalVoices
import com.music.bitchord.playback.voice.VoicePack
import com.music.bitchord.ui.theme.ChipSurface
import com.music.bitchord.ui.theme.LyreloChip
import com.music.bitchord.ui.theme.LyreloIconButton
import com.music.bitchord.ui.theme.Spacing
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Settings → Learning → the DJ: how often it talks, how loud, and in which voice. */
@Composable
fun DjVoiceSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { NaturalVoices.refresh(context) }
    val chattiness by AppSettings.djChattiness.collectAsStateWithLifecycle()
    val volume by AppSettings.djVoiceVolume.collectAsStateWithLifecycle()
    val selected by AppSettings.djNaturalVoice.collectAsStateWithLifecycle()
    Column(modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Label(stringResource(R.string.dj_chattiness))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            DjChattiness.entries.forEach { option ->
                LyreloChip(
                    label = stringResource(
                        when (option) {
                            DjChattiness.CHATTY -> R.string.dj_chattiness_chatty
                            DjChattiness.NORMAL -> R.string.dj_chattiness_normal
                            DjChattiness.QUIET -> R.string.dj_chattiness_quiet
                        },
                    ),
                    selected = option == chattiness,
                    onClick = { AppSettings.setDjChattiness(option) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        val percent = (volume * 100).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.dj_voice_volume), Modifier.weight(1f))
            Text(stringResource(R.string.dj_voice_volume_value, percent), style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = volume,
            onValueChange = { AppSettings.setDjVoiceVolume((it * 20).roundToInt() / 20f) },
            valueRange = 0.5f..1.5f,
            steps = 19,
            modifier = Modifier.semantics { contentDescription = "${context.getString(R.string.dj_voice_volume)} $percent%" },
        )

        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(stringResource(R.string.dj_voice), Modifier.weight(1f))
            LyreloIconButton(
                icon = Icons.Rounded.PlayArrow,
                description = stringResource(R.string.dj_voice_preview),
                onClick = { scope.launch { LyreloDj.preview() } },
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            NaturalVoice.ENGLISH.forEach { voice ->
                LyreloChip(
                    label = stringResource(voice.label()),
                    selected = voice.name == selected,
                    onClick = { AppSettings.setDjNaturalVoice(voice.name) },
                    surface = ChipSurface.Page,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))
        Label(stringResource(R.string.dj_voice_installed))
        VoicePack.entries.forEach { pack -> PackRow(pack) }
    }
}

@Composable
private fun PackRow(pack: VoicePack) {
    val context = LocalContext.current
    val state by NaturalVoices.state(pack).collectAsStateWithLifecycle()
    val name = stringResource(
        when (pack) {
            VoicePack.KOKORO -> R.string.dj_voice_af_heart
            VoicePack.RU_DMITRI -> R.string.dj_voice_ru_dmitri
            VoicePack.KK_ISSAI -> R.string.dj_voice_kk_issai
        },
    ).let { if (pack == VoicePack.KOKORO) "English · Kokoro" else it }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            when (val s = state) {
                is NaturalVoices.PackState.Downloading -> {
                    Text(
                        stringResource(R.string.dj_voice_downloading, (s.fraction * 100).roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(progress = { s.fraction }, modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs))
                }
                is NaturalVoices.PackState.Failed -> Text(
                    stringResource(R.string.dj_voice_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text(
                    "~${pack.megabytes} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        when (state) {
            NaturalVoices.PackState.Installed -> LyreloIconButton(
                icon = Icons.Rounded.Delete,
                description = "${stringResource(R.string.dj_voice_delete)} $name",
                onClick = { NaturalVoices.delete(context, pack) },
            )
            is NaturalVoices.PackState.Downloading, NaturalVoices.PackState.Installing -> LyreloIconButton(
                icon = Icons.Rounded.Close,
                description = stringResource(R.string.dj_voice_cancel),
                onClick = { NaturalVoices.cancelDownload(pack) },
            )
            else -> LyreloIconButton(
                icon = Icons.Rounded.Download,
                description = "${stringResource(R.string.dj_voice_get)} $name",
                onClick = { NaturalVoices.startDownload(context, pack) },
            )
        }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = Spacing.xs),
    )
}

private fun NaturalVoice.label(): Int = when (this) {
    NaturalVoice.AF_HEART -> R.string.dj_voice_af_heart
    NaturalVoice.AF_BELLA -> R.string.dj_voice_af_bella
    NaturalVoice.AM_MICHAEL -> R.string.dj_voice_am_michael
    NaturalVoice.AM_FENRIR -> R.string.dj_voice_am_fenrir
    NaturalVoice.RU_DMITRI -> R.string.dj_voice_ru_dmitri
    NaturalVoice.KK_ISSAI -> R.string.dj_voice_kk_issai
}
