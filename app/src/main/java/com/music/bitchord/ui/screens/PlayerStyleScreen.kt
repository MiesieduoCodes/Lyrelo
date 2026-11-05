package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.ArtworkCorner
import com.music.bitchord.data.settings.PlayerBackground
import com.music.bitchord.data.settings.PlayerStyle
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.icons.SfIcons

private val ArtFill = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))

/** Settings → Appearance → Player style: the choices, above a preview that follows them. */
@Composable
fun PlayerStyleScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val style by AppSettings.playerStyle.collectAsStateWithLifecycle()
    val background by AppSettings.playerBackground.collectAsStateWithLifecycle()
    val corner by AppSettings.artworkCorner.collectAsStateWithLifecycle()
    val showStrip by AppSettings.showLyricStrip.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            PlayerPreview(style, background, corner, showStrip)
        }
        ChoiceRow(
            label = stringResource(R.string.player_style),
            options = PlayerStyle.entries,
            selected = style,
            nameRes = {
                when (it) {
                    PlayerStyle.CLASSIC -> R.string.player_style_classic
                    PlayerStyle.IMMERSIVE -> R.string.player_style_immersive
                    PlayerStyle.VINYL -> R.string.player_style_vinyl
                    PlayerStyle.MINIMAL -> R.string.player_style_minimal
                    PlayerStyle.LYRICS_FIRST -> R.string.player_style_lyrics_first
                }
            },
            onSelect = AppSettings::setPlayerStyle,
        )
        Text(
            text = stringResource(
                when (style) {
                    PlayerStyle.CLASSIC -> R.string.player_style_classic_detail
                    PlayerStyle.IMMERSIVE -> R.string.player_style_immersive_detail
                    PlayerStyle.VINYL -> R.string.player_style_vinyl_detail
                    PlayerStyle.MINIMAL -> R.string.player_style_minimal_detail
                    PlayerStyle.LYRICS_FIRST -> R.string.player_style_lyrics_first_detail
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        ChoiceRow(
            label = stringResource(R.string.player_background),
            options = PlayerBackground.entries,
            selected = background,
            nameRes = {
                when (it) {
                    PlayerBackground.ARTWORK_COLORS -> R.string.player_background_artwork_colors
                    PlayerBackground.BLUR -> R.string.player_background_blur
                    PlayerBackground.SOLID_DARK -> R.string.player_background_solid_dark
                    PlayerBackground.ANIMATED_GRADIENT -> R.string.player_background_animated_gradient
                }
            },
            onSelect = AppSettings::setPlayerBackground,
        )
        ChoiceRow(
            label = stringResource(R.string.artwork_corners),
            options = ArtworkCorner.entries,
            selected = corner,
            nameRes = {
                when (it) {
                    ArtworkCorner.NONE -> R.string.artwork_corners_none
                    ArtworkCorner.SMALL -> R.string.artwork_corners_small
                    ArtworkCorner.MEDIUM -> R.string.artwork_corners_medium
                    ArtworkCorner.LARGE -> R.string.artwork_corners_large
                }
            },
            onSelect = AppSettings::setArtworkCorner,
        )
        Text(
            text = stringResource(R.string.artwork_corners_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        SettingsGroup {
            SettingsRow(
                icon = SfIcons.Translate,
                title = stringResource(R.string.show_lyric_strip),
                trailing = {
                    IosSwitch(checked = showStrip, onCheckedChange = AppSettings::setShowLyricStrip)
                },
                onClick = { AppSettings.setShowLyricStrip(!showStrip) },
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** A row of selectable pills under a heading. */
@Composable
internal fun <T> ChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    nameRes: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                text = stringResource(nameRes(option)),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        },
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** A small drawing of the player, redrawn as each choice changes. */
@Composable
private fun PlayerPreview(
    style: PlayerStyle,
    background: PlayerBackground,
    corner: ArtworkCorner,
    showStrip: Boolean,
) {
    val motion = rememberInfiniteTransition(label = "playerPreview")
    val spin by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "spin",
    )
    val drift by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val fill: Brush = when (background) {
        PlayerBackground.SOLID_DARK -> Brush.verticalGradient(listOf(Color(0xFF0C0C0E), Color(0xFF0C0C0E)))
        PlayerBackground.BLUR -> Brush.verticalGradient(listOf(Color(0xFF4A2C7A), Color(0xFF6B2A58)))
        PlayerBackground.ARTWORK_COLORS -> Brush.linearGradient(
            listOf(Color(0xFF3B1F6E), Color(0xFF8B2F67), Color(0xFF1E1140)),
        )
        PlayerBackground.ANIMATED_GRADIENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF1E1140)),
            start = androidx.compose.ui.geometry.Offset(drift * 400f, 0f),
            end = androidx.compose.ui.geometry.Offset(400f - drift * 200f, 620f),
        )
    }
    val artShape: Shape = RoundedCornerShape(corner.dp.dp / 2)
    Column(
        modifier = Modifier
            .width(180.dp)
            .height(340.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(fill)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (style) {
            PlayerStyle.CLASSIC, PlayerStyle.IMMERSIVE -> {
                PreviewArt(Modifier.size(150.dp), artShape)
                Spacer(Modifier.weight(1f))
                PreviewTitle(titleHeight = 9.dp)
            }
            PlayerStyle.VINYL -> {
                Box(
                    Modifier
                        .size(150.dp)
                        .graphicsLayer { rotationZ = spin }
                        .clip(CircleShape)
                        .background(ArtFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.5f)
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = 1.dp)
                            .background(Color.White.copy(alpha = 0.7f)),
                    )
                    Box(Modifier.size(20.dp).clip(CircleShape).background(Color.Black))
                }
                Spacer(Modifier.weight(1f))
                PreviewTitle(titleHeight = 9.dp)
            }
            PlayerStyle.MINIMAL -> {
                Spacer(Modifier.height(20.dp))
                PreviewArt(Modifier.size(78.dp), artShape)
                Spacer(Modifier.weight(1f))
                PreviewTitle(titleHeight = 15.dp)
            }
            PlayerStyle.LYRICS_FIRST -> {
                repeat(6) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth(if (index % 2 == 0) 1f else 0.75f)
                            .padding(vertical = 4.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (index == 2) 1f else 0.35f)),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PreviewArt(Modifier.size(30.dp), artShape)
                    Spacer(Modifier.width(8.dp))
                    PreviewBar(fraction = 1f, height = 8.dp, alpha = 0.9f)
                }
            }
        }
        if (showStrip && style != PlayerStyle.LYRICS_FIRST) {
            Spacer(Modifier.height(8.dp))
            PreviewBar(fraction = 0.8f, height = 6.dp, alpha = 0.55f)
        }
        Spacer(Modifier.height(8.dp))
        val frosted = style == PlayerStyle.IMMERSIVE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (frosted) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                .padding(if (frosted) 8.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewBar(fraction = 1f, height = 4.dp, alpha = 0.6f)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White))
                Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)))
            }
        }
    }
}

@Composable
private fun PreviewArt(modifier: Modifier, shape: Shape) {
    Box(modifier.clip(shape).background(ArtFill))
}

@Composable
private fun ColumnScope.PreviewTitle(titleHeight: Dp) {
    PreviewBar(fraction = 0.72f, height = titleHeight, alpha = 0.95f, modifier = Modifier.align(Alignment.Start))
    Spacer(Modifier.height(5.dp))
    PreviewBar(fraction = 0.45f, height = 6.dp, alpha = 0.55f, modifier = Modifier.align(Alignment.Start))
}

@Composable
private fun PreviewBar(fraction: Float, height: Dp, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha)),
    )
}
