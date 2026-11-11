package com.kopyamatik.app.data

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val SYSTEM_PROMPT = """Sana attığım kağıt fotoğraflarındaki tüm soruları müfredata uygun, kesinlikle eksiksiz ve doğru şekilde çöz. Çıktıyı doğrudan seslendirilecek düz bir metin olarak ver, markdown (*, # vb.) veya bozuk karakter KULLANMA. Her soruda şu formatı birebir uygula:
1. '(Soru Numarası). soruya geçiyorum.' yaz.
2. Sorunun soru kalıbını 1 kez tam olarak yaz.
3. Soruyu nasıl çözeceğimizi ve ne yapacağımızı 1 kez kısaca anlat.
4. 'Cevapları söylüyorum.' yaz.
5. Eğer sorunun cevabı uzun bir metinse, cevabı 6 kelimelik gruplara böl. Her 6 kelimelik grubu peş peşe 6 kez tekrarla (Örnek: 'kelime1 kelime2 kelime3 kelime4 kelime5 kelime6. ' x6) ve sonraki 6 kelimeye geç.
6. Eğer cevap kısa veya tek kullanımlıksa (şık, tek kelime, kısa işlem sonucu vb.), cevabı peş peşe 6 kez tekrarla.
7. Ardından bir sonraki soruya geç."""

        /**
         * Ordered fallback models to try when a model is deprecated or returns 404.
         */
        val FALLBACK_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.0-flash",
            "gemini-flash-latest",
            "gemini-2.0-flash"
        )

        private fun buildEndpoint(model: String, apiKey: String): String {
            return "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${apiKey.trim()}"
        }
    }

    private val gson = Gson()

    // Cache the last working model name across the app session
    @Volatile
    private var lastWorkingModel: String? = null

    private fun getOrderedModelsToTry(): List<String> {
        val working = lastWorkingModel
        return if (working != null) {
            listOf(working) + (FALLBACK_MODELS - working)
        } else {
            FALLBACK_MODELS
        }
    }

    /**
     * Test the validity of the Gemini API key using fallback models.
     */
    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val testPayload = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(
                            mapOf("text" to "ping")
                        )
                    )
                )
            )
            val requestBodyString = gson.toJson(testPayload)
            val modelsToTry = getOrderedModelsToTry()
            var lastError: String? = null

            for (model in modelsToTry) {
                val url = buildEndpoint(model, apiKey)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBodyString.toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            lastWorkingModel = model
                            return@withContext Result.success(true)
                        }

                        val errorBody = response.body?.string().orEmpty()
                        lastError = "Model $model (${response.code}): $errorBody"

                        // If 404 (model not found / deprecated for this API key), try next model
                        if (response.code == 404) {
                            return@use
                        } else {
                            // Non-404 errors (e.g. 400 Invalid Key or 403 Forbidden) fail immediately
                            return@withContext Result.failure(
                                Exception("Gemini API Hatası (${response.code}): $errorBody")
                            )
                        }
                    }
                } catch (e: Exception) {
                    lastError = "Model $model bağlantı hatası: ${e.localizedMessage}"
                }
            }

            Result.failure(Exception("Hiçbir Gemini modeli çalıştırılamadı. Son hata: $lastError"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Solves questions present in the given bitmaps, using fallback models if 404 occurs.
     */
    suspend fun solveQuestions(
        apiKey: String,
        images: List<Bitmap>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (images.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Hiç görsel seçilmedi."))
            }

            val partsArray = mutableListOf<Map<String, Any>>()

            // Add images as inline base64 data
            for (bitmap in images) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                val imagePart = mapOf(
                    "inline_data" to mapOf(
                        "mime_type" to "image/jpeg",
                        "data" to base64Data
                    )
                )
                partsArray.add(imagePart)
            }

            // User prompt instruction
            partsArray.add(
                mapOf("text" to "Lütfen yüklenen fotoğraflardaki tüm soruları sistem talimatına harfiyen uyarak çöz.")
            )

            val payload = mapOf(
                "system_instruction" to mapOf(
                    "parts" to listOf(
                        mapOf("text" to SYSTEM_PROMPT)
                    )
                ),
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to partsArray
                    )
                ),
                "generationConfig" to mapOf(
                    "temperature" to 0.2
                )
            )

            val requestBodyString = gson.toJson(payload)
            val modelsToTry = getOrderedModelsToTry()
            var lastError: String? = null

            for (model in modelsToTry) {
                val url = buildEndpoint(model, apiKey)
                val request = Request.Builder()
                    .url(url)
                    .post(requestBodyString.toRequestBody("application/json".toMediaType()))
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        val bodyString = response.body?.string().orEmpty()

                        if (!response.isSuccessful) {
                            lastError = "Model $model (${response.code}): $bodyString"
                            if (response.code == 404) {
                                // Fallback to next model
                                return@use
                            } else {
                                return@withContext Result.failure(
                                    Exception("Gemini Soru Çözüm Hatası (${response.code}): $bodyString")
                                )
                            }
                        }

                        val jsonResponse = gson.fromJson(bodyString, JsonObject::class.java)
                        val candidates = jsonResponse.getAsJsonArray("candidates")
                        if (candidates != null && candidates.size() > 0) {
                            val candidate = candidates.get(0).asJsonObject
                            val content = candidate.getAsJsonObject("content")
                            val parts = content.getAsJsonArray("parts")
                            if (parts != null && parts.size() > 0) {
                                val rawText = parts.get(0).asJsonObject.get("text").asString
                                val sanitizedText = cleanMarkdownAndArtifacts(rawText)
                                lastWorkingModel = model
                                return@withContext Result.success(sanitizedText)
                            }
                        }

                        lastError = "Geçerli metin yanıtı alınamadı ($model)."
                    }
                } catch (e: Exception) {
                    lastError = "Model $model bağlantı hatası: ${e.localizedMessage}"
                }
            }

            Result.failure(Exception("Hiçbir Gemini modeli yanıt vermedi ($modelsToTry). Son hata: $lastError"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Strips markdown symbols (*, #, `, _, etc.) to ensure clean, natural speech synthesis.
     */
    private fun cleanMarkdownAndArtifacts(text: String): String {
        return text
            .replace(Regex("[*#`_~>]"), "") // Remove markdown format symbols
            .replace(Regex("\\s+"), " ")      // Collapse multiple whitespace
            .replace("\n\n\n+", "\n\n")       // Normalize multiple newlines
            .trim()
    }
}
