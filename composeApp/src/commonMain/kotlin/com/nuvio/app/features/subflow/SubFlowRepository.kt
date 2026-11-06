package com.nuvio.app.features.subflow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

object SubFlowRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val meaningCache = mutableMapOf<String, String>()
    private val selectionRequest = mutableMapOf<String, Long>()
    private var requestCounter = 0L
    private var loaded = false

    private val _preferences = MutableStateFlow(SubFlowPreferences())
    val preferences: StateFlow<SubFlowPreferences> = _preferences.asStateFlow()

    private val _words = MutableStateFlow<Map<String, SubFlowWordEntry>>(emptyMap())
    val words: StateFlow<Map<String, SubFlowWordEntry>> = _words.asStateFlow()

    private val _selection = MutableStateFlow<SubFlowSelection?>(null)
    val selection: StateFlow<SubFlowSelection?> = _selection.asStateFlow()

    val enabledNow: Boolean
        get() {
            ensureLoaded()
            return _preferences.value.enabled
        }

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        SubFlowStorage.loadPreferencesJson()?.let { raw ->
            runCatching { json.decodeFromString<SubFlowPreferences>(raw) }
                .getOrNull()
                ?.normalized()
                ?.let { _preferences.value = it }
        }
        SubFlowStorage.loadWordsJson()?.let { raw ->
            runCatching { json.decodeFromString<Map<String, SubFlowWordEntry>>(raw) }
                .getOrNull()
                ?.let { _words.value = it }
        }
    }

    fun setEnabled(enabled: Boolean) = updatePreferences { copy(enabled = enabled) }

    fun setTargetLanguage(language: String) = updatePreferences { copy(targetLanguage = language) }

    fun setAutoSpeakWord(enabled: Boolean) = updatePreferences { copy(autoSpeakWord = enabled) }

    fun setTranslateSentence(enabled: Boolean) = updatePreferences { copy(translateSentence = enabled) }

    fun setDictionaryEnabled(enabled: Boolean) = updatePreferences { copy(dictionaryEnabled = enabled) }

    fun setSpeechRate(rate: Int) = updatePreferences { copy(speechRate = rate) }

    fun setNewWordColor(colorHex: String) = updatePreferences { copy(newWordColor = colorHex) }

    fun setLearningWordColor(colorHex: String) = updatePreferences { copy(learningWordColor = colorHex) }

    fun selectWord(word: String, context: String) {
        ensureLoaded()
        if (!_preferences.value.enabled) return
        val display = word.trim()
        if (display.isBlank()) return
        val key = display.lowercase()
        val existing = _words.value[key]
        val initial = existing ?: SubFlowWordEntry(
            word = display,
            context = context,
            createdAtEpochMs = 0L,
        )
        val requestId = ++requestCounter
        selectionRequest[key] = requestId

        _selection.value = SubFlowSelection(
            word = display,
            context = context,
            entry = initial,
            sentenceTranslation = null,
            isLoading = existing?.meaning.isNullOrBlank(),
            errorMessage = null,
        )

        if (_preferences.value.autoSpeakWord) {
            speakWord(display)
        }

        scope.launch {
            val prefs = _preferences.value
            var meaning = existing?.meaning ?: meaningCache[key]
            var dictionary = SubFlowDictionaryResult(
                phonetic = existing?.phonetic,
                partOfSpeech = existing?.partOfSpeech,
                definition = existing?.definition,
            )
            var sentenceTranslation: String? = null

            if (meaning.isNullOrBlank()) {
                meaning = runCatching {
                    SubFlowTranslationService.translate(display, prefs.targetLanguage)
                }.getOrNull()
                if (!meaning.isNullOrBlank()) meaningCache[key] = meaning
            }

            if (prefs.dictionaryEnabled && dictionary.definition.isNullOrBlank()) {
                val fetched = runCatching { SubFlowTranslationService.lookup(display) }.getOrNull()
                if (fetched != null) {
                    dictionary = SubFlowDictionaryResult(
                        phonetic = fetched.phonetic ?: dictionary.phonetic,
                        partOfSpeech = fetched.partOfSpeech ?: dictionary.partOfSpeech,
                        definition = fetched.definition ?: dictionary.definition,
                    )
                }
            }

            if (prefs.translateSentence && context.isNotBlank()) {
                sentenceTranslation = runCatching {
                    SubFlowTranslationService.translate(context, prefs.targetLanguage)
                }.getOrNull()
            }

            if (selectionRequest[key] != requestId) return@launch
            val current = _selection.value ?: return@launch
            if (current.word.lowercase() != key) return@launch

            val updatedEntry = current.entry.copy(
                meaning = meaning?.takeIf { it.isNotBlank() } ?: current.entry.meaning,
                phonetic = dictionary?.phonetic ?: current.entry.phonetic,
                partOfSpeech = dictionary?.partOfSpeech ?: current.entry.partOfSpeech,
                definition = dictionary?.definition ?: current.entry.definition,
            )
            _selection.value = current.copy(
                entry = updatedEntry,
                sentenceTranslation = sentenceTranslation,
                isLoading = false,
                errorMessage = if (meaning.isNullOrBlank() && updatedEntry.meaning.isNullOrBlank()) {
                    "تعذّر جلب المعنى"
                } else {
                    null
                },
            )
        }
    }

    fun closeCard() {
        _selection.value = null
        SubFlowSpeech.stop()
    }

    fun speakWord() {
        val selection = _selection.value ?: return
        speakWord(selection.word)
    }

    fun speakWordByText(word: String) {
        speakWord(word.trim())
    }

    private fun speakWord(word: String) {
        val rate = _preferences.value.speechRate.coerceIn(20, 100) / 100f
        SubFlowSpeech.speak(word, detectSpeechTag(word), rate)
    }

    fun speakMeaning() {
        val selection = _selection.value ?: return
        val meaning = selection.entry.meaning ?: return
        val prefs = _preferences.value
        val rate = prefs.speechRate.coerceIn(20, 100) / 100f
        SubFlowSpeech.speak(meaning, SubFlowSpeechTag(prefs.targetLanguage), rate)
    }

    fun setStatus(status: SubFlowWordStatus) {
        val selection = _selection.value ?: return
        val key = selection.word.lowercase()
        val existing = _words.value[key]
        val now = Clock.System.now().toEpochMilliseconds()
        val base = existing ?: selection.entry
        val entry = base.copy(
            word = selection.word,
            meaning = selection.entry.meaning ?: base.meaning,
            context = selection.context.ifBlank { base.context },
            phonetic = selection.entry.phonetic ?: base.phonetic,
            partOfSpeech = selection.entry.partOfSpeech ?: base.partOfSpeech,
            definition = selection.entry.definition ?: base.definition,
            status = status,
            createdAtEpochMs = base.createdAtEpochMs.takeIf { it > 0L } ?: now,
        )
        _words.value = _words.value + (key to entry)
        persistWords()
        _selection.value = null
        SubFlowSpeech.stop()
    }

    fun setWordStatus(word: String, status: SubFlowWordStatus) {
        val key = word.trim().lowercase()
        if (key.isEmpty()) return
        val existing = _words.value[key] ?: return
        _words.value = _words.value + (
            key to existing.copy(
                word = existing.word.ifBlank { word },
                status = status,
            )
        )
        persistWords()
    }

    fun markReviewed(word: String) {
        val key = word.trim().lowercase()
        if (key.isEmpty()) return
        val existing = _words.value[key] ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        _words.value = _words.value + (
            key to existing.copy(
                reviewCount = existing.reviewCount + 1,
                lastReviewedEpochMs = now,
            )
        )
        persistWords()
    }

    fun reviewQueue(): List<SubFlowWordEntry> {
        ensureLoaded()
        return _words.value.values
            .filter { it.status == SubFlowWordStatus.Learning }
            .sortedWith(
                compareBy(
                    { it.lastReviewedEpochMs ?: 0L },
                    { it.word.lowercase() },
                )
            )
    }

    fun exportJson(): String {
        ensureLoaded()
        return json.encodeToString(_words.value)
    }

    fun importJson(text: String): Boolean {
        ensureLoaded()
        val imported = runCatching {
            json.decodeFromString<Map<String, SubFlowWordEntry>>(text.trim())
        }.getOrNull() ?: return false
        if (imported.isEmpty()) return false
        _words.value = _words.value + imported
        persistWords()
        return true
    }

    fun removeWord(word: String) {
        val key = word.trim().lowercase()
        if (key.isEmpty()) return
        _words.value = _words.value - key
        persistWords()
    }

    fun clearWords() {
        _words.value = emptyMap()
        meaningCache.clear()
        persistWords()
    }

    fun totalWords(): Int = _words.value.size

    fun learningWords(): Int = _words.value.values.count { it.status == SubFlowWordStatus.Learning }

    private fun updatePreferences(transform: SubFlowPreferences.() -> SubFlowPreferences) {
        ensureLoaded()
        val next = _preferences.value.transform().normalized()
        _preferences.value = next
        SubFlowStorage.savePreferencesJson(json.encodeToString(next))
    }

    private fun persistWords() {
        SubFlowStorage.saveWordsJson(json.encodeToString(_words.value))
    }

    private fun detectSpeechTag(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return SubFlowSpeechTag(_preferences.value.targetLanguage)
        val first = trimmed.first()
        return when {
            first in '\u0600'..'\u06FF' -> "ar-SA"
            first in '\u0590'..'\u05FF' -> "he-IL"
            first in '\u0400'..'\u04FF' -> "ru-RU"
            first in '\u0900'..'\u097F' -> "hi-IN"
            first in '\u3040'..'\u30FF' -> "ja-JP"
            first in '\uAC00'..'\uD7AF' -> "ko-KR"
            first in '\u4E00'..'\u9FFF' -> "zh-CN"
            first in '\u0E00'..'\u0E7F' -> "th-TH"
            else -> "en-US"
        }
    }
}
