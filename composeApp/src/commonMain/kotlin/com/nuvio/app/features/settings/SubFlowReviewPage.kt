package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowWordEntry
import com.nuvio.app.features.subflow.SubFlowWordStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.subflow_review_description
import nuvio.composeapp.generated.resources.subflow_review_done
import nuvio.composeapp.generated.resources.subflow_review_empty
import nuvio.composeapp.generated.resources.subflow_review_no_meaning
import nuvio.composeapp.generated.resources.subflow_review_progress
import nuvio.composeapp.generated.resources.subflow_review_reveal
import nuvio.composeapp.generated.resources.subflow_review_title
import nuvio.composeapp.generated.resources.subflow_status_known
import nuvio.composeapp.generated.resources.subflow_status_learning
import nuvio.composeapp.generated.resources.subflow_speak_word
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.subFlowReviewContent(isTablet: Boolean) {
    item {
        SubFlowReviewBody(isTablet = isTablet)
    }
}

@Composable
private fun SubFlowReviewBody(isTablet: Boolean) {
    SubFlowRepository.ensureLoaded()
    val words by SubFlowRepository.words.collectAsState()
    val queue = remember(words) { SubFlowRepository.reviewQueue() }
    val entry = queue.firstOrNull()
    var revealed by remember(entry?.word) { mutableStateOf(false) }
    val tokens = MaterialTheme.nuvio

    SettingsSection(title = stringResource(Res.string.subflow_review_title), isTablet = isTablet) {
        SettingsGroup(isTablet = isTablet) {
            if (entry == null) {
                Text(
                    text = if (words.isEmpty()) {
                        stringResource(Res.string.subflow_review_empty)
                    } else {
                        stringResource(Res.string.subflow_review_done)
                    },
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = if (isTablet) 18.dp else 16.dp,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textMuted,
                )
            } else {
                SubFlowReviewCard(
                    entry = entry,
                    remaining = queue.size,
                    revealed = revealed,
                    onReveal = { revealed = true },
                    onStatus = { status ->
                        SubFlowRepository.markReviewed(entry.word)
                        SubFlowRepository.setWordStatus(entry.word, status)
                        revealed = false
                    },
                    isTablet = isTablet,
                )
            }
        }
    }
}

@Composable
private fun SubFlowReviewCard(
    entry: SubFlowWordEntry,
    remaining: Int,
    revealed: Boolean,
    onReveal: () -> Unit,
    onStatus: (SubFlowWordStatus) -> Unit,
    isTablet: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 20.dp else 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.subflow_review_progress, 1, remaining),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.textMuted,
        )
        Text(
            text = entry.word,
            style = MaterialTheme.typography.headlineLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
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
        entry.context?.takeIf { it.isNotBlank() }?.let { context ->
            Text(
                text = context,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textSecondary,
            )
        }

        IconButton(onClick = { SubFlowRepository.speakWordByText(entry.word) }) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = stringResource(Res.string.subflow_speak_word),
                tint = tokens.colors.accent,
            )
        }

        if (revealed) {
            Text(
                text = entry.meaning?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.subflow_review_no_meaning),
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            entry.definition?.takeIf { it.isNotBlank() }?.let { definition ->
                Text(
                    text = definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.colors.textSecondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onStatus(SubFlowWordStatus.Learning) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.warning.copy(alpha = 0.18f),
                        contentColor = tokens.colors.warning,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(Res.string.subflow_status_learning), maxLines = 1)
                }
                Button(
                    onClick = { onStatus(SubFlowWordStatus.Known) },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(stringResource(Res.string.subflow_status_known), maxLines = 1)
                }
            }
        } else {
            Button(onClick = onReveal) {
                Text(stringResource(Res.string.subflow_review_reveal))
            }
        }

        Text(
            text = stringResource(Res.string.subflow_review_description),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.colors.textMuted,
        )
    }
}
