package com.kopyamatik.app.data

object TextChunker {

    private const val MAX_CHUNK_LENGTH = 500

    /**
     * Splits text into pieces of at most 500 characters, ensuring that words
     * are never cut in half and whitespace is preserved naturally.
     */
    fun splitIntoChunks(text: String, maxLimit: Int = MAX_CHUNK_LENGTH): List<String> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return emptyList()
        if (cleanText.length <= maxLimit) return listOf(cleanText)

        val words = cleanText.split("\\s+".toRegex())
        val chunks = mutableListOf<String>()
        val currentChunk = StringBuilder()

        for (word in words) {
            if (word.isEmpty()) continue

            // If a single word itself is longer than maxLimit (rare anomaly), split it safely
            if (word.length > maxLimit) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk.clear()
                }
                var start = 0
                while (start < word.length) {
                    val end = (start + maxLimit).coerceAtMost(word.length)
                    chunks.add(word.substring(start, end))
                    start = end
                }
                continue
            }

            // Check if adding the word exceeds the chunk limit
            val additionalLength = if (currentChunk.isEmpty()) word.length else 1 + word.length
            if (currentChunk.length + additionalLength <= maxLimit) {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ")
                }
                currentChunk.append(word)
            } else {
                chunks.add(currentChunk.toString().trim())
                currentChunk.clear()
                currentChunk.append(word)
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }
}
