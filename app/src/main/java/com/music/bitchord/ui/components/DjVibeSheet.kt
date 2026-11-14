package com.music.bitchord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.playback.DjVibe
import com.music.bitchord.playback.voice.NaturalVoices
import com.music.bitchord.playback.voice.VoicePack
import com.music.bitchord.ui.player.LyreloDjOrb
import com.music.bitchord.ui.theme.LyreloButton
import com.music.bitchord.ui.theme.LyreloButtonStyle
import com.music.bitchord.ui.theme.LyreloTheme
import com.music.bitchord.ui.theme.Radius
import com.music.bitchord.ui.theme.Spacing
import com.music.bitchord.ui.theme.TileColors
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * "Pick a vibe": where a DJ mix starts. A tap starts the mix right away and
 * closes the sheet; the music follows as soon as the songs are fetched.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DjVibeSheet(onPick: (DjVibe) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = Spacing.gutter)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LyreloDjOrb(modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(Spacing.md))
                Column {
                    Text(stringResource(R.string.dj_vibe_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.dj_vibe_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            NaturalVoicePrompt()
            val colors = LyreloTheme.colors
            val learning = AppSettings.learningLanguage.collectAsStateWithLifecycle().value
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.heightIn(max = 520.dp),
            ) {
                items(DjVibe.entries.toList()) { vibe ->
                    val tile = when (vibe) {
                        DjVibe.FAVORITES -> colors.tileDj
                        DjVibe.LEARN -> colors.tilePractice
                        DjVibe.CHILL, DjVibe.FOCUS -> colors.tileContinue
                        DjVibe.ROMANCE, DjVibe.PARTY -> TileColors(colors.tileDj.end, colors.tilePractice.end)
                        DjVibe.FEEL_GOOD, DjVibe.ENERGY -> colors.tileMoods
                    }
                    val subtitle = when (vibe) {
                        DjVibe.FAVORITES -> stringResource(R.string.dj_vibe_favorites_subtitle)
                        DjVibe.LEARN -> learning.takeIf { it.isNotBlank() }?.let {
                            stringResource(
                                R.string.dj_vibe_learn_subtitle,
                                com.music.bitchord.data.lyrics.translationLanguageName(it, Locale.getDefault()),
                            )
                        }
                        else -> null
                    }
                    com.music.bitchord.ui.theme.LyreloTile(
                        title = stringResource(vibe.label),
                        subtitle = subtitle,
                        colors = tile,
                        onClick = { onPick(vibe) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

/**
 * The first time: an offer of the natural voice, with its size, a progress
 * bar and Cancel. Hidden once it's installed or after "Not now".
 */
@Composable
private fun NaturalVoicePrompt() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { NaturalVoices.refresh(context) }
    val state by NaturalVoices.state(VoicePack.KOKORO).collectAsStateWithLifecycle()
    var dismissed by remember { mutableStateOf(AppSettings.djVoicePromptDismissed.value) }
    if (state is NaturalVoices.PackState.Installed || dismissed) return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.lg)
            .background(MaterialTheme.colorScheme.surfaceVariant, Radius.large)
            .padding(Spacing.lg),
    ) {
        Text(
            stringResource(R.string.dj_voice_prompt_title, VoicePack.KOKORO.megabytes),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.dj_voice_prompt_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        when (val s = state) {
            is NaturalVoices.PackState.Downloading, NaturalVoices.PackState.Installing -> {
                val fraction = (s as? NaturalVoices.PackState.Downloading)?.fraction
                if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Spacing.sm))
                LyreloButton(
                    text = stringResource(R.string.dj_voice_cancel),
                    onClick = { NaturalVoices.cancelDownload(VoicePack.KOKORO) },
                    style = LyreloButtonStyle.Ghost,
                )
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (s is NaturalVoices.PackState.Failed) {
                    Text(
                        stringResource(R.string.dj_voice_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = Spacing.sm),
                    )
                }
                LyreloButton(
                    text = stringResource(R.string.dj_voice_get),
                    onClick = { NaturalVoices.startDownload(context, VoicePack.KOKORO) },
                )
                LyreloButton(
                    text = stringResource(R.string.dj_voice_not_now),
                    onClick = {
                        dismissed = true
                        AppSettings.setDjVoicePromptDismissed(true)
                    },
                    style = LyreloButtonStyle.Ghost,
                )
            }
        }
    }
}
