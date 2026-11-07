package com.nuvio.app.features.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.parseSubFlowColor
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowWordEntry
import com.nuvio.app.features.subflow.SubFlowWordStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.subflow_card_definition
import nuvio.composeapp.generated.resources.subflow_card_no_meaning
import nuvio.composeapp.generated.resources.subflow_status_known
import nuvio.composeapp.generated.resources.subflow_status_learning
import nuvio.composeapp.generated.resources.subflow_status_new
import nuvio.composeapp.generated.resources.subflow_words_delete
import nuvio.composeapp.generated.resources.subflow_words_empty
import nuvio.composeapp.generated.resources.subflow_words_filter_all
import nuvio.composeapp.generated.resources.subflow_words_filter_known
import nuvio.composeapp.generated.resources.subflow_words_filter_learning
import nuvio.composeapp.generated.resources.subflow_words_filter_new
import nuvio.composeapp.generated.resources.subflow_words_no_results
import nuvio.composeapp.generated.resources.subflow_words_search
import nuvio.composeapp.generated.resources.subflow_words_title
import org.jetbrains.compose.resources.stringResource

private enum class SubFlowWordsFilter {
    All,
    New,
    Learning,
    Known,
}

internal fun LazyListScope.subFlowWordsContent(isTablet: Boolean) {
    item {
        SubFlowWordsBody(isTablet = isTablet)
    }
}

@Composable
private fun SubFlowWordsBody(isTablet: Boolean) {
    SubFlowRepository.ensureLoaded()
    val words by SubFlowRepository.words.collectAsState()
    val preferences by SubFlowRepository.preferences.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(SubFlowWordsFilter.All.name) }
    var selectedEntry by remember { mutableStateOf<SubFlowWordEntry?>(null) }
    val activeFilter = runCatching { SubFlowWordsFilter.valueOf(filter) }.getOrDefault(SubFlowWordsFilter.All)
    val filtered = remember(words, query, activeFilter) {
        words.values
            .asSequence()
            .filter { entry ->
                when (activeFilter) {
                    SubFlowWordsFilter.All -> true
                    SubFlowWordsFilter.New -> entry.status == SubFlowWordStatus.New
                    SubFlowWordsFilter.Learning -> entry.status == SubFlowWordStatus.Learning
                    SubFlowWordsFilter.Known -> entry.status == SubFlowWordStatus.Known
                }
            }
            .filter { entry ->
                query.isBlank() ||
                    entry.word.contains(query, ignoreCase = true) ||
                    entry.meaning?.contains(query, ignoreCase = true) == true
            }
            .sortedBy { it.word.lowercase() }
            .toList()
    }
    val tokens = MaterialTheme.nuvio

    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 12.dp)) {
        SettingsSection(title = stringResource(Res.string.subflow_words_title), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(stringResource(Res.string.subflow_words_search)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = tokens.colors.textMuted,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = tokens.colors.accent.copy(alpha = 0.75f),
                            unfocusedBorderColor = tokens.colors.borderSubtle,
                            focusedContainerColor = tokens.colors.surface,
                            unfocusedContainerColor = tokens.colors.surface,
                        ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SubFlowWordsFilter.entries.forEach { option ->
                            FilterChip(
                                selected = activeFilter == option,
                                onClick = { filter = option.name },
                                label = { Text(subFlowWordsFilterLabel(option)) },
                            )
                        }
                    }
                }
                SettingsGroupDivider(isTablet = isTablet)
                when {
                    words.isEmpty() -> SubFlowWordsEmptyRow(
                        text = stringResource(Res.string.subflow_words_empty),
                        isTablet = isTablet,
                    )
                    filtered.isEmpty() -> SubFlowWordsEmptyRow(
                        text = stringResource(Res.string.subflow_words_no_results),
                        isTablet = isTablet,
                    )
                    else -> filtered.forEachIndexed { index, entry ->
                        if (index > 0) SettingsGroupDivider(isTablet = isTablet)
                        SettingsNavigationRow(
                            title = entry.word,
                            description = entry.meaning?.takeIf { it.isNotBlank() }
                                ?: stringResource(Res.string.subflow_card_no_meaning),
                            isTablet = isTablet,
                            trailingContent = {
                                Text(
                                    text = subFlowStatusLabel(entry.status),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = parseSubFlowColor(
                                        when (entry.status) {
                                            SubFlowWordStatus.New -> preferences.newWordColor
                                            SubFlowWordStatus.Learning -> preferences.learningWordColor
                                            SubFlowWordStatus.Known -> preferences.knownWordColor
                                        },
                                        tokens.colors.textMuted,
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            onClick = { selectedEntry = entry },
                        )
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        SubFlowWordDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
        )
    }
}

@Composable
private fun SubFlowWordsEmptyRow(text: String, isTablet: Boolean) {
    val tokens = MaterialTheme.nuvio
    Text(
        text = text,
        modifier = Modifier.padding(
            horizontal = if (isTablet) 20.dp else 16.dp,
            vertical = if (isTablet) 18.dp else 16.dp,
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = tokens.colors.textMuted,
    )
}

@Composable
private fun subFlowWordsFilterLabel(filter: SubFlowWordsFilter): String = when (filter) {
    SubFlowWordsFilter.All -> stringResource(Res.string.subflow_words_filter_all)
    SubFlowWordsFilter.New -> stringResource(Res.string.subflow_words_filter_new)
    SubFlowWordsFilter.Learning -> stringResource(Res.string.subflow_words_filter_learning)
    SubFlowWordsFilter.Known -> stringResource(Res.string.subflow_words_filter_known)
}

@Composable
private fun subFlowStatusLabel(status: SubFlowWordStatus): String = when (status) {
    SubFlowWordStatus.New -> stringResource(Res.string.subflow_status_new)
    SubFlowWordStatus.Learning -> stringResource(Res.string.subflow_status_learning)
    SubFlowWordStatus.Known -> stringResource(Res.string.subflow_status_known)
}

@Composable
private fun SubFlowWordDetailDialog(
    entry: SubFlowWordEntry,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    DialogSurface(
        onDismissRequest = onDismiss,
        title = entry.word,
    ) {
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
        Text(
            text = entry.meaning?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.subflow_card_no_meaning),
            style = MaterialTheme.typography.bodyLarge,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        entry.definition?.takeIf { it.isNotBlank() }?.let { definition ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        entry.context?.takeIf { it.isNotBlank() }?.let { context ->
            Text(
                text = context,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
        DialogButtons {
            DialogButton(
                text = subFlowStatusLabel(SubFlowWordStatus.Learning),
                onClick = {
                    SubFlowRepository.setWordStatus(entry.word, SubFlowWordStatus.Learning)
                    onDismiss()
                },
            )
            DialogButton(
                text = subFlowStatusLabel(SubFlowWordStatus.Known),
                style = DialogButtonStyle.Primary,
                onClick = {
                    SubFlowRepository.setWordStatus(entry.word, SubFlowWordStatus.Known)
                    onDismiss()
                },
            )
        }
        DialogButtons {
            DialogButton(
                text = stringResource(Res.string.subflow_words_delete),
                style = DialogButtonStyle.Destructive,
                onClick = {
                    SubFlowRepository.removeWord(entry.word)
                    onDismiss()
                },
            )
            DialogButton(
                text = stringResource(Res.string.action_cancel),
                onClick = onDismiss,
            )
        }
    }
}
