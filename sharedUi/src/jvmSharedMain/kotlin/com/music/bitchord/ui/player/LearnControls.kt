package com.music.bitchord.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.playback.LearnPlayback
import com.music.bitchord.sharedui.resources.*
import java.util.Locale
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** How the lyric rows behave while the Learn panel is open. */
internal class LyricsLearnGestures(
    /** Returns true when the press was used, so it doesn't also start a share pick. */
    val onLongPress: (Int) -> Boolean,
    /** Returns true when the tap was used, so it doesn't also seek. */
    val onTap: (Int) -> Boolean,
    /** The lines to mark on screen: the current loop, or a section's first line. */
    val markedLines: IntRange?,
)

/** The Learn panel's state for one song. */
internal class LyricsLearnUi(
    val open: Boolean,
    val toggleOpen: () -> Unit,
    val close: () -> Unit,
    val pickingSection: Boolean,
    val sectionStart: Int?,
    val togglePickingSection: () -> Unit,
    val gestures: LyricsLearnGestures,
)

@Composable
internal fun rememberLyricsLearn(
    trackKey: String,
    lines: List<LyricLine>,
    offsetMs: Int,
    durationMs: Long,
    /** False while the share picker owns the lines. */
    enabled: Boolean,
): LyricsLearnUi {
    var open by remember(trackKey) { mutableStateOf(false) }
    var pickingSection by remember(trackKey) { mutableStateOf(false) }
    var sectionStart by remember(trackKey) { mutableStateOf<Int?>(null) }
    val loop by LearnPlayback.loop.collectAsStateWithLifecycle()

    fun loopLines(first: Int, last: Int) {
        val start = adjustedLyricsSeekTarget(lines[first].timeMs, offsetMs)
        val end = lines.getOrNull(last + 1)?.let { adjustedLyricsSeekTarget(it.timeMs, offsetMs) }
            ?: lines[last].sungUntilMs?.let { adjustedLyricsSeekTarget(it, offsetMs) }
            ?: durationMs
        LearnPlayback.setLoop(
            LearnPlayback.Loop(
                startMs = start,
                endMs = end.coerceAtLeast(start + MIN_LOOP_MS),
                firstLine = first,
                lastLine = last,
            ),
        )
    }

    val gestures = LyricsLearnGestures(
        onLongPress = onLongPress@{ index ->
            if (!open || !enabled || index !in lines.indices) return@onLongPress false
            val current = loop
            if (current != null && current.firstLine == index && current.lastLine == index) {
                LearnPlayback.setLoop(null)
            } else {
                pickingSection = false
                sectionStart = null
                loopLines(index, index)
            }
            true
        },
        onTap = onTap@{ index ->
            if (!open || !enabled || !pickingSection || index !in lines.indices) return@onTap false
            val first = sectionStart
            if (first == null) {
                sectionStart = index
            } else {
                loopLines(minOf(first, index), maxOf(first, index))
                sectionStart = null
                pickingSection = false
            }
            true
        },
        markedLines = sectionStart?.let { it..it } ?: loop?.let { it.firstLine..it.lastLine },
    )
    return LyricsLearnUi(
        open = open && enabled,
        toggleOpen = {
            open = !open
            pickingSection = false
            sectionStart = null
        },
        close = {
            open = false
            pickingSection = false
            sectionStart = null
        },
        pickingSection = pickingSection,
        sectionStart = sectionStart,
        togglePickingSection = {
            pickingSection = !pickingSection
            sectionStart = null
        },
        gestures = gestures,
    )
}

/** The disc that opens the Learn panel, sized like the translate button beside it. */
@Composable
internal fun LearnToggleButton(open: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val tint = LocalPlayerTint.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (open) 0.32f else 0.14f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.School,
            contentDescription = stringResource(Res.string.learn),
            tint = if (enabled) tint else tint.copy(alpha = 0.42f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
internal fun LearnPanel(learn: LyricsLearnUi, modifier: Modifier = Modifier) {
    val speed by LearnPlayback.speed.collectAsStateWithLifecycle()
    val pitch by LearnPlayback.pitchSemitones.collectAsStateWithLifecycle()
    val loop by LearnPlayback.loop.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            // Taps on the panel's empty space don't fall through to the lyrics.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.learn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(Res.string.learn_close),
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = learn.close),
            )
        }

        Spacer(Modifier.height(10.dp))
        LabelledValue(stringResource(Res.string.learn_speed), speedLabel(speed))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5f, 0.75f, 1f).forEach { chip ->
                LearnChip(
                    label = speedLabel(chip),
                    selected = speed == chip,
                    onClick = { LearnPlayback.setSpeed(chip) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ThinSlider(
            value = (speed - LearnPlayback.MIN_SPEED) / (LearnPlayback.MAX_SPEED - LearnPlayback.MIN_SPEED),
            onValueChange = {
                LearnPlayback.setSpeed(
                    LearnPlayback.MIN_SPEED + it * (LearnPlayback.MAX_SPEED - LearnPlayback.MIN_SPEED),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                LabelledValue(stringResource(Res.string.learn_pitch), pitchLabel(pitch))
            }
            Spacer(Modifier.width(10.dp))
            LearnChip(
                label = stringResource(Res.string.learn_reset),
                selected = false,
                enabled = pitch != 0,
                onClick = { LearnPlayback.setPitch(0) },
            )
        }
        Spacer(Modifier.height(10.dp))
        val pitchSpan = (LearnPlayback.MAX_PITCH - LearnPlayback.MIN_PITCH).toFloat()
        ThinSlider(
            value = (pitch - LearnPlayback.MIN_PITCH) / pitchSpan,
            onValueChange = {
                LearnPlayback.setPitch((LearnPlayback.MIN_PITCH + it * pitchSpan).roundToInt())
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))
        Text(
            text = when {
                learn.pickingSection && learn.sectionStart != null ->
                    stringResource(Res.string.learn_ab_pick_end)
                learn.pickingSection -> stringResource(Res.string.learn_ab_pick_start)
                else -> stringResource(Res.string.learn_loop_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LearnChip(
                label = stringResource(Res.string.learn_loop_ab),
                selected = learn.pickingSection,
                onClick = learn.togglePickingSection,
            )
            if (loop != null) {
                LearnChip(
                    label = stringResource(Res.string.learn_loop_off),
                    selected = false,
                    onClick = { LearnPlayback.setLoop(null) },
                )
            }
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun LearnChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        color = when {
            selected -> Color.Black
            enabled -> Color.White
            else -> Color.White.copy(alpha = 0.4f)
        },
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.16f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** Says what's changed on the player. One tap puts playback back to normal. */
@Composable
internal fun LearnIndicator(modifier: Modifier = Modifier) {
    val speed by LearnPlayback.speed.collectAsStateWithLifecycle()
    val pitch by LearnPlayback.pitchSemitones.collectAsStateWithLifecycle()
    val loop by LearnPlayback.loop.collectAsStateWithLifecycle()
    if (speed == 1f && pitch == 0 && loop == null) return
    val loopLabel = stringResource(Res.string.learn_loop)
    val label = buildList {
        if (speed != 1f) add(speedLabel(speed))
        if (pitch != 0) add(pitchLabel(pitch))
        if (loop != null) add(loopLabel)
    }.joinToString(" · ")
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = LearnPlayback::turnOff)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(Res.string.learn_turn_off),
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
    }
}

private fun speedLabel(speed: Float): String =
    String.format(Locale.ROOT, "%.2f", speed).trimEnd('0').trimEnd('.') + "x"

private fun pitchLabel(semitones: Int): String = when {
    semitones > 0 -> "+$semitones"
    else -> semitones.toString()
}

private const val MIN_LOOP_MS = 1_000L
