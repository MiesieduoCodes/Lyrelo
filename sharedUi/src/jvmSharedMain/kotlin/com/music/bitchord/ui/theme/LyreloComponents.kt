package com.music.bitchord.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/*
 * Lyrelo's core components. Each one takes its colours, gaps and corners from
 * LyreloTokens, and every tappable one is at least [Touch.min] high, even when
 * it draws smaller.
 */

enum class LyreloButtonStyle { Primary, Secondary, Ghost }

/** A pill button. Primary is the accent gradient; Secondary a quiet fill; Ghost only text. */
@Composable
fun LyreloButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: LyreloButtonStyle = LyreloButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = LyreloTheme.colors
    val fill = when (style) {
        LyreloButtonStyle.Primary -> Modifier.background(colors.accentBrush, CircleShape)
        LyreloButtonStyle.Secondary -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        LyreloButtonStyle.Ghost -> Modifier
    }
    val content = when (style) {
        LyreloButtonStyle.Primary -> colors.onAccent
        LyreloButtonStyle.Secondary -> MaterialTheme.colorScheme.onSurface
        LyreloButtonStyle.Ghost -> MaterialTheme.colorScheme.primary
    }.copy(alpha = if (enabled) 1f else 0.4f)
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = Touch.min)
            .clip(CircleShape)
            .then(fill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(Touch.icon))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, textAlign = TextAlign.Center)
    }
}

/** Where a chip sits: on the page, or on dark glass over artwork. */
enum class ChipSurface { Page, Overlay }

/**
 * A selectable pill. Draws about 34dp tall but takes a [Touch.min] target, so
 * a row of them stays compact without being hard to hit.
 */
@Composable
fun LyreloChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    surface: ChipSurface = ChipSurface.Page,
) {
    val scheme = MaterialTheme.colorScheme
    val (fill, text) = when {
        selected && surface == ChipSurface.Overlay -> Color.White to Color.Black
        selected -> scheme.primary to scheme.onPrimary
        surface == ChipSurface.Overlay -> Color.White.copy(alpha = 0.16f) to Color.White
        else -> scheme.surfaceVariant to scheme.onSurface
    }
    Box(
        modifier = modifier
            .heightIn(min = Touch.min)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = text.copy(alpha = if (enabled) 1f else 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(fill)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/**
 * An icon you tap: a [Touch.min] target around a [Touch.icon] glyph, with an
 * optional disc behind it. [description] is what a screen reader says.
 */
@Composable
fun LyreloIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    disc: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(Touch.min)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = if (enabled) 1f else 0.42f),
                modifier = Modifier.size(Touch.icon),
            )
        }
    }
}

/** A raised surface for grouped content. */
@Composable
fun LyreloCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(Radius.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) { content() }
}

/** One row of a list: an optional leading icon, a title, a subtitle and a trailing slot. */
@Composable
fun LyreloListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** A section's heading on a page, in the display face. */
@Composable
fun LyreloSectionHeader(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.gutter, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

/**
 * One of Home's colour blocks. Grows with the text instead of clipping it, so
 * it holds at the largest font size; [badge] sits in the top corner.
 */
@Composable
fun LyreloTile(
    title: String,
    subtitle: String?,
    colors: TileColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier
            .heightIn(min = 96.dp)
            .clip(Radius.large)
            .background(colors.brush)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Spacing.lg),
    ) {
        badge?.invoke(this)
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(top = if (badge != null) 28.dp else 20.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The learning streak pill: a flame and "N-day streak". */
@Composable
fun LearningPill(label: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = LyreloTheme.colors
    Box(
        modifier = modifier
            .heightIn(min = Touch.min)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.flameContainer)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.flame, modifier = Modifier.size(Touch.icon))
            Spacer(Modifier.width(Spacing.xs))
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
