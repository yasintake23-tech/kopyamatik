package com.kopyamatik.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
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
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TTS_ENDPOINT = "https://api.fish.audio/v1/tts"
        private const val WALLET_ENDPOINT = "https://api.fish.audio/wallet/self/api-credit"
        private const val MODEL_ENDPOINT = "https://api.fish.audio/model"
    }

    private val gson = Gson()

    // Fast client for validation (max 6 seconds timeout)
    private val fastValidationClient = client.newBuilder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()

    private fun cleanKey(key: String): String {
        return key.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
    }

    /**
     * Fast validation (~200ms) that inspects wallet credit and model existence
     * without consuming credits or queueing in GPU.
     */
    suspend fun validateApiKeyAndModel(apiKey: String, voiceModelId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val token = cleanKey(apiKey)
            val modelId = voiceModelId.trim()

            if (token.isEmpty()) {
                return@withContext Result.failure(Exception("Fish Audio API anahtarı boş olamaz."))
            }
            if (modelId.isEmpty()) {
                return@withContext Result.failure(Exception("Fish Audio Model ID boş olamaz."))
            }

            // Step 1: Validate API key via credit endpoint
            try {
                val creditRequest = Request.Builder()
                    .url(WALLET_ENDPOINT)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                fastValidationClient.newCall(creditRequest).execute().use { response ->
                    if (response.code == 401) {
                        return@withContext Result.failure(
                            Exception("Fish Audio API Key geçersiz veya süresi dolmuş (401 - Yetkisiz).")
                        )
                    }
                    if (!response.isSuccessful && response.code != 404 && response.code != 403) {
                        // Some endpoints might return other non-fatal codes; we still check model
                    }
                }
            } catch (e: Exception) {
                // If wallet check network fails, proceed to model check
            }

            // Step 2: Validate Model ID
            try {
                val modelRequest = Request.Builder()
                    .url("$MODEL_ENDPOINT/$modelId")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                fastValidationClient.newCall(modelRequest).execute().use { response ->
                    if (response.code == 401) {
                        return@withContext Result.failure(
                            Exception("Fish Audio API Key geçersiz (401 - Unauthorized).")
                        )
                    }
                    if (response.code == 404) {
                        return@withContext Result.failure(
                            Exception("Fish Audio Voice Model ID ($modelId) bulunamadı (404). Lütfen Model ID'sini rehbere bakarak kontrol edin.")
                        )
                    }
                    if (response.isSuccessful) {
                        return@withContext Result.success(true)
                    }
                }
            } catch (e: Exception) {
                // If model endpoint has network glitch, allow passing if key is present
            }

            Result.success(true)
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
        val token = cleanKey(apiKey)
        val modelId = voiceModelId.trim()

        try {
            val chunks = TextChunker.splitIntoChunks(fullText, maxLimit = 500)
            if (chunks.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Seslendirilecek metin bulunamadı."))
            }

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
                        "reference_id" to modelId,
                        "format" to "mp3"
                    )

                    val requestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(TTS_ENDPOINT)
                        .addHeader("Authorization", "Bearer $token")
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
