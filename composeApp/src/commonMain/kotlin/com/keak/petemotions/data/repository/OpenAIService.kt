package com.keak.petemotions.data.repository

import com.keak.petemotions.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface OpenAIService {
    suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<AnalysisResult>
    suspend fun analyzeMedia(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult>
}

class OpenAIServiceImpl : OpenAIService {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        return try {
            val base64Image = Base64.encode(imageBytes)

            val request = OpenAIRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    Message(
                        role = "system",
                        content = listOf(
                            Content(
                                type = "text",
                                text = createSystemPrompt()
                            )
                        )
                    ),
                    Message(
                        role = "user",
                        content = listOf(
                            Content(
                                type = "image_url",
                                image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image")
                            ),
                            Content(
                                type = "text",
                                text = "Analyze this pet's emotions using the schema provided."
                            )
                        )
                    )
                ),
                temperature = 0.2,
                response_format = mapOf("type" to "json_object")
            )

            val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val openAIResponse = response.body<OpenAIResponse>()
            val content = openAIResponse.choices.firstOrNull()?.message?.content
                ?: throw Exception("No response content from OpenAI")

            val analysisResult = Json.decodeFromString<AnalysisResult>(content)
            Result.success(analysisResult)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun analyzeMedia(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        // For now, treat all media as images
        return analyzeImage(mediaBytes, apiKey)
    }

    private fun createSystemPrompt(): String {
        return """
            You are an assistant that analyzes pet emotions from a photo/video frame.
            Return STRICT JSON ONLY, matching this schema:
            {
              "emotion": "<one of: Happy, Relaxed, Curious, Alert, Stressed, Playful, Sad, Excited>",
              "confidence": 0.0-1.0,
              "summary": "<one short paragraph user-friendly>",
              "details": {
                "bodyLanguage": "<bullet-like text>",
                "vocalization": "<bullet-like text or 'N/A'>",
                "context": "<bullet-like text>"
              },
              "tags": ["morning|afternoon|evening", "indoors|outdoors", "cat|dog|other"]
            }

            If uncertain, choose the closest label and lower confidence.
            Use neutral tone, avoid medical claims.
        """.trimIndent()
    }
}

// Offline/Mock service for testing
class MockOpenAIService : OpenAIService {
    override suspend fun analyzeImage(imageBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        // Simulate network delay
        delay(2000)

        val mockResult = AnalysisResult(
            emotion = "Happy",
            confidence = 0.85,
            summary = "Your pet appears to be in a very positive and content state. The body language suggests comfort and happiness.",
            details = AnalysisDetails(
                bodyLanguage = "Relaxed posture, ears in neutral position, tail in comfortable position",
                vocalization = "N/A",
                context = "Indoor environment, good lighting, pet appears comfortable"
            ),
            tags = listOf("afternoon", "indoors", "cat")
        )

        return Result.success(mockResult)
    }

    override suspend fun analyzeMedia(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        return analyzeImage(mediaBytes, apiKey)
    }
}