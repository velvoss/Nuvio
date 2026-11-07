package com.nuvio.app.features.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowText
import com.nuvio.app.features.subflow.SubFlowWordStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.subflow_speak_word
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SubFlowElevatedSubtitleBar(
    subtitleStyle: SubtitleStyleState,
    barWidth: Dp,
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

    BoxWithConstraints(modifier = modifier.width(barWidth)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (preferences.showNavigation) {
                IconButton(
                    onClick = SubFlowRepository::previousCue,
                    modifier = Modifier.size(30.dp),
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
                modifier = Modifier.weight(1f),
                color = background,
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    val fontFamily = rememberSubtitleFontFamily(subtitleStyle.appearance.fontId)
                    val annotatedText = remember(subtitleText, preferences, words, currentSelection.word) {
                        buildAnnotatedString {
                            append(subtitleText)
                            SubFlowText.wordRanges(subtitleText).forEach { range ->
                                val word = subtitleText.substring(range.first, range.last + 1)
                                val key = word.lowercase()
                                val status = words[key]?.status
                                val wordColor = when (status) {
                                    SubFlowWordStatus.Learning -> parseSubFlowColor(
                                        preferences.learningWordColor,
                                        Color(0xFF5CC8F5),
                                    )
                                    SubFlowWordStatus.Known -> parseSubFlowColor(
                                        preferences.knownWordColor,
                                        Color(0xFF4DD18C),
                                    )
                                    else -> null
                                }
                                if (wordColor != null) {
                                    addStyle(SpanStyle(color = wordColor), range.first, range.last + 1)
                                }
                                if (key == currentSelection.word.lowercase()) {
                                    addStyle(
                                        SpanStyle(background = Color.White.copy(alpha = 0.22f)),
                                        range.first,
                                        range.last + 1,
                                    )
                                }
                            }
                        }
                    }
                    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Text(
                        text = annotatedText,
                        modifier = Modifier.pointerInput(subtitleText, textLayout) {
                            detectTapGestures { position ->
                                val layout = textLayout ?: return@detectTapGestures
                                val offset = layout.getOffsetForPosition(position)
                                SubFlowText.wordAt(subtitleText, offset)?.let { hit ->
                                    SubFlowRepository.selectWord(hit.word, hit.line)
                                }
                            }
                        },
                        style = TextStyle(
                            color = subtitleStyle.textColor,
                            fontFamily = fontFamily,
                            fontSize = subtitleStyle.fontSizeSp.sp,
                            fontWeight = if (subtitleStyle.bold) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        ),
                        onTextLayout = { textLayout = it },
                    )
                }
            }

            if (preferences.showSentenceSpeaker) {
                IconButton(
                    onClick = SubFlowRepository::speakCueText,
                    modifier = Modifier.size(30.dp),
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
                    modifier = Modifier.size(30.dp),
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
