package com.music.bitchord.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.bitchord.R
import com.music.bitchord.data.settings.AccentChoice
import com.music.bitchord.ui.theme.Spacing
import com.music.bitchord.ui.theme.Touch

/** A row of accent swatches, each with its name under it. */
@Composable
fun AccentPicker(
    selected: AccentChoice,
    onSelect: (AccentChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = AccentChoice.entries.filter {
        it != AccentChoice.MATERIAL_YOU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Spacer(Modifier.width(Spacing.sm))
        choices.forEach { choice ->
            Swatch(choice, choice == selected) { onSelect(choice) }
        }
        Spacer(Modifier.width(Spacing.sm))
    }
}

@Composable
private fun Swatch(choice: AccentChoice, selected: Boolean, onClick: () -> Unit) {
    val name = choice.label()
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                contentDescription = name
            }
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Touch.min)
                .clip(CircleShape)
                .background(choice.swatch())
                .then(
                    if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when {
                selected -> Icons.Rounded.Check
                choice == AccentChoice.DYNAMIC -> Icons.Rounded.Image
                choice == AccentChoice.MATERIAL_YOU -> Icons.Rounded.Wallpaper
                else -> null
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(Touch.icon))
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun AccentChoice.swatch(): Brush = when (this) {
    AccentChoice.LYRELO -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))
    AccentChoice.DYNAMIC -> Brush.sweepGradient(listOf(Color(0xFFF97316), Color(0xFFEAB308), Color(0xFF10B981), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFF97316)))
    AccentChoice.MATERIAL_YOU -> Brush.linearGradient(listOf(Color(0xFF6750A4), Color(0xFF7D5260)))
    AccentChoice.VIOLET -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)))
    AccentChoice.PINK -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF43F5E)))
    AccentChoice.OCEAN -> Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)))
    AccentChoice.MINT -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF06B6D4)))
    AccentChoice.SUNSET -> Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEC4899)))
    AccentChoice.GOLD -> Brush.linearGradient(listOf(Color(0xFFEAB308), Color(0xFFF97316)))
    AccentChoice.CRIMSON -> Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C)))
}

@Composable
private fun AccentChoice.label(): String = stringResource(
    when (this) {
        AccentChoice.LYRELO -> R.string.accent_lyrelo
        AccentChoice.DYNAMIC -> R.string.accent_dynamic
        AccentChoice.MATERIAL_YOU -> R.string.accent_material_you
        AccentChoice.VIOLET -> R.string.accent_violet
        AccentChoice.PINK -> R.string.accent_pink
        AccentChoice.OCEAN -> R.string.accent_ocean
        AccentChoice.MINT -> R.string.accent_mint
        AccentChoice.SUNSET -> R.string.accent_sunset
        AccentChoice.GOLD -> R.string.accent_gold
        AccentChoice.CRIMSON -> R.string.accent_crimson
    },
)
