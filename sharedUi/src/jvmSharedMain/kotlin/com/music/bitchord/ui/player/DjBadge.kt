package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.bitchord.playback.DjPresence
import com.music.bitchord.sharedui.resources.*
import com.music.bitchord.ui.theme.LyreloBrand
import com.music.bitchord.ui.theme.LyreloIconButton
import com.music.bitchord.ui.theme.LyreloTheme
import com.music.bitchord.ui.theme.Radius
import com.music.bitchord.ui.theme.Spacing
import com.music.bitchord.ui.theme.Touch
import org.jetbrains.compose.resources.stringResource

/**
 * The DJ on the player: a glowing orb that breathes and spins while the DJ
 * talks, its words underneath, a button to change the vibe and an X that ends
 * the mix. Both buttons are full-size touch targets inside the compact pill.
 */
@Composable
internal fun DjBadge(state: DjPresence.State, modifier: Modifier = Modifier) {
    val colors = LyreloTheme.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.overlay)
                .padding(start = Spacing.sm, end = Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DjOrb(speaking = state.speaking, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.lyrelo_dj),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onOverlay,
            )
            Spacer(Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .heightIn(min = Touch.min)
                    .clickable(role = Role.Button) { DjPresence.onChangeVibe?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.dj_change_vibe),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs),
                )
            }
            LyreloIconButton(
                icon = Icons.Rounded.Close,
                description = stringResource(Res.string.dj_end),
                onClick = { DjPresence.onStop?.invoke() },
                tint = colors.onOverlayMuted,
            )
        }
        AnimatedVisibility(
            visible = !state.caption.isNullOrBlank() && (state.speaking || state.notice),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = state.caption.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onOverlay,
                textAlign = TextAlign.Center,
                maxLines = 4,
                modifier = Modifier
                    .padding(top = Spacing.sm, start = Spacing.xl, end = Spacing.xl)
                    .widthIn(max = 420.dp)
                    .clip(Radius.medium)
                    .background(colors.overlay)
                    .padding(horizontal = Spacing.md + Spacing.xxs, vertical = Spacing.sm + Spacing.xxs)
                    // Read out as it changes, for people listening with a screen reader.
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/**
 * A sci-fi orb: a glowing core with two orbiting rings. Faster and brighter
 * while speaking; still when the app is asked to reduce animation.
 */
@Composable
internal fun DjOrb(speaking: Boolean, modifier: Modifier = Modifier) {
    val still = LyreloTheme.reduceMotion
    val motion = rememberInfiniteTransition(label = "djOrb")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = if (still) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(if (speaking) 1_600 else 6_000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by motion.animateFloat(
        initialValue = if (still) 1f else 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (speaking) 260 else 1_400), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        modifier
            .graphicsLayer { rotationZ = spin }
            .drawBehind {
                val radius = size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.White, LyreloBrand.Pink, LyreloBrand.Violet.copy(alpha = 0.6f), Color.Transparent),
                        center = center,
                        radius = radius * pulse,
                    ),
                    radius = radius * pulse,
                )
                drawArc(
                    color = LyreloBrand.Cyan.copy(alpha = 0.9f),
                    startAngle = 0f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.06f, size.height * 0.06f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.88f, size.height * 0.88f),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.7f),
                    startAngle = 180f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            },
    )
}

/** The same orb, large, for buttons that start the DJ. */
@Composable
fun LyreloDjOrb(speaking: Boolean = false, modifier: Modifier = Modifier) = DjOrb(speaking, modifier)
