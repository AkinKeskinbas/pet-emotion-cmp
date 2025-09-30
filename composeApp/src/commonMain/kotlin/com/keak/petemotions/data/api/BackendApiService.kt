package com.keak.petemotions.data.api

import com.keak.petemotions.data.model.*
import com.keak.petemotions.platform.PlatformConfig
import com.revenuecat.purchases.kmp.Purchases
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

expect fun getStoredRevenueCatUserId(): String?

// Extension function to extract UUID from RevenueCat user ID format
fun String.extractUuidFromRevenueCatId(): String {
    return if (this.startsWith("\$RCAnonymousID:")) {
        this.substringAfter("\$RCAnonymousID:")
    } else {
        this
    }
}

class BackendApiService(
    private val baseUrl: String = PlatformConfig.backendBaseUrl
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true // Handle null/missing values gracefully
        encodeDefaults = true
    }

    private val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
            install(Logging) {
                level = LogLevel.INFO
            }

            // Set timeouts for better error handling
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000  // 10 seconds
                requestTimeoutMillis = 30_000  // 30 seconds
                socketTimeoutMillis = 30_000   // 30 seconds
            }
        }
    }

    private var authToken: String? = null

    suspend fun register(name: String? = null, species: String? = null): Result<RegisterResponse> {
        return try {
            println("Backend: Attempting registration to $baseUrl")

            // Get RevenueCat user ID for backend sync - always required
            val revenueCatUserId = try {
                resolveRevenueCatUserId().also {
                    println("Backend: Using RevenueCat user ID for registration: $it")
                }
            } catch (e: Exception) {
                println("Backend: Failed to resolve RevenueCat user ID: ${e.message}")
                // Generate emergency fallback ID
                generateRevenueCatUserId()
            }

            val request = RegisterRequest(
                name = name,
                species = species,
                userId = revenueCatUserId
            )

            println("Backend: Sending registration request:")
            println("  - name: $name")
            println("  - species: $species")
            println("  - userId: $revenueCatUserId")
            println("  - userId is blank: ${revenueCatUserId.isBlank()}")
            println("  - URL: $baseUrl/auth/register")

            // Serialize and print the actual JSON being sent
            val requestJson = json.encodeToString(request)
            println("Backend: Request JSON: $requestJson")

            val response = client.post("$baseUrl/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            println("Backend: Registration response status: ${response.status}")
            println("Backend: Registration response headers: ${response.headers}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Registration success response: $responseText")

                val registerResponse = json.decodeFromString<RegisterResponse>(responseText)
                authToken = registerResponse.token
                val expirationInfo = registerResponse.expiresIn?.let { "expires in ${it}s" } ?: "no expiration info"
                println("Backend: Registration successful, $expirationInfo")
                println("Backend: Auth token set: ${authToken?.take(20)}...")
                Result.success(registerResponse)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Registration failed with status ${response.status.value}")
                println("Backend: Raw error response: $errorText")

                try {
                    val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
                    println("Backend: Parsed error: ${errorResponse.error.message}")
                    Result.failure(Exception("Registration failed: ${errorResponse.error.message}"))
                } catch (parseError: Exception) {
                    println("Backend: Failed to parse error response: ${parseError.message}")
                    val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                    Result.failure(Exception("Registration failed: $userFriendlyError"))
                }
            }
        } catch (e: Exception) {
            println("Backend: Registration error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun analyzeImage(imageBytes: ByteArray): Result<BackendAnalysisResult> {
        return try {
            // Check if we have a valid token (should be set during app startup registration)
            if (authToken == null) {
                return Result.failure(Exception("Authentication required. Registration should happen during app startup."))
            }

            // Convert image to base64
            val base64Image = encodeBase64(imageBytes)
            val request = AnalyzeJsonRequest(imageBase64 = base64Image)

            println("Backend: Sending analysis request:")
            println("  - Image size: ${imageBytes.size} bytes")
            println("  - Base64 length: ${base64Image.length}")
            println("  - URL: $baseUrl/v1/pet-emotions:analyze")

            // Serialize and print the actual JSON being sent
            val requestJson = json.encodeToString(request)
            println("Backend: Analysis request JSON: $requestJson")

            val response = client.post("$baseUrl/v1/pet-emotions:analyze") {
                contentType(ContentType.Application.Json)
                bearerAuth(authToken!!)
                header("X-Request-Id", generateUuid())
                header("Idempotency-Key", generateUuid())
                setBody(request)
            }

            println("Backend: Analysis response status: ${response.status}")
            println("Backend: Analysis response headers: ${response.headers}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Analysis success response: $responseText")

                val wrapper = json.decodeFromString<AnalysisResponseWrapper>(responseText)
                val analysisResult = wrapper.result
                println("Backend: Analysis successful - emotion: ${analysisResult.emotion}, confidence: ${analysisResult.confidence}")
                Result.success(analysisResult)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Analysis failed with status ${response.status}: $errorText")

                // Handle insufficient coins specifically
                if (response.status.value == 402) {
                    try {
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
                        val hintMessage = errorResponse.error.details.hint ?: "Insufficient coins for this operation."
                        println("Backend: Insufficient coins hint: $hintMessage")
                        Result.failure(InsufficientCoinsException(0, 0, hintMessage))
                    } catch (e: Exception) {
                        println("Backend: Failed to parse 402 error: ${e.message}")
                        Result.failure(Exception("Insufficient coins for this operation."))
                    }
                } else {
                    val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                    Result.failure(Exception(userFriendlyError))
                }
            }
        } catch (e: Exception) {
            println("Backend: Analysis error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun comparePetEmotions(firstPetAnalyses: List<AnalysisResult>, secondPetAnalyses: List<AnalysisResult>): Result<ComparisonResult> {
        return try {
            // Check if we have a valid token (should be set during app startup registration)
            if (authToken == null) {
                return Result.failure(Exception("Authentication required. Registration should happen during app startup."))
            }

            val request = CompareRequest(
                first = firstPetAnalyses,
                second = secondPetAnalyses
            )

            println("Backend: Sending comparison request:")
            println("  - First pet analyses: ${firstPetAnalyses.size} records")
            println("  - Second pet analyses: ${secondPetAnalyses.size} records")
            println("  - URL: $baseUrl/v1/pet-emotions:compare")

            // Serialize and print the actual JSON being sent
            val requestJson = json.encodeToString(request)
            println("Backend: Comparison request JSON: $requestJson")

            val response = client.post("$baseUrl/v1/pet-emotions:compare") {
                contentType(ContentType.Application.Json)
                bearerAuth(authToken!!)
                header("X-Request-Id", generateUuid())
                header("Idempotency-Key", generateUuid())
                setBody(request)
            }

            println("Backend: Comparison response status: ${response.status}")
            println("Backend: Comparison response headers: ${response.headers}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Comparison success response: $responseText")

                val comparisonResult = json.decodeFromString<ComparisonResult>(responseText)
                println("Backend: Comparison successful - compatibility score: ${comparisonResult.compatibilityScore}")
                Result.success(comparisonResult)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Comparison failed with status ${response.status}: $errorText")

                // Handle insufficient coins specifically
                if (response.status.value == 402) {
                    try {
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
                        val hintMessage = errorResponse.error.details.hint ?: "Insufficient coins for this operation."
                        println("Backend: Insufficient coins hint: $hintMessage")
                        Result.failure(InsufficientCoinsException(0, 0, hintMessage))
                    } catch (e: Exception) {
                        println("Backend: Failed to parse 402 error: ${e.message}")
                        Result.failure(Exception("Insufficient coins for this operation."))
                    }
                } else {
                    val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                    Result.failure(Exception(userFriendlyError))
                }
            }
        } catch (e: Exception) {
            println("Backend: Comparison error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun validatePurchase(
        platform: String,
        receipt: String,
        transactionId: String,
        productId: String
    ): Result<PurchaseValidationResponse> {
        return try {
            // Ensure we have a valid token
            if (authToken == null) {
                register().getOrThrow()
            }

            val request = PurchaseValidationRequest(
                platform = platform,
                receipt = receipt,
                transactionId = transactionId,
                productId = productId
            )

            println("Backend: Sending purchase validation request:")
            println("  - Platform: $platform")
            println("  - Product ID: $productId")
            println("  - Transaction ID: $transactionId")
            println("  - Receipt length: ${receipt.length} chars")
            println("  - URL: $baseUrl/v1/purchases:validate")

            // Serialize and print the actual JSON being sent (excluding receipt for brevity)
            val requestForLogging = request.copy(receipt = receipt.take(50) + "...")
            val requestJson = json.encodeToString(requestForLogging)
            println("Backend: Purchase validation request JSON: $requestJson")

            val response = client.post("$baseUrl/v1/purchases:validate") {
                contentType(ContentType.Application.Json)
                bearerAuth(authToken!!)
                header("X-Request-Id", generateUuid())
                header("Idempotency-Key", transactionId) // Use transaction ID for idempotency
                setBody(request)
            }

            println("Backend: Purchase validation response status: ${response.status}")
            println("Backend: Purchase validation response headers: ${response.headers}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Purchase validation success response: $responseText")

                val validationResponse = json.decodeFromString<PurchaseValidationResponse>(responseText)
                println("Backend: Purchase validation successful - ${validationResponse.coinsAdded} coins added")
                Result.success(validationResponse)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Purchase validation failed with status ${response.status}: $errorText")

                // Try to parse structured error response
                try {
                    val validationResponse = json.decodeFromString<PurchaseValidationResponse>(errorText)
                    Result.failure(Exception(validationResponse.error ?: "Purchase validation failed"))
                } catch (e: Exception) {
                    val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                    Result.failure(Exception(userFriendlyError))
                }
            }
        } catch (e: Exception) {
            println("Backend: Purchase validation error: ${e.message}")
            Result.failure(e)
        }
    }

    fun isAuthenticated(): Boolean = authToken != null

    suspend fun getCoinBalance(): Result<CoinBalanceResponse> {
        return try {
            // Check if we have a valid token
            println("Backend: getCoinBalance() called - authToken is ${if (authToken != null) "SET" else "NULL"}")
            if (authToken == null) {
                println("Backend: No auth token available for coin balance request")
                return Result.failure(Exception("Authentication required. Registration should happen during app startup."))
            }

            println("Backend: Sending coin balance request:")
            println("  - URL: $baseUrl/v1/coins:balance")

            val response = client.get("$baseUrl/v1/coins:balance") {
                bearerAuth(authToken!!)
                header("X-Request-Id", generateUuid())
            }

            println("Backend: Coin balance response status: ${response.status}")
            println("Backend: Coin balance response headers: ${response.headers}")

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Coin balance success response: $responseText")

                val balanceResponse = json.decodeFromString<CoinBalanceResponse>(responseText)
                println("Backend: Coin balance successful - balance: ${balanceResponse.balance}")
                Result.success(balanceResponse)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Coin balance failed with status ${response.status}: $errorText")
                val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                Result.failure(Exception(userFriendlyError))
            }
        } catch (e: Exception) {
            println("Backend: Coin balance error: ${e.message}")
            Result.failure(e)
        }
    }

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

    private fun resolveRevenueCatUserId(): String {
        println("Backend: Resolving RevenueCat user ID...")
        println("Backend: RevenueCat isConfigured: ${Purchases.isConfigured}")

        if (Purchases.isConfigured) {
            try {
                val appUserId = Purchases.sharedInstance.appUserID
                println("Backend: RevenueCat appUserID from instance: '$appUserId'")
                if (appUserId.isNotBlank()) {
                    val extractedId = appUserId.extractUuidFromRevenueCatId()
                    println("Backend: Extracted UUID from RevenueCat ID: '$extractedId'")
                    return extractedId
                } else {
                    println("Backend: RevenueCat app user ID is blank or empty")
                }
            } catch (e: Exception) {
                println("Backend: Unable to read RevenueCat app user ID: ${e.message}")
            }
        } else {
            println("Backend: RevenueCat is not configured yet")
        }

        // Try to get the stored user ID from platform-specific storage
        try {
            val storedUserId = getStoredRevenueCatUserId()
            if (storedUserId != null && storedUserId.isNotBlank()) {
                val extractedId = storedUserId.extractUuidFromRevenueCatId()
                println("Backend: Extracted UUID from stored RevenueCat ID: '$extractedId'")
                return extractedId
            }
        } catch (e: Exception) {
            println("Backend: Failed to get stored user ID: ${e.message}")
        }

        // Always generate a fallback ID to ensure we never return null
        return generateRevenueCatUserId().extractUuidFromRevenueCatId().also {
            println("Backend: Generated fallback UUID: $it")
        }
    }

    private fun generateRevenueCatUserId(): String {
        // Generate a user ID that matches what RevenueCat expects
        // Format: "$RCAnonymousID:" + UUID-like string
        val anonymousId = "\$RCAnonymousID:" + generateUuid()
        return anonymousId
    }

    private fun parseErrorMessage(statusCode: Int, errorText: String): String {
        return try {
            println("Backend: Parsing error response: $errorText")
            val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
            val errorCode = errorResponse.error.code
            val details = errorResponse.error.details
            println("Backend: Parsed error code: $errorCode, httpStatus: ${details.httpStatus}")

            when (errorCode) {
                "OPENAI_ERROR" -> {
                    when (statusCode) {
                        502 -> "The AI service is temporarily unavailable. Please try again in a few moments."
                        429 -> "Too many requests. Please wait a moment before trying again."
                        500 -> "There's a temporary issue with the analysis service. Please try again."
                        else -> "The analysis service encountered an issue. Please try again."
                    }
                }
                "VALIDATION_ERROR" -> "Invalid image format. Please try with a different photo."
                "AUTHENTICATION_ERROR" -> "Authentication failed. Please restart the app."
                "RATE_LIMIT_ERROR" -> "You've reached the request limit. Please wait before trying again."
                else -> {
                    // For unknown error codes, provide a generic but helpful message
                    when (statusCode) {
                        400 -> "Invalid request. Please try with a different photo."
                        401 -> "Authentication failed. Please restart the app."
                        403 -> "Access denied. Please check your account status."
                        404 -> "Service not found. Please check your connection."
                        500, 502, 503, 504 -> "The analysis service is temporarily unavailable. Please try again."
                        else -> "Analysis failed. Please check your connection and try again."
                    }
                }
            }
        } catch (parseError: Exception) {
            // If we can't parse the error response, provide a generic message based on status code
            println("Backend: Failed to parse error response: ${parseError.message}")
            println("Backend: Raw error text: $errorText")
            when (statusCode) {
                400 -> "Invalid request. Please try with a different photo."
                401 -> "Authentication failed. Please restart the app."
                403 -> "Access denied. Please check your account status."
                404 -> "Service not found. Please check your connection."
                429 -> "Too many requests. Please wait before trying again."
                500, 502, 503, 504 -> "The analysis service is temporarily unavailable. Please try again."
                else -> "Analysis failed. Please check your connection and try again."
            }
        }
    }
}

@Serializable
data class CompareRequest(
    @SerialName("first") val first: List<AnalysisResult> = emptyList(),
    @SerialName("second") val second: List<AnalysisResult> = emptyList()
)

@Serializable
data class ComparisonResult(
    val compatibilityScore: Double,
    val overview: String,
    val sharedTraits: List<String>,
    val keyDifferences: List<String>,
    val recommendations: List<String>,
    val coinInfo: BackendCoinInfo? = null // Backend'den gelen coin bilgisi
)

@Serializable
data class InsufficientCoinsError(
    val error: String,
    val requiredCoins: Int,
    val currentCoins: Int,
    val operationType: String
)

class InsufficientCoinsException(
    val requiredCoins: Int,
    val currentCoins: Int,
    message: String
) : Exception(message)
