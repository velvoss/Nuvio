package com.nuvio.app.features.subflow

import com.nuvio.app.core.network.createApiHttpClient
import com.nuvio.app.core.network.readBoundedResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class SubFlowDictionaryResult(
    val phonetic: String?,
    val partOfSpeech: String?,
    val definition: String?,
)

internal object SubFlowTranslationService {
    private val client: HttpClient by lazy { createApiHttpClient() }
    private val json = Json { ignoreUnknownKeys = true }
    private val translationCache = mutableMapOf<String, String>()
    private val dictionaryCache = mutableMapOf<String, SubFlowDictionaryResult?>()

    suspend fun translate(text: String, targetLanguage: String): String? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val key = "$targetLanguage|$clean"
        translationCache[key]?.let { return it }

        val encoded = clean.encodeURLParameter()
        val googleUrl = "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=auto&tl=${targetLanguage.encodeURLParameter()}&dt=t&q=$encoded"
        val google = runCatching { fetchText(googleUrl) }
            .getOrNull()
            ?.let { parseGoogleTranslation(it) }
            ?.takeIf { it.isNotBlank() }
        if (google != null) {
            translationCache[key] = google
            return google
        }

        val memoryUrl = "https://api.mymemory.translated.net/get" +
            "?q=$encoded&langpair=${"Autodetect".encodeURLParameter()}|${targetLanguage.encodeURLParameter()}"
        val memory = runCatching { fetchText(memoryUrl) }
            .getOrNull()
            ?.let { parseMyMemoryTranslation(it) }
            ?.takeIf { it.isNotBlank() }
        if (memory != null) translationCache[key] = memory
        return memory
    }

    suspend fun lookup(word: String): SubFlowDictionaryResult? {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return null
        if (dictionaryCache.containsKey(clean)) return dictionaryCache[clean]

        val url = "https://api.dictionaryapi.dev/api/v2/entries/en/${clean.encodeURLParameter()}"
        val result = runCatching { fetchText(url) }
            .getOrNull()
            ?.let { parseDictionary(it) }
        dictionaryCache[clean] = result
        return result
    }

    private suspend fun fetchText(url: String): String {
        val response = client.get(url)
        return readBoundedResponseBody(response.bodyAsChannel(), response.contentLength())
    }

    private fun parseGoogleTranslation(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body).jsonArray
        root.firstOrNull()
            ?.jsonArray
            ?.mapNotNull { segment ->
                segment.jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNull
            }
            ?.joinToString("")
    }.getOrNull()

    private fun parseMyMemoryTranslation(body: String): String? = runCatching {
        json.parseToJsonElement(body)
            .jsonObject["responseData"]
            ?.jsonObject
            ?.get("translatedText")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    private fun parseDictionary(body: String): SubFlowDictionaryResult? = runCatching {
        val entry = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonObject ?: return null
        val phonetic = entry["phonetic"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: entry["phonetics"]
                ?.jsonArray
                ?.firstNotNullOfOrNull { item ->
                    item.jsonObject["text"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                }
        val meaning = entry["meanings"]?.jsonArray?.firstOrNull()?.jsonObject
        val partOfSpeech = meaning?.get("partOfSpeech")?.jsonPrimitive?.contentOrNull
        val definition = meaning
            ?.get("definitions")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("definition")
            ?.jsonPrimitive
            ?.contentOrNull
        SubFlowDictionaryResult(
            phonetic = phonetic,
            partOfSpeech = partOfSpeech,
            definition = definition,
        ).takeIf { it.phonetic != null || it.partOfSpeech != null || it.definition != null }
    }.getOrNull()
}
