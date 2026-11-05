package com.music.bitchord.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.playback.DjPresence

private val Violet = Color(0xFF8B5CF6)
private val Pink = Color(0xFFEC4899)
private val Cyan = Color(0xFF22D3EE)

/**
 * The DJ on the player: a glowing orb that breathes and spins while the DJ
 * talks, its words underneath, and an X that ends the mix.
 */
@Composable
internal fun DjBadge(state: DjPresence.State, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DjOrb(speaking = state.speaking, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Lyrelo DJ",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "End DJ",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { DjPresence.onStop?.invoke() },
            )
        }
        AnimatedVisibility(
            visible = state.speaking && !state.caption.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = state.caption.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 4,
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp, end = 24.dp)
                    .widthIn(max = 420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** A sci-fi orb: a glowing core with two orbiting rings. Faster and brighter while speaking. */
@Composable
internal fun DjOrb(speaking: Boolean, modifier: Modifier = Modifier) {
    val motion = rememberInfiniteTransition(label = "djOrb")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (speaking) 1_600 else 6_000, easing = LinearEasing)),
        label = "spin",
    )
    val pulse by motion.animateFloat(
        initialValue = 0.82f,
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
                        listOf(Color.White, Pink, Violet.copy(alpha = 0.6f), Color.Transparent),
                        center = center,
                        radius = radius * pulse,
                    ),
                    radius = radius * pulse,
                )
                drawArc(
                    color = Cyan.copy(alpha = 0.9f),
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
