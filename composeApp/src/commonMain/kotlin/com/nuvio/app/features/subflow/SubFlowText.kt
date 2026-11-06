package com.nuvio.app.features.subflow

internal data class SubFlowWordHit(
    val word: String,
    val line: String,
)

internal object SubFlowText {
    private val wordRegex = Regex("""[\p{L}\p{M}][\p{L}\p{M}'’\-]*""")

    fun wordRanges(text: String): List<IntRange> =
        wordRegex.findAll(text).map { it.range }.toList()

    fun wordAt(text: String, offset: Int): SubFlowWordHit? {
        if (text.isBlank()) return null
        val safeOffset = offset.coerceIn(0, text.length)
        val match = wordRegex.findAll(text).firstOrNull { result ->
            safeOffset >= result.range.first && safeOffset <= result.range.last + 1
        } ?: return null
        val word = match.value.trim()
        if (word.isBlank()) return null
        val lineStart = text.lastIndexOf('\n', (match.range.first - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', match.range.first).let { if (it < 0) text.length else it }
        val line = text.substring(lineStart, lineEnd).trim()
        return SubFlowWordHit(word = word, line = line)
    }
}
