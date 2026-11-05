package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.LyricAlign
import com.music.bitchord.data.settings.LyricFont
import com.music.bitchord.data.settings.LyricFontSize
import com.music.bitchord.data.settings.LyricStyle
import com.music.bitchord.data.settings.TranslationColor
import com.music.bitchord.data.settings.TranslationSize
import com.music.bitchord.ui.components.IosSwitch
import com.music.bitchord.ui.icons.SfIcons
import com.music.bitchord.ui.player.LyricStylePreview

/** Settings → Lyrics → Style: the choices, above the real lyric panel running on a sample verse. */
@Composable
fun LyricStyleScreen(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    val style by AppSettings.lyricStyle.collectAsStateWithLifecycle()
    val size by AppSettings.lyricFontSize.collectAsStateWithLifecycle()
    val font by AppSettings.lyricFont.collectAsStateWithLifecycle()
    val align by AppSettings.lyricAlign.collectAsStateWithLifecycle()
    val blur by AppSettings.lyricsBlur.collectAsStateWithLifecycle()
    val translationSize by AppSettings.translationSize.collectAsStateWithLifecycle()
    val translationColor by AppSettings.translationColor.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // Held above the scroll, so every change can be watched as it is made.
        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF17121F)),
        ) {
            LyricStylePreview(Modifier.fillMaxSize().padding(horizontal = 12.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ChoiceRow(
                label = stringResource(R.string.lyric_style),
                options = LyricStyle.entries,
                selected = style,
                nameRes = {
                    when (it) {
                        LyricStyle.LINE -> R.string.lyric_style_line
                        LyricStyle.GLOW -> R.string.lyric_style_glow
                        LyricStyle.KARAOKE -> R.string.lyric_style_karaoke
                        LyricStyle.WORD_BY_WORD -> R.string.lyric_style_word_by_word
                    }
                },
                onSelect = AppSettings::setLyricStyle,
            )
            if (style == LyricStyle.KARAOKE || style == LyricStyle.WORD_BY_WORD) {
                Text(
                    text = stringResource(R.string.lyric_style_timing_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            ChoiceRow(
                label = stringResource(R.string.lyric_font_size),
                options = LyricFontSize.entries,
                selected = size,
                nameRes = {
                    when (it) {
                        LyricFontSize.SMALL -> R.string.size_small
                        LyricFontSize.MEDIUM -> R.string.size_medium
                        LyricFontSize.LARGE -> R.string.size_large
                        LyricFontSize.HUGE -> R.string.size_huge
                    }
                },
                onSelect = AppSettings::setLyricFontSize,
            )
            ChoiceRow(
                label = stringResource(R.string.lyric_font),
                options = LyricFont.entries,
                selected = font,
                nameRes = {
                    when (it) {
                        LyricFont.DEFAULT -> R.string.lyric_font_default
                        LyricFont.SANS -> R.string.lyric_font_sans
                        LyricFont.SERIF -> R.string.lyric_font_serif
                        LyricFont.MONO -> R.string.lyric_font_mono
                    }
                },
                onSelect = AppSettings::setLyricFont,
            )
            ChoiceRow(
                label = stringResource(R.string.lyric_alignment),
                options = LyricAlign.entries,
                selected = align,
                nameRes = {
                    when (it) {
                        LyricAlign.LEFT -> R.string.lyric_alignment_left
                        LyricAlign.CENTER -> R.string.lyric_alignment_center
                    }
                },
                onSelect = AppSettings::setLyricAlign,
            )
            ChoiceRow(
                label = stringResource(R.string.translation_size),
                options = TranslationSize.entries,
                selected = translationSize,
                nameRes = {
                    when (it) {
                        TranslationSize.SMALL -> R.string.size_small
                        TranslationSize.MEDIUM -> R.string.size_medium
                        TranslationSize.LARGE -> R.string.size_large
                    }
                },
                onSelect = AppSettings::setTranslationSize,
            )
            ChoiceRow(
                label = stringResource(R.string.translation_color),
                options = TranslationColor.entries,
                selected = translationColor,
                nameRes = {
                    when (it) {
                        TranslationColor.DEFAULT -> R.string.translation_color_default
                        TranslationColor.WHITE -> R.string.translation_color_white
                        TranslationColor.AMBER -> R.string.translation_color_amber
                        TranslationColor.MINT -> R.string.translation_color_mint
                        TranslationColor.PINK -> R.string.translation_color_pink
                    }
                },
                onSelect = AppSettings::setTranslationColor,
            )
            SettingsGroup {
                SettingsRow(
                    icon = SfIcons.BlurOn,
                    title = stringResource(R.string.blur_unfocused_lyrics),
                    subtitle = stringResource(R.string.blur_unfocused_lyrics_subtitle),
                    trailing = {
                        IosSwitch(checked = blur, onCheckedChange = AppSettings::setLyricsBlur)
                    },
                    onClick = { AppSettings.setLyricsBlur(!blur) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
