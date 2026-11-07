package com.nuvio.app.features.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.subflow.SubFlowRepository
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.subflow_speak_word
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SubFlowElevatedSubtitleOverlay(
    subtitleStyle: SubtitleStyleState,
    modifier: Modifier = Modifier,
) {
    SubFlowRepository.ensureLoaded()
    val selection by SubFlowRepository.selection.collectAsState()
    val cueState by SubFlowRepository.cueState.collectAsState()
    val preferences by SubFlowRepository.preferences.collectAsState()
    val words by SubFlowRepository.words.collectAsState()
    val liveSubtitleText by SubFlowRepository.liveSubtitleText.collectAsState()
    val tokens = MaterialTheme.nuvio

    AnimatedVisibility(
        visible = selection != null,
        modifier = modifier.fillMaxSize(),
        enter = fadeIn(tween(160)) + slideInVertically(tween(220), initialOffsetY = { -it / 8 }),
        exit = fadeOut(tween(120)) + slideOutVertically(tween(160), targetOffsetY = { -it / 8 }),
    ) {
        val currentSelection = selection ?: return@AnimatedVisibility
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val topPadding = 72.dp
            val subtitleText = cueState?.current?.text
                ?.takeIf { it.isNotBlank() }
                ?: liveSubtitleText.takeIf { it.isNotBlank() }
                ?: currentSelection.context

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = topPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (preferences.showNavigation) {
                    IconButton(
                        onClick = SubFlowRepository::previousCue,
                        enabled = cueState?.canGoPrevious == true,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous subtitle",
                            tint = tokens.colors.textPrimary.copy(
                                alpha = if (cueState?.canGoPrevious == true) 0.95f else 0.35f,
                            ),
                        )
                    }
                }

                val background = if (subtitleStyle.backgroundColor.alpha > 0f) {
                    subtitleStyle.backgroundColor
                } else {
                    Color.Black.copy(alpha = 0.38f)
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    color = background,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        StyledSubtitleText(
                            text = subtitleText,
                            style = subtitleStyle.copy(backgroundColor = Color.Transparent),
                            subFlowPreferences = preferences,
                            subFlowWords = words,
                            selectedWord = currentSelection.word,
                            onSubFlowWordTap = SubFlowRepository::selectWord,
                        )
                    }
                }

                if (preferences.showSentenceSpeaker) {
                    IconButton(onClick = SubFlowRepository::speakCueText) {
                        Icon(
                            imageVector = Icons.Rounded.VolumeUp,
                            contentDescription = stringResource(Res.string.subflow_speak_word),
                            tint = tokens.colors.accent,
                        )
                    }
                }

                if (preferences.showNavigation) {
                    IconButton(
                        onClick = SubFlowRepository::nextCue,
                        enabled = cueState?.canGoNext == true,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next subtitle",
                            tint = tokens.colors.textPrimary.copy(
                                alpha = if (cueState?.canGoNext == true) 0.95f else 0.35f,
                            ),
                        )
                    }
                }
            }
        }
    }
}
