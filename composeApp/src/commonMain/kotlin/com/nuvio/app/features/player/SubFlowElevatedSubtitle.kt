package com.nuvio.app.features.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
internal fun SubFlowElevatedSubtitleBar(
    subtitleStyle: SubtitleStyleState,
    modifier: Modifier = Modifier,
) {
    SubFlowRepository.ensureLoaded()
    val selection by SubFlowRepository.selection.collectAsState()
    val cueState by SubFlowRepository.cueState.collectAsState()
    val preferences by SubFlowRepository.preferences.collectAsState()
    val words by SubFlowRepository.words.collectAsState()
    val liveSubtitleText by SubFlowRepository.liveSubtitleText.collectAsState()
    val currentSelection = selection ?: return
    val tokens = MaterialTheme.nuvio
    val subtitleText = cueState?.current?.text
        ?.takeIf { it.isNotBlank() }
        ?: liveSubtitleText.takeIf { it.isNotBlank() }
        ?: currentSelection.context
    val background = if (subtitleStyle.backgroundColor.alpha > 0f) {
        subtitleStyle.backgroundColor
    } else {
        Color.Black.copy(alpha = 0.40f)
    }

    BoxWithConstraints(modifier = modifier) {
        val controlsWidth = (if (preferences.showNavigation) 68.dp else 0.dp) +
            (if (preferences.showSentenceSpeaker) 34.dp else 0.dp) +
            8.dp
        val fieldMaxWidth = (maxWidth - controlsWidth).coerceAtLeast(120.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (preferences.showNavigation) {
                IconButton(
                    onClick = SubFlowRepository::previousCue,
                    modifier = Modifier.size(34.dp),
                    enabled = cueState?.canGoPrevious == true,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous subtitle",
                        tint = tokens.colors.textPrimary.copy(
                            alpha = if (cueState?.canGoPrevious == true) 0.95f else 0.30f,
                        ),
                    )
                }
            }

            Surface(
                modifier = Modifier.widthIn(max = fieldMaxWidth).wrapContentWidth(),
                color = background,
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    StyledSubtitleText(
                        text = subtitleText,
                        style = subtitleStyle.copy(backgroundColor = Color.Transparent),
                        subFlowPreferences = preferences,
                        subFlowWords = words,
                        selectedWord = currentSelection.word,
                        onSubFlowWordTap = SubFlowRepository::selectWord,
                        fillWidth = false,
                    )
                }
            }

            if (preferences.showSentenceSpeaker) {
                IconButton(
                    onClick = SubFlowRepository::speakCueText,
                    modifier = Modifier.size(34.dp),
                ) {
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
                    modifier = Modifier.size(34.dp),
                    enabled = cueState?.canGoNext == true,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next subtitle",
                        tint = tokens.colors.textPrimary.copy(
                            alpha = if (cueState?.canGoNext == true) 0.95f else 0.30f,
                        ),
                    )
                }
            }
        }
    }
}
