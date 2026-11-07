package com.nuvio.app.features.subflow

import kotlinx.serialization.Serializable

@Serializable
enum class SubFlowWordStatus {
    New,
    Learning,
    Known,
}

@Serializable
data class SubFlowWordEntry(
    val word: String,
    val meaning: String? = null,
    val context: String? = null,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val definition: String? = null,
    val status: SubFlowWordStatus = SubFlowWordStatus.New,
    val createdAtEpochMs: Long = 0L,
    val reviewCount: Int = 0,
    val lastReviewedEpochMs: Long? = null,
)

@Serializable
data class SubFlowPreferences(
    val enabled: Boolean = true,
    val targetLanguage: String = "ar",
    val autoSpeakWord: Boolean = false,
    val translateSentence: Boolean = true,
    val dictionaryEnabled: Boolean = true,
    val speechRate: Int = 48,
    val newWordColor: String = "#FFD65E",
    val learningWordColor: String = "#5CC8F5",
    val showMeaning: Boolean = true,
    val showContext: Boolean = true,
    val showDefinition: Boolean = true,
    val showWordSpeaker: Boolean = true,
    val showSentenceSpeaker: Boolean = true,
    val showNavigation: Boolean = true,
    val showStatus: Boolean = true,
) {
    fun normalized(): SubFlowPreferences = copy(
        targetLanguage = targetLanguage.ifBlank { "ar" },
        speechRate = speechRate.coerceIn(20, 100),
        newWordColor = newWordColor.normalizedHex("#FFD65E"),
        learningWordColor = learningWordColor.normalizedHex("#5CC8F5"),
    )
}

private fun String.normalizedHex(fallback: String): String =
    takeIf { it.matches(Regex("^#[0-9a-fA-F]{6}$")) } ?: fallback

data class SubFlowSelection(
    val word: String,
    val context: String,
    val entry: SubFlowWordEntry = SubFlowWordEntry(word = word, context = context),
    val sentenceTranslation: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class SubFlowLanguage(
    val code: String,
    val label: String,
    val speechTag: String,
)

val SubFlowLanguages: List<SubFlowLanguage> = listOf(
    SubFlowLanguage("ar", "العربية", "ar-SA"),
    SubFlowLanguage("en", "English", "en-US"),
    SubFlowLanguage("fr", "Français", "fr-FR"),
    SubFlowLanguage("es", "Español", "es-ES"),
    SubFlowLanguage("de", "Deutsch", "de-DE"),
    SubFlowLanguage("it", "Italiano", "it-IT"),
    SubFlowLanguage("pt", "Português", "pt-PT"),
    SubFlowLanguage("tr", "Türkçe", "tr-TR"),
    SubFlowLanguage("ru", "Русский", "ru-RU"),
    SubFlowLanguage("fa", "فارسی", "fa-IR"),
    SubFlowLanguage("ur", "اردو", "ur-PK"),
    SubFlowLanguage("hi", "हिन्दी", "hi-IN"),
    SubFlowLanguage("ja", "日本語", "ja-JP"),
    SubFlowLanguage("ko", "한국어", "ko-KR"),
    SubFlowLanguage("zh", "中文", "zh-CN"),
    SubFlowLanguage("nl", "Nederlands", "nl-NL"),
    SubFlowLanguage("pl", "Polski", "pl-PL"),
    SubFlowLanguage("id", "Indonesia", "id-ID"),
    SubFlowLanguage("vi", "Tiếng Việt", "vi-VN"),
    SubFlowLanguage("th", "ไทย", "th-TH"),
)

fun SubFlowLanguageName(code: String): String =
    SubFlowLanguages.firstOrNull { it.code == code }?.label ?: code.uppercase()

fun SubFlowSpeechTag(code: String): String =
    SubFlowLanguages.firstOrNull { it.code == code }?.speechTag
        ?: code.takeIf { it.isNotBlank() }?.let { "$it-$it".uppercase() }
        ?: "en-US"

fun SubFlowWordStatusLabel(status: SubFlowWordStatus): String = when (status) {
    SubFlowWordStatus.New -> "جديدة"
    SubFlowWordStatus.Learning -> "أتعلمها"
    SubFlowWordStatus.Known -> "أعرفها"
}


data class SubFlowCue(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
)

data class SubFlowCueState(
    val cues: List<SubFlowCue>,
    val index: Int,
    val translation: String? = null,
    val isLoading: Boolean = false,
) {
    val current: SubFlowCue? get() = cues.getOrNull(index)
    val canGoPrevious: Boolean get() = index > 0
    val canGoNext: Boolean get() = index < cues.lastIndex
}
