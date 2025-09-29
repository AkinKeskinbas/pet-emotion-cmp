package com.keak.petemotions.data.api

import com.keak.petemotions.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.random.Random

class BackendApiService(
    private val baseUrl: String = "http://localhost:8080"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    private var authToken: String? = null

    suspend fun register(name: String? = null, species: String? = null): Result<RegisterResponse> {
        return try {
            println("Backend: Attempting registration to $baseUrl")
            val request = RegisterRequest(name = name, species = species)
            val response = client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }


            if (response.status.isSuccess()) {
                val registerResponse = response.body<RegisterResponse>()
                authToken = registerResponse.token
                println("Backend: Registration successful, token expires in ${registerResponse.expiresIn}s")
                Result.success(registerResponse)
            } else {
                val errorResponse = response.body<ErrorResponse>()
                Result.failure(Exception("Registration failed: ${errorResponse.error.message}"))
            }
        } catch (e: Exception) {
            println("Backend: Registration error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun analyzeImage(imageBytes: ByteArray): Result<BackendAnalysisResult> {
        return try {
            // Ensure we have a valid token
            if (authToken == null) {
                register().getOrThrow()
            }

            // Convert image to base64
            val base64Image = encodeBase64(imageBytes)
            val request = AnalyzeJsonRequest(imageBase64 = base64Image)

            val response = client.post("$baseUrl/v1/pet-emotions:analyze") {
                contentType(ContentType.Application.Json)
                bearerAuth(authToken!!)
                header("X-Request-Id", generateUuid())
                header("Idempotency-Key", generateUuid())
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val analysisResult = response.body<BackendAnalysisResult>()
                println("Backend: Analysis successful - emotion: ${analysisResult.emotion}, confidence: ${analysisResult.confidence}")
                Result.success(analysisResult)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Analysis failed with status ${response.status}: $errorText")

                try {
                    val errorResponse = Json.decodeFromString<ErrorResponse>(errorText)
                    Result.failure(Exception("Analysis failed: ${errorResponse.error.message}"))
                } catch (parseError: Exception) {
                    Result.failure(Exception("Analysis failed with status ${response.status}: $errorText"))
                }
            }
        } catch (e: Exception) {
            println("Backend: Analysis error: ${e.message}")
            Result.failure(e)
        }
    }

    fun isAuthenticated(): Boolean = authToken != null

    fun clearAuth() {
        authToken = null
    }

    private fun encodeBase64(bytes: ByteArray): String {
        // Simple base64 encoding for common case
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val result = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b3 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0

            val bitmap = (b1 shl 16) or (b2 shl 8) or b3

            result.append(base64Chars[(bitmap shr 18) and 0x3F])
            result.append(base64Chars[(bitmap shr 12) and 0x3F])
            result.append(if (i + 1 < bytes.size) base64Chars[(bitmap shr 6) and 0x3F] else '=')
            result.append(if (i + 2 < bytes.size) base64Chars[bitmap and 0x3F] else '=')

            i += 3
        }
        return result.toString()
    }

    private fun generateUuid(): String {
        // Simple UUID v4 implementation for common platforms
        val chars = "0123456789abcdef"
        val uuid = StringBuilder(36)

        for (i in 0..35) {
            when (i) {
                8, 13, 18, 23 -> uuid.append('-')
                14 -> uuid.append('4') // Version 4
                19 -> uuid.append(chars[8 + Random.nextInt(4)]) // Variant bits
                else -> uuid.append(chars[Random.nextInt(16)])
            }
        }

        return uuid.toString()
    }
}