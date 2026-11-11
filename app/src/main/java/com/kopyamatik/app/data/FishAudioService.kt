package com.kopyamatik.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class FishAudioService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TTS_ENDPOINT = "https://api.fish.audio/v1/tts"
    }

    private val gson = Gson()

    /**
     * Validates Fish Audio API Key and Model ID by generating a tiny 1-word TTS preview.
     */
    suspend fun validateApiKeyAndModel(apiKey: String, voiceModelId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val payload = mapOf(
                    "text" to "Test",
                    "reference_id" to voiceModelId.trim(),
                    "format" to "mp3"
                )
                val requestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(TTS_ENDPOINT)
                    .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success(true)
                    } else {
                        val error = response.body?.string().orEmpty()
                        Result.failure(Exception("Fish Audio Doğrulama Hatası (${response.code}): $error"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Synthesizes text in <=500 character chunks and concatenates resulting MP3 bytes into a single output file.
     * Reports progress via the [onProgress] callback: (currentChunkIndex, totalChunks).
     */
    suspend fun synthesizeTextToFile(
        apiKey: String,
        voiceModelId: String,
        fullText: String,
        outputFile: File,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val chunks = TextChunker.splitIntoChunks(fullText, maxLimit = 500)
            if (chunks.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Seslendirilecek metin bulunamadı."))
            }

            // Ensure parent directory exists
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) {
                outputFile.delete()
            }

            val total = chunks.size
            FileOutputStream(outputFile, true).use { fileOutputStream ->
                for ((index, chunk) in chunks.withIndex()) {
                    val current = index + 1
                    onProgress(current, total)

                    val payload = mapOf(
                        "text" to chunk,
                        "reference_id" to voiceModelId.trim(),
                        "format" to "mp3"
                    )

                    val requestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(TTS_ENDPOINT)
                        .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string().orEmpty()
                            return@withContext Result.failure(
                                Exception("Fish Audio TTS Hatası (Parça $current/$total, Kod ${response.code}): $errorBody")
                            )
                        }

                        val responseBody = response.body
                            ?: return@withContext Result.failure(Exception("Boş ses yanıtı alındı."))

                        // Write MP3 byte stream sequentially
                        responseBody.byteStream().use { input ->
                            input.copyTo(fileOutputStream)
                        }
                        fileOutputStream.flush()
                    }
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
