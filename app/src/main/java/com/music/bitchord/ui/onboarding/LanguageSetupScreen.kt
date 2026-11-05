package com.music.bitchord.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.LocaleTags
import com.music.bitchord.data.lyrics.TRANSLATION_LANGUAGES
import com.music.bitchord.data.lyrics.translationLanguageName
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.ui.components.LanguagePickerDialog
import com.music.bitchord.ui.player.MESH_CYCLE_MILLIS
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.WelcomeMeshPalettes
import com.music.bitchord.ui.player.cyclingMeshPalette
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.util.Locale

private enum class LanguageField { Learning, Native }

/**
 * Asks once, on first launch, which language is being learned and which one
 * translations should be shown in. Both can be changed later in Settings.
 *
 * Drawn on the welcome's own mesh, with the frosted language picker from
 * Settings. It blurs this screen's mesh rather than the app underneath.
 */
@Composable
fun LanguageSetupScreen(
    onFinish: (learning: String, native: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
    var learning by rememberSaveable { mutableStateOf(AppSettings.learningLanguage.value) }
    var native by rememberSaveable {
        mutableStateOf(AppSettings.translationLanguage.value.ifBlank { supportedLanguageFor(locale) })
    }
    var picking by rememberSaveable { mutableStateOf<LanguageField?>(null) }
    val hazeState = remember { HazeState() }
    val sameLanguage = learning.isNotBlank() && learning.equals(native, ignoreCase = true)

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keeps touches from reaching the app laid out underneath.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            MeshGradientBackground(
                palette = cyclingMeshPalette(WelcomeMeshPalettes),
                trackKey = 0,
                continuous = true,
                driftMillis = 6_000,
                colorFadeMillis = MESH_CYCLE_MILLIS,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.05f),
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
            )
        }

        // Scrolls so that nothing is cut off on a phone held sideways, while the
        // minimum height keeps the button at the foot of the screen otherwise.
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Spacer(Modifier.height(40.dp))
                    Title(stringResource(R.string.language_setup_title))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.language_setup_body),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Spacer(Modifier.height(28.dp))
                }
                Column {
                    LanguageRow(
                        label = stringResource(R.string.language_setup_learning_label),
                        code = learning,
                        locale = locale,
                        onClick = { picking = LanguageField.Learning },
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageRow(
                        label = stringResource(R.string.language_setup_native_label),
                        code = native,
                        locale = locale,
                        onClick = { picking = LanguageField.Native },
                    )
                    if (sameLanguage) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.language_setup_same_language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    PillButton(
                        label = stringResource(R.string.onboarding_next),
                        onClick = { onFinish(learning, native) },
                        enabled = learning.isNotBlank() && native.isNotBlank() && !sameLanguage,
                        primary = true,
                    )
                }
            }
        }

        picking?.let { field ->
            BackHandler { picking = null }
            val isLearning = field == LanguageField.Learning
            LanguagePickerDialog(
                title = stringResource(if (isLearning) R.string.learning_language else R.string.translation_language),
                description = stringResource(
                    if (isLearning) R.string.learning_language_description else R.string.translation_language_description,
                ),
                selected = if (isLearning) learning else native,
                offerAppLanguage = false,
                hazeState = hazeState,
                onSelect = { if (isLearning) learning = it else native = it },
                onDismiss = { picking = null },
            )
        }
    }
}

/** A frosted row naming one of the two choices, which opens the picker when tapped. */
@Composable
private fun LanguageRow(label: String, code: String, locale: Locale, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
            Text(
                text = if (code.isBlank()) {
                    stringResource(R.string.language_setup_choose)
                } else {
                    translationLanguageName(code, locale)
                },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
                color = if (code.isBlank()) Color.White.copy(alpha = 0.55f) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** The translator's code for [locale]'s language, or blank if it doesn't offer one. */
private fun supportedLanguageFor(locale: Locale): String {
    val tag = locale.toLanguageTag()
    val wanted = if (LocaleTags.isChineseTag(tag)) {
        LocaleTags.translationWireTarget(tag)
    } else {
        // The translator keeps the older codes for these three.
        when (locale.language) {
            "he" -> "iw"
            "jv" -> "jw"
            "fil" -> "tl"
            else -> locale.language
        }
    }
    return TRANSLATION_LANGUAGES.firstOrNull { it.code.equals(wanted, ignoreCase = true) }?.code.orEmpty()
}
