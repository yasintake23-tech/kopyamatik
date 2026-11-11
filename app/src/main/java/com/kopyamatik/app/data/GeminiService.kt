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

        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    private val gson = Gson()

    /**
     * Test the validity of the Gemini API key using gemini-2.5-flash.
     */
    suspend fun validateApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$GEMINI_ENDPOINT?key=${apiKey.trim()}"
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
            val requestBody = gson.toJson(testPayload).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorBody = response.body?.string().orEmpty()
                    Result.failure(Exception("Gemini API Hatası (${response.code}): $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Solves questions present in the given bitmaps.
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

            val requestBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$GEMINI_ENDPOINT?key=${apiKey.trim()}")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Gemini Soru Çözüm Hatası (${response.code}): $bodyString")
                    )
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
                        return@withContext Result.success(sanitizedText)
                    }
                }

                Result.failure(Exception("Gemini'den geçerli bir metin yanıtı alınamadı."))
            }
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
