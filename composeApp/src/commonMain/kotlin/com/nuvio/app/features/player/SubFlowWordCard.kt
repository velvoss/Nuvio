package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.subflow.SubFlowCardSection
import com.nuvio.app.features.subflow.SubFlowPreferences
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowSelection
import com.nuvio.app.features.subflow.SubFlowWordStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.subflow_card_close
import nuvio.composeapp.generated.resources.subflow_card_context
import nuvio.composeapp.generated.resources.subflow_card_definition
import nuvio.composeapp.generated.resources.subflow_card_known
import nuvio.composeapp.generated.resources.subflow_card_learning
import nuvio.composeapp.generated.resources.subflow_card_no_meaning
import nuvio.composeapp.generated.resources.subflow_speak_word
import nuvio.composeapp.generated.resources.subflow_status_known
import nuvio.composeapp.generated.resources.subflow_status_learning
import nuvio.composeapp.generated.resources.subflow_status_new
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SubFlowWordCardOverlay(
    subtitleStyle: SubtitleStyleState,
    isPlaying: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubFlowRepository.ensureLoaded()
    val selection by SubFlowRepository.selection.collectAsState()
    val preferences by SubFlowRepository.preferences.collectAsState()
    val cueState by SubFlowRepository.cueState.collectAsState()
    var lastSelection by remember { mutableStateOf<SubFlowSelection?>(null) }
    if (selection != null) lastSelection = selection
    var resumeOnClose by remember { mutableStateOf(false) }

    LaunchedEffect(selection?.word) {
        if (selection != null) {
            if (isPlaying) {
                resumeOnClose = true
                onPause()
            }
        } else if (resumeOnClose) {
            resumeOnClose = false
            onResume()
        }
    }

    AnimatedVisibility(
        visible = selection != null,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(150)) + scaleIn(tween(180), initialScale = 0.94f),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f),
    ) {
        lastSelection?.let { current ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenHeight = maxHeight
                val compactHeight = maxHeight < 500.dp
                val compactWidth = maxWidth < 600.dp
                val horizontalMargin = if (compactWidth) 8.dp else 24.dp
                val topInset = if (compactHeight) 52.dp else 88.dp
                val bottomInset = if (compactHeight) 6.dp else 16.dp
                val gap = if (compactHeight) 8.dp else 18.dp
                val cardMaxWidth = minOf(
                    maxWidth - horizontalMargin * 2 - 8.dp,
                    if (compactWidth) 360.dp else 400.dp,
                )
                val barMaxWidth = minOf(cardMaxWidth + 20.dp, maxWidth - horizontalMargin * 2)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = topInset,
                            start = horizontalMargin,
                            end = horizontalMargin,
                            bottom = bottomInset,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SubFlowElevatedSubtitleBar(
                        subtitleStyle = subtitleStyle,
                        modifier = Modifier.widthIn(max = barMaxWidth),
                    )
                    Spacer(modifier = Modifier.height(gap))
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        SubFlowWordCard(
                            selection = current,
                            preferences = preferences,
                            cueText = cueState?.current?.text ?: current.context,
                            cueTranslation = cueState?.translation ?: current.sentenceTranslation,
                            onClose = { SubFlowRepository.closeCard() },
                            modifier = Modifier
                                .widthIn(max = cardMaxWidth)
                                .fillMaxWidth()
                                .heightIn(max = minOf(maxHeight, screenHeight * 0.72f, 430.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubFlowWordCard(
    selection: SubFlowSelection,
    preferences: SubFlowPreferences,
    cueText: String,
    cueTranslation: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val entry = selection.entry
    val statusColor = subFlowStatusColor(entry.status, preferences)
    val meaning = entry.meaning?.takeIf { it.isNotBlank() }
    val contextText = cueText.takeIf { it.isNotBlank() }
    val translatedContext = cueTranslation?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier,
        color = tokens.colors.surfaceDialog.copy(alpha = 0.97f),
        contentColor = tokens.colors.textPrimary,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(tokens.borders.thin, tokens.colors.borderSubtle),
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = entry.word,
                                style = MaterialTheme.typography.headlineLarge,
                                color = tokens.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            if (preferences.showWordSpeaker) {
                                IconButton(
                                    onClick = SubFlowRepository::speakWord,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = stringResource(Res.string.subflow_speak_word),
                                        tint = tokens.colors.textPrimary,
                                    )
                                }
                            }
                        }
                        val meta = listOfNotNull(
                            entry.phonetic?.takeIf { it.isNotBlank() },
                            entry.partOfSpeech?.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.colors.textMuted,
                            )
                        }
                    }
                    if (preferences.showStatus) {
                        SubFlowStatusBadge(status = entry.status, color = statusColor)
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.subflow_card_close),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }

                preferences.cardSectionOrder.forEach { section ->
                    when (section) {
                        SubFlowCardSection.Meaning -> if (preferences.showMeaning) {
                            SubFlowMeaningSection(
                                meaning = meaning,
                                isLoading = selection.isLoading,
                            )
                        }
                        SubFlowCardSection.Context -> if (preferences.showContext && contextText != null) {
                            SubFlowContextSection(
                                contextText = contextText,
                                translatedContext = translatedContext,
                            )
                        }
                        SubFlowCardSection.Definition -> if (preferences.showDefinition) {
                            entry.definition?.takeIf { it.isNotBlank() }?.let { definition ->
                                SubFlowDefinitionSection(definition = definition)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { SubFlowRepository.setStatus(SubFlowWordStatus.Learning) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.warning.copy(alpha = 0.22f),
                        contentColor = tokens.colors.warning,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.subflow_card_learning),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
                Button(
                    onClick = { SubFlowRepository.setStatus(SubFlowWordStatus.Known) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.subflow_card_known),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubFlowMeaningSection(
    meaning: String?,
    isLoading: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isLoading && meaning == null) {
            NuvioLoadingIndicator(size = 18.dp)
        }
        Text(
            text = meaning ?: if (isLoading) {
                "…"
            } else {
                stringResource(Res.string.subflow_card_no_meaning)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (meaning != null) tokens.colors.textPrimary else tokens.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SubFlowContextSection(
    contextText: String,
    translatedContext: String?,
) {
    val tokens = MaterialTheme.nuvio
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = stringResource(Res.string.subflow_card_context),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
        )
        Text(
            text = contextText,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textSecondary,
            textAlign = TextAlign.Start,
        )
        if (translatedContext != null) {
            Text(
                text = "«$translatedContext»",
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
            )
        } else if (cueStateIsLoading()) {
            NuvioLoadingIndicator(size = 16.dp)
        }
    }
}

@Composable
private fun SubFlowDefinitionSection(
    definition: String,
) {
    val tokens = MaterialTheme.nuvio
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = stringResource(Res.string.subflow_card_definition),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
        )
        Text(
            text = definition,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textSecondary,
        )
    }
}

@Composable
private fun cueStateIsLoading(): Boolean {
    val cueState by SubFlowRepository.cueState.collectAsState()
    return cueState?.isLoading == true
}

@Composable
private fun SubFlowStatusBadge(status: SubFlowWordStatus, color: Color) {
    val label = when (status) {
        SubFlowWordStatus.New -> stringResource(Res.string.subflow_status_new)
        SubFlowWordStatus.Learning -> stringResource(Res.string.subflow_status_learning)
        SubFlowWordStatus.Known -> stringResource(Res.string.subflow_status_known)
    }
    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

internal fun subFlowStatusColor(status: SubFlowWordStatus, preferences: SubFlowPreferences): Color = when (status) {
    SubFlowWordStatus.New -> parseSubFlowColor(preferences.newWordColor, Color(0xFFFFD65E))
    SubFlowWordStatus.Learning -> parseSubFlowColor(preferences.learningWordColor, Color(0xFF5CC8F5))
    SubFlowWordStatus.Known -> parseSubFlowColor(preferences.knownWordColor, Color(0xFF4DD18C))
}

internal fun parseSubFlowColor(hex: String, fallback: Color): Color {
    val clean = hex.removePrefix("#")
    if (clean.length != 6) return fallback
    return runCatching {
        val value = clean.toInt(16)
        Color(
            red = ((value shr 16) and 0xFF) / 255f,
            green = ((value shr 8) and 0xFF) / 255f,
            blue = (value and 0xFF) / 255f,
            alpha = 1f,
        )
    }.getOrDefault(fallback)
}
