package com.nuvio.app.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.DialogButton
import com.nuvio.app.core.ui.DialogButtonStyle
import com.nuvio.app.core.ui.DialogButtons
import com.nuvio.app.core.ui.DialogSurface
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.player.parseSubFlowColor
import com.nuvio.app.features.subflow.SubFlowCardSection
import com.nuvio.app.features.subflow.SubFlowLanguageName
import com.nuvio.app.features.subflow.SubFlowLanguages
import com.nuvio.app.features.subflow.SubFlowRepository
import com.nuvio.app.features.subflow.SubFlowWordStatus
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.subflow_language_dialog
import nuvio.composeapp.generated.resources.subflow_export_copied
import nuvio.composeapp.generated.resources.subflow_export_description
import nuvio.composeapp.generated.resources.subflow_export_title
import nuvio.composeapp.generated.resources.subflow_import_description
import nuvio.composeapp.generated.resources.subflow_import_empty
import nuvio.composeapp.generated.resources.subflow_import_failed
import nuvio.composeapp.generated.resources.subflow_import_success
import nuvio.composeapp.generated.resources.subflow_import_title
import nuvio.composeapp.generated.resources.subflow_review_description
import nuvio.composeapp.generated.resources.subflow_review_title
import nuvio.composeapp.generated.resources.subflow_settings_about
import nuvio.composeapp.generated.resources.subflow_settings_about_description
import nuvio.composeapp.generated.resources.subflow_settings_auto_speak
import nuvio.composeapp.generated.resources.subflow_settings_colors_section
import nuvio.composeapp.generated.resources.subflow_settings_known_color
import nuvio.composeapp.generated.resources.subflow_settings_color_hex
import nuvio.composeapp.generated.resources.subflow_settings_color_invalid
import nuvio.composeapp.generated.resources.subflow_settings_color_save
import nuvio.composeapp.generated.resources.subflow_settings_card_order_section
import nuvio.composeapp.generated.resources.subflow_settings_move_up
import nuvio.composeapp.generated.resources.subflow_settings_move_down
import nuvio.composeapp.generated.resources.subflow_settings_card_section
import nuvio.composeapp.generated.resources.subflow_settings_show_meaning
import nuvio.composeapp.generated.resources.subflow_settings_show_context
import nuvio.composeapp.generated.resources.subflow_settings_show_definition
import nuvio.composeapp.generated.resources.subflow_settings_show_word_speaker
import nuvio.composeapp.generated.resources.subflow_settings_show_sentence_speaker
import nuvio.composeapp.generated.resources.subflow_settings_show_navigation
import nuvio.composeapp.generated.resources.subflow_settings_show_status
import nuvio.composeapp.generated.resources.subflow_settings_clear_action
import nuvio.composeapp.generated.resources.subflow_settings_clear_message
import nuvio.composeapp.generated.resources.subflow_settings_clear_title
import nuvio.composeapp.generated.resources.subflow_settings_clear_words
import nuvio.composeapp.generated.resources.subflow_settings_dictionary
import nuvio.composeapp.generated.resources.subflow_settings_dictionary_description
import nuvio.composeapp.generated.resources.subflow_settings_enable
import nuvio.composeapp.generated.resources.subflow_settings_enable_description
import nuvio.composeapp.generated.resources.subflow_settings_saved_words
import nuvio.composeapp.generated.resources.subflow_settings_saved_words_value
import nuvio.composeapp.generated.resources.subflow_settings_speech_rate
import nuvio.composeapp.generated.resources.subflow_settings_target_language
import nuvio.composeapp.generated.resources.subflow_settings_translate_sentence
import nuvio.composeapp.generated.resources.subflow_title
import nuvio.composeapp.generated.resources.subflow_words_title
import org.jetbrains.compose.resources.stringResource

private enum class SubFlowColorTarget {
    Learning,
    Known,
}

internal fun LazyListScope.subFlowSettingsContent(
    isTablet: Boolean,
    onPageChange: (SettingsPage) -> Unit,
) {
    item {
        SubFlowSettingsBody(isTablet = isTablet, onPageChange = onPageChange)
    }
}

@Composable
private fun SubFlowSettingsBody(
    isTablet: Boolean,
    onPageChange: (SettingsPage) -> Unit,
) {
    SubFlowRepository.ensureLoaded()
    val preferences by SubFlowRepository.preferences.collectAsState()
    val words by SubFlowRepository.words.collectAsState()
    val clipboard = LocalClipboardManager.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var colorTarget by remember { mutableStateOf<SubFlowColorTarget?>(null) }
    var colorDraft by remember { mutableStateOf("") }
    val learningCount = words.values.count { it.status == SubFlowWordStatus.Learning }
    val exportCopiedMessage = stringResource(Res.string.subflow_export_copied)
    val importEmptyMessage = stringResource(Res.string.subflow_import_empty)
    val importSuccessMessage = stringResource(Res.string.subflow_import_success)
    val importFailedMessage = stringResource(Res.string.subflow_import_failed)

    Column(verticalArrangement = Arrangement.spacedBy(if (isTablet) 18.dp else 12.dp)) {
        SettingsSection(title = stringResource(Res.string.subflow_title), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_enable),
                    description = stringResource(Res.string.subflow_settings_enable_description),
                    checked = preferences.enabled,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setEnabled,
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_target_language), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.subflow_settings_target_language),
                    description = SubFlowLanguageName(preferences.targetLanguage),
                    isTablet = isTablet,
                    onClick = { showLanguageDialog = true },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_auto_speak),
                    checked = preferences.autoSpeakWord,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setAutoSpeakWord,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_translate_sentence),
                    checked = preferences.translateSentence,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setTranslateSentence,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_dictionary),
                    description = stringResource(Res.string.subflow_settings_dictionary_description),
                    checked = preferences.dictionaryEnabled,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setDictionaryEnabled,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSliderRow(
                    title = stringResource(Res.string.subflow_settings_speech_rate),
                    value = preferences.speechRate,
                    valueRange = 20..100,
                    step = 1,
                    isTablet = isTablet,
                    formatValue = { value -> "$value%" },
                    onValueChange = SubFlowRepository::setSpeechRate,
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_card_section), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_meaning),
                    checked = preferences.showMeaning,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowMeaning,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_context),
                    checked = preferences.showContext,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowContext,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_definition),
                    checked = preferences.showDefinition,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowDefinition,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_word_speaker),
                    checked = preferences.showWordSpeaker,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowWordSpeaker,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_sentence_speaker),
                    checked = preferences.showSentenceSpeaker,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowSentenceSpeaker,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_navigation),
                    checked = preferences.showNavigation,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowNavigation,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.subflow_settings_show_status),
                    checked = preferences.showStatus,
                    isTablet = isTablet,
                    onCheckedChange = SubFlowRepository::setShowStatus,
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_colors_section), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SubFlowColorSettingRow(
                    title = stringResource(Res.string.subflow_settings_learning_color),
                    colorHex = preferences.learningWordColor,
                    isTablet = isTablet,
                    onClick = {
                        colorDraft = preferences.learningWordColor
                        colorTarget = SubFlowColorTarget.Learning
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SubFlowColorSettingRow(
                    title = stringResource(Res.string.subflow_settings_known_color),
                    colorHex = preferences.knownWordColor,
                    isTablet = isTablet,
                    onClick = {
                        colorDraft = preferences.knownWordColor
                        colorTarget = SubFlowColorTarget.Known
                    },
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_card_order_section), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                preferences.cardSectionOrder.forEachIndexed { index, section ->
                    if (index > 0) SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = subFlowCardSectionLabel(section),
                        isTablet = isTablet,
                        trailingContent = {
                            IconButton(
                                onClick = { SubFlowRepository.moveCardSection(section, -1) },
                                enabled = index > 0,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(Res.string.subflow_settings_move_up),
                                    tint = MaterialTheme.nuvio.colors.textMuted,
                                )
                            }
                            IconButton(
                                onClick = { SubFlowRepository.moveCardSection(section, 1) },
                                enabled = index < preferences.cardSectionOrder.lastIndex,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(Res.string.subflow_settings_move_down),
                                    tint = MaterialTheme.nuvio.colors.textMuted,
                                )
                            }
                        },
                        onClick = {},
                    )
                }
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_saved_words), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.subflow_words_title),
                    description = stringResource(
                        Res.string.subflow_settings_saved_words_value,
                        words.size,
                        learningCount,
                    ),
                    isTablet = isTablet,
                    onClick = { onPageChange(SettingsPage.SubFlowWords) },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.subflow_review_title),
                    description = stringResource(Res.string.subflow_review_description),
                    isTablet = isTablet,
                    onClick = { onPageChange(SettingsPage.SubFlowReview) },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.subflow_export_title),
                    description = stringResource(Res.string.subflow_export_description),
                    enabled = words.isNotEmpty(),
                    isTablet = isTablet,
                    onClick = {
                        clipboard.setText(AnnotatedString(SubFlowRepository.exportJson()))
                        feedback = exportCopiedMessage
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.subflow_import_title),
                    description = stringResource(Res.string.subflow_import_description),
                    isTablet = isTablet,
                    onClick = {
                        val clipboardText = clipboard.getText()?.text
                        feedback = when {
                            clipboardText.isNullOrBlank() -> importEmptyMessage
                            SubFlowRepository.importJson(clipboardText) -> importSuccessMessage
                            else -> importFailedMessage
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsClickableRow(
                    title = stringResource(Res.string.subflow_settings_clear_words),
                    enabled = words.isNotEmpty(),
                    isTablet = isTablet,
                    onClick = { showClearDialog = true },
                )
            }
            feedback?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(
                        horizontal = if (isTablet) 20.dp else 16.dp,
                        vertical = 6.dp,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        SettingsSection(title = stringResource(Res.string.subflow_settings_about), isTablet = isTablet) {
            SettingsGroup(isTablet = isTablet) {
                SubFlowInfoRow(
                    title = stringResource(Res.string.subflow_settings_about),
                    value = stringResource(Res.string.subflow_settings_about_description),
                    isTablet = isTablet,
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            title = stringResource(Res.string.subflow_language_dialog),
            options = SubFlowLanguages.map { language ->
                LanguageSelectionOption(language.code, language.label)
            },
            selectedValue = preferences.targetLanguage,
            onSelect = { value ->
                value?.let(SubFlowRepository::setTargetLanguage)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }

    colorTarget?.let { target ->
        val validColor = colorDraft.matches(Regex("^#[0-9a-fA-F]{6}$"))
        DialogSurface(
            onDismissRequest = { colorTarget = null },
            title = if (target == SubFlowColorTarget.Learning) {
                stringResource(Res.string.subflow_settings_learning_color)
            } else {
                stringResource(Res.string.subflow_settings_known_color)
            },
        ) {
            OutlinedTextField(
                value = colorDraft,
                onValueChange = { colorDraft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.subflow_settings_color_hex)) },
                isError = colorDraft.isNotBlank() && !validColor,
            )
            if (colorDraft.isNotBlank() && !validColor) {
                Text(
                    text = stringResource(Res.string.subflow_settings_color_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(parseSubFlowColor(colorDraft, MaterialTheme.nuvio.colors.textMuted))
                    .border(1.dp, MaterialTheme.nuvio.colors.borderDefault, CircleShape),
            )
            DialogButtons {
                DialogButton(
                    text = stringResource(Res.string.subflow_settings_color_save),
                    enabled = validColor,
                    style = DialogButtonStyle.Primary,
                    onClick = {
                        when (target) {
                            SubFlowColorTarget.Learning -> SubFlowRepository.setLearningWordColor(colorDraft)
                            SubFlowColorTarget.Known -> SubFlowRepository.setKnownWordColor(colorDraft)
                        }
                        colorTarget = null
                    },
                )
                DialogButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = { colorTarget = null },
                )
            }
        }
    }

    if (showClearDialog) {
        DialogSurface(
            onDismissRequest = { showClearDialog = false },
            title = stringResource(Res.string.subflow_settings_clear_title),
            message = stringResource(Res.string.subflow_settings_clear_message),
        ) {
            DialogButtons {
                DialogButton(
                    text = stringResource(Res.string.subflow_settings_clear_action),
                    style = DialogButtonStyle.Destructive,
                    onClick = {
                        SubFlowRepository.clearWords()
                        showClearDialog = false
                    },
                )
                DialogButton(
                    text = stringResource(Res.string.action_cancel),
                    onClick = { showClearDialog = false },
                )
            }
        }
    }
}

@Composable
private fun subFlowCardSectionLabel(section: SubFlowCardSection): String = when (section) {
    SubFlowCardSection.Meaning -> stringResource(Res.string.subflow_settings_show_meaning)
    SubFlowCardSection.Context -> stringResource(Res.string.subflow_settings_show_context)
    SubFlowCardSection.Definition -> stringResource(Res.string.subflow_settings_show_definition)
}

@Composable
private fun SubFlowColorSettingRow(
    title: String,
    colorHex: String,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = colorHex,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(parseSubFlowColor(colorHex, tokens.colors.textMuted))
                .border(1.dp, tokens.colors.borderDefault, CircleShape),
        )
    }
}

@Composable
private fun SubFlowInfoRow(
    title: String,
    value: String,
    isTablet: Boolean,
) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isTablet) 20.dp else 16.dp,
                vertical = if (isTablet) 16.dp else 14.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
        }
    }
}
