package com.keak.petemotions.data.api

import com.keak.petemotions.data.model.*
import com.keak.petemotions.platform.PlatformConfig
import com.keak.petemotions.platform.getCurrentLanguage
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

expect fun getStoredRevenueCatUserId(): String?
expect fun getStoredBackendToken(): String?
expect fun setStoredBackendToken(token: String?)

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
                connectTimeoutMillis = 15_000   // 15 seconds
                requestTimeoutMillis = 120_000  // 2 minutes for large video files
                socketTimeoutMillis = 120_000   // 2 minutes for large video files
            }
        }
    }

    private var authToken: String? = getStoredBackendToken()

    init {
        println("BackendApiService: Initialized with cached token: ${authToken?.take(20)}...")
    }

    suspend fun register(
        name: String? = null,
        species: String? = null,
        userId: String? = null
    ): Result<RegisterResponse> {
        return try {
            println("Backend: Attempting registration to $baseUrl")

            // Use provided userId, or fall back to RevenueCat user ID resolution
            val registrationUserId = userId ?: try {
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
                userId = registrationUserId
            )

            println("Backend: Sending registration request:")
            println("  - name: $name")
            println("  - species: $species")
            println("  - userId: $registrationUserId")
            println("  - userId is blank: ${registrationUserId.isBlank()}")
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
                setStoredBackendToken(authToken) // Cache token
                val expirationInfo = registerResponse.expiresIn?.let { "expires in ${it}s" } ?: "no expiration info"
                println("Backend: Registration successful, $expirationInfo")
                println("Backend: Auth token set and cached: ${authToken?.take(20)}...")
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
        val currentLanguage = getCurrentLanguage()
        var attempt = 0

        while (attempt < 2) {
            attempt++

            val token = try {
                requireAuthTokenOrThrow()
            } catch (authError: Exception) {
                println("Backend: Unable to acquire auth token: ${authError.message}")
                return Result.failure(authError)
            }

            println("Backend: Sending analysis request with multipart:")
            println("  - Image size: ${imageBytes.size} bytes")
            println("  - URL: $baseUrl/v1/pet-emotions:analyze")
            println("  - Language: $currentLanguage")
            println("  - Attempt: $attempt")

            val response = try {
                client.post("$baseUrl/v1/pet-emotions:analyze") {
                    bearerAuth(token)
                    header("X-Request-Id", generateUuid())
                    header("Idempotency-Key", generateUuid())
                    header(HttpHeaders.AcceptLanguage, currentLanguage)
                    setBody(
                        io.ktor.client.request.forms.MultiPartFormDataContent(
                            io.ktor.client.request.forms.formData {
                                append("language", currentLanguage)
                                append("image", imageBytes, io.ktor.http.Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"image.jpg\"")
                                })
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                println("Backend: Analysis error: ${e.message}")
                return Result.failure(e)
            }

            println("Backend: Analysis response status: ${response.status}")
            println("Backend: Analysis response headers: ${response.headers}")

            if (response.status == HttpStatusCode.Unauthorized) {
                println("Backend: Analysis unauthorized. Clearing cached auth and retrying...")
                clearAuth()
                continue
            }

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Analysis success response: $responseText")

                val wrapper = json.decodeFromString<AnalysisResponseWrapper>(responseText)
                val analysisResult = wrapper.result
                println("Backend: Analysis successful - emotion: ${analysisResult.emotion}, confidence: ${analysisResult.confidence}")
                return Result.success(analysisResult)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Analysis failed with status ${response.status}: $errorText")

                if (response.status.value == 402) {
                    return try {
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
                        val hintMessage = errorResponse.error.details.hint ?: "Insufficient coins for this operation."
                        println("Backend: Insufficient coins hint: $hintMessage")
                        Result.failure(InsufficientCoinsException(0, 0, hintMessage))
                    } catch (e: Exception) {
                        println("Backend: Failed to parse 402 error: ${e.message}")
                        Result.failure(Exception("Insufficient coins for this operation."))
                    }
                }

                val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                return Result.failure(Exception(userFriendlyError))
            }
        }

        return Result.failure(Exception("Authentication failed. Please try again."))
    }

    suspend fun comparePetEmotions(firstPetAnalyses: List<AnalysisResult>, secondPetAnalyses: List<AnalysisResult>): Result<ComparisonResultWithCoinInfo> {
        var attempt = 0

        while (attempt < 2) {
            attempt++

            val token = try {
                requireAuthTokenOrThrow()
            } catch (authError: Exception) {
                println("Backend: Unable to acquire auth token for comparison: ${authError.message}")
                return Result.failure(authError)
            }

            val request = buildCompareRequest(firstPetAnalyses, secondPetAnalyses)

            println("Backend: Sending comparison request:")
            println("  - First pet analyses: ${firstPetAnalyses.size} records")
            println("  - Second pet analyses: ${secondPetAnalyses.size} records")
            println("  - URL: $baseUrl/v1/pet-emotions:compare")
            println("  - Attempt: $attempt")

            val requestJson = json.encodeToString(request)
            println("Backend: Comparison request JSON: $requestJson")

            val response = try {
                client.post("$baseUrl/v1/pet-emotions:compare") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    header("X-Request-Id", generateUuid())
                    header("Idempotency-Key", generateUuid())
                    setBody(request)
                }
            } catch (e: Exception) {
                println("Backend: Comparison request error: ${e.message}")
                return Result.failure(e)
            }

            println("Backend: Comparison response status: ${response.status}")
            println("Backend: Comparison response headers: ${response.headers}")

            if (response.status == HttpStatusCode.Unauthorized) {
                println("Backend: Comparison unauthorized. Clearing cached auth and retrying...")
                clearAuth()
                continue
            }

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Comparison success response: $responseText")

                val wrapper = json.decodeFromString<ComparisonResponseWrapper>(responseText)
                val enrichedResult = wrapper.result.enrichWith(request)

                // Create final result with coinInfo from wrapper
                val finalResult = ComparisonResultWithCoinInfo(
                    compatibilityScore = enrichedResult.compatibilityScore,
                    overview = enrichedResult.overview,
                    sharedTraits = enrichedResult.sharedTraits,
                    keyDifferences = enrichedResult.keyDifferences,
                    recommendations = enrichedResult.recommendations,
                    coinInfo = wrapper.coinInfo
                )

                println("Backend: Comparison successful - compatibility score: ${finalResult.compatibilityScore}")
                return Result.success(finalResult)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Comparison failed with status ${response.status}: $errorText")

                if (response.status.value == 402) {
                    return try {
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorText)
                        val hintMessage = errorResponse.error.details.hint ?: "Insufficient coins for this operation."
                        println("Backend: Insufficient coins hint: $hintMessage")
                        Result.failure(InsufficientCoinsException(0, 0, hintMessage))
                    } catch (e: Exception) {
                        println("Backend: Failed to parse 402 error: ${e.message}")
                        Result.failure(Exception("Insufficient coins for this operation."))
                    }
                }

                val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                return Result.failure(Exception(userFriendlyError))
            }
        }

        return Result.failure(Exception("Authentication failed. Please try again."))
    }

    suspend fun validatePurchase(
        platform: String,
        receipt: String,
        transactionId: String,
        productId: String
    ): Result<PurchaseValidationResponse> {
        var attempt = 0

        while (attempt < 2) {
            attempt++

            val token = try {
                requireAuthTokenOrThrow()
            } catch (authError: Exception) {
                println("Backend: Unable to acquire auth token for purchase validation: ${authError.message}")
                return Result.failure(authError)
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
            println("  - Receipt preview: ${receipt.take(20)}...")
            println("  - URL: $baseUrl/v1/purchases:validate")
            println("  - Attempt: $attempt")

            val requestForLogging = request.copy(receipt = receipt.take(50) + "...")
            val requestJson = json.encodeToString(requestForLogging)
            println("Backend: Purchase validation request JSON: $requestJson")

            val response = try {
                client.post("$baseUrl/v1/purchases:validate") {
                    contentType(ContentType.Application.Json)
                    bearerAuth(token)
                    header("X-Request-Id", generateUuid())
                    header("Idempotency-Key", transactionId)
                    setBody(request)
                }
            } catch (e: Exception) {
                println("Backend: Purchase validation request error: ${e.message}")
                return Result.failure(e)
            }

            println("Backend: Purchase validation response status: ${response.status}")
            println("Backend: Purchase validation response headers: ${response.headers}")

            if (response.status == HttpStatusCode.Unauthorized) {
                println("Backend: Purchase validation unauthorized. Clearing cached auth and retrying...")
                clearAuth()
                continue
            }

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Purchase validation success response: $responseText")

                val validationResponse = json.decodeFromString<PurchaseValidationResponse>(responseText)
                println("Backend: Purchase validation successful - ${validationResponse.coinsAdded} coins added")
                return Result.success(validationResponse)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Purchase validation failed with status ${response.status}: $errorText")

                if (errorText.contains("Google Play Error: Invalid Value")) {
                    println("Backend: DEBUGGING - RevenueCat Google Play Integration Issue:")
                    println("  - This usually indicates a problem with:")
                    println("    1. RevenueCat product configuration")
                    println("    2. Google Play Console setup")
                    println("    3. Purchase token format mismatch")
                    println("    4. Sandbox/Production environment mismatch")
                    println("  - Receipt being sent: ${receipt.take(30)}...")
                    println("  - Product ID being validated: $productId")
                    println("  - Consider checking RevenueCat dashboard for product mapping")
                }

                return try {
                    val validationResponse = json.decodeFromString<PurchaseValidationResponse>(errorText)
                    Result.failure(Exception(validationResponse.error ?: "Purchase validation failed"))
                } catch (e: Exception) {
                    val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                    Result.failure(Exception(userFriendlyError))
                }
            }
        }

        return Result.failure(Exception("Authentication failed. Please try again."))
    }

    fun isAuthenticated(): Boolean = authToken != null

    suspend fun getCoinBalance(): Result<CoinBalanceResponse> {
        var attempt = 0

        while (attempt < 2) {
            attempt++

            val token = try {
                requireAuthTokenOrThrow()
            } catch (authError: Exception) {
                println("Backend: Unable to acquire auth token for coin balance: ${authError.message}")
                return Result.failure(authError)
            }

            println("Backend: getCoinBalance() called - auth token is SET")
            println("Backend: Sending coin balance request (attempt $attempt):")
            println("  - URL: $baseUrl/v1/coins:balance")

            val response = try {
                client.get("$baseUrl/v1/coins:balance") {
                    bearerAuth(token)
                    header("X-Request-Id", generateUuid())
                }
            } catch (e: Exception) {
                println("Backend: Coin balance request error: ${e.message}")
                return Result.failure(e)
            }

            println("Backend: Coin balance response status: ${response.status}")
            println("Backend: Coin balance response headers: ${response.headers}")

            if (response.status == HttpStatusCode.Unauthorized) {
                println("Backend: Coin balance unauthorized. Clearing cached auth and retrying...")
                clearAuth()
                continue
            }

            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                println("Backend: Coin balance success response: $responseText")

                val balanceResponse = json.decodeFromString<CoinBalanceResponse>(responseText)
                println("Backend: Coin balance successful - balance: ${balanceResponse.balance}")
                return Result.success(balanceResponse)
            } else {
                val errorText = response.bodyAsText()
                println("Backend: Coin balance failed with status ${response.status}: $errorText")
                val userFriendlyError = parseErrorMessage(response.status.value, errorText)
                return Result.failure(Exception(userFriendlyError))
            }
        }

        return Result.failure(Exception("Authentication failed. Please try again."))
    }

    fun clearAuth() {
        println("Backend: Clearing cached authentication state")
        authToken = null
        setStoredBackendToken(null)
    }

    private fun buildCompareRequest(
        first: List<AnalysisResult>,
        second: List<AnalysisResult>
    ): CompareRequest {
        val firstStats = aggregateEmotionStats(first)
        val secondStats = aggregateEmotionStats(second)

        val compatibilityScore = computeCompatibilityScore(firstStats, secondStats)
        val overview = buildOverviewText(firstStats, secondStats)
        val sharedTraits = buildSharedTraits(firstStats, secondStats)
        val keyDifferences = buildKeyDifferences(firstStats, secondStats)
        val recommendations = buildRecommendations(firstStats, secondStats, sharedTraits, keyDifferences)

        return CompareRequest(
            first = first,
            second = second,
            compatibilityScore = compatibilityScore,
            overview = overview,
            sharedTraits = sharedTraits.ifEmpty {
                listOf("Both pets have unique emotional signatures — keep collecting insights to uncover overlaps.")
            },
            keyDifferences = keyDifferences.ifEmpty {
                listOf("Emotion intensities are balanced; no major differences detected.")
            },
            recommendations = recommendations.ifEmpty {
                listOf("Log a few more analyses for each pet to unlock tailored recommendations.")
            }
        )
    }

    private fun aggregateEmotionStats(analyses: List<AnalysisResult>): Map<String, Double> {
        if (analyses.isEmpty()) return emptyMap()

        val counts = analyses.groupingBy { it.emotion.lowercase() }.eachCount()
        val total = counts.values.sum().toDouble()
        if (total == 0.0) return emptyMap()

        return counts.mapValues { (_, count) -> count / total }
    }

    private fun computeCompatibilityScore(
        firstStats: Map<String, Double>,
        secondStats: Map<String, Double>
    ): Double {
        if (firstStats.isEmpty() || secondStats.isEmpty()) return 0.0
        val union = (firstStats.keys + secondStats.keys).toSet()
        if (union.isEmpty()) return 0.0

        val averageDiff = union
            .map { emotion ->
                val a = firstStats[emotion] ?: 0.0
                val b = secondStats[emotion] ?: 0.0
                abs(a - b)
            }
            .average()

        return (1.0 - averageDiff).coerceIn(0.0, 1.0)
    }

    private fun buildOverviewText(
        firstStats: Map<String, Double>,
        secondStats: Map<String, Double>
    ): String {
        if (firstStats.isEmpty() && secondStats.isEmpty()) {
            return "Add more analyses for each pet to unlock meaningful insights."
        }

        val sharedEmotions = firstStats.keys.intersect(secondStats.keys)
        return if (sharedEmotions.isNotEmpty()) {
            val strongestShared = sharedEmotions.maxByOrNull { emotion ->
                min(firstStats[emotion] ?: 0.0, secondStats[emotion] ?: 0.0)
            }

            val formatted = strongestShared?.let { formatEmotion(it) } ?: "similar moods"
            "Both pets regularly express $formatted, suggesting compatible emotional rhythms."
        } else {
            val topA = firstStats.maxByOrNull { it.value }?.key?.let { formatEmotion(it) } ?: "unique"
            val topB = secondStats.maxByOrNull { it.value }?.key?.let { formatEmotion(it) } ?: "unique"
            "Pet A leans toward $topA emotions, while Pet B shows more $topB tendencies."
        }
    }

    private fun buildSharedTraits(
        firstStats: Map<String, Double>,
        secondStats: Map<String, Double>
    ): List<String> {
        val shared = firstStats.keys.intersect(secondStats.keys)
        if (shared.isEmpty()) return emptyList()

        return shared
            .sortedByDescending { emotion -> min(firstStats[emotion] ?: 0.0, secondStats[emotion] ?: 0.0) }
            .map { emotion ->
                val a = formatPercentage(firstStats[emotion] ?: 0.0)
                val b = formatPercentage(secondStats[emotion] ?: 0.0)
                "Both pets often feel ${formatEmotion(emotion)} (Pet A $a, Pet B $b)."
            }
    }

    private fun buildKeyDifferences(
        firstStats: Map<String, Double>,
        secondStats: Map<String, Double>
    ): List<String> {
        val union = (firstStats.keys + secondStats.keys).toSet()
        if (union.isEmpty()) return emptyList()

        return union
            .map { emotion ->
                val a = firstStats[emotion] ?: 0.0
                val b = secondStats[emotion] ?: 0.0
                val diff = abs(a - b)
                emotion to diff
            }
            .filter { (_, diff) -> diff >= 0.12 }
            .sortedByDescending { it.second }
            .map { (emotion, diff) ->
                val aVal = formatPercentage(firstStats[emotion] ?: 0.0)
                val bVal = formatPercentage(secondStats[emotion] ?: 0.0)
                "${formatEmotion(emotion)} differs by ${formatPercentageFraction(diff)} (Pet A $aVal vs Pet B $bVal)."
            }
    }

    private fun buildRecommendations(
        firstStats: Map<String, Double>,
        secondStats: Map<String, Double>,
        sharedTraits: List<String>,
        keyDifferences: List<String>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (sharedTraits.isNotEmpty()) {
            val primarySharedEmotion = sharedTraits.firstOrNull()?.substringAfter("feel ")?.substringBefore(" (")
            if (!primarySharedEmotion.isNullOrBlank()) {
                recommendations += "Plan joint activities that encourage ${primarySharedEmotion.lowercase()} moments for both pets."
            }
        }

        if (keyDifferences.isNotEmpty()) {
            val primaryDifferenceEmotion = keyDifferences.firstOrNull()?.substringBefore(" differs")
            if (!primaryDifferenceEmotion.isNullOrBlank()) {
                recommendations += "Balance routines to support Pet B when Pet A shows strong ${primaryDifferenceEmotion.lowercase()} cues (and vice versa)."
            }
        }

        if (recommendations.isEmpty()) {
            val topA = firstStats.maxByOrNull { it.value }?.key?.let { formatEmotion(it) }
            val topB = secondStats.maxByOrNull { it.value }?.key?.let { formatEmotion(it) }
            if (topA != null && topB != null) {
                if (topA == topB) {
                    recommendations += "Celebrate shared ${topA.lowercase()} moments with joint playtime or bonding exercises."
                } else {
                    recommendations += "Alternate activities that tap into Pet A's ${topA.lowercase()} energy and Pet B's ${topB.lowercase()} mood for harmony."
                }
            }
        }

        return recommendations
    }

    private fun formatEmotion(emotion: String): String {
        if (emotion.isBlank()) return "Neutral"
        return emotion.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun formatPercentage(value: Double): String = formatPercentageFraction(value)

    private fun formatPercentageFraction(value: Double): String {
        val percentage = (value * 100).roundToInt().coerceIn(0, 100)
        return "$percentage%"
    }

    private fun ComparisonResult.enrichWith(request: CompareRequest): ComparisonResult {
        val mergedSharedTraits = if (sharedTraits.isNotEmpty()) sharedTraits else request.sharedTraits
        val mergedDifferences = if (keyDifferences.isNotEmpty()) keyDifferences else request.keyDifferences
        val mergedRecommendations = if (recommendations.isNotEmpty()) recommendations else request.recommendations
        val mergedOverview = overview.ifBlank { request.overview }
        val mergedScore = if (compatibilityScore == 0.0 && request.compatibilityScore > 0.0) {
            request.compatibilityScore
        } else {
            compatibilityScore
        }

        return copy(
            compatibilityScore = mergedScore,
            overview = mergedOverview,
            sharedTraits = mergedSharedTraits,
            keyDifferences = mergedDifferences,
            recommendations = mergedRecommendations
        )
    }

    private suspend fun requireAuthTokenOrThrow(): String {
        authToken?.let { current ->
            if (current.isNotBlank()) {
                return current
            }
        }

        println("Backend: No cached auth token, attempting registration")
        val registerResult = register()
        if (registerResult.isFailure) {
            throw registerResult.exceptionOrNull()
                ?: Exception("Authentication failed during registration.")
        }

        val token = authToken ?: registerResult.getOrThrow().token
        if (token.isBlank()) {
            throw Exception("Authentication token missing after registration.")
        }

        return token
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
    @SerialName("second") val second: List<AnalysisResult> = emptyList(),
    @SerialName("compatibilityScore") val compatibilityScore: Double = 0.0,
    val overview: String = "",
    @SerialName("sharedTraits") val sharedTraits: List<String> = emptyList(),
    @SerialName("keyDifferences") val keyDifferences: List<String> = emptyList(),
    @SerialName("recommendations") val recommendations: List<String> = emptyList()
)

@Serializable
data class ComparisonResponseWrapper(
    val result: ComparisonResult,
    val coinInfo: BackendCoinInfo? = null
)

@Serializable
data class ComparisonResult(
    val compatibilityScore: Double = 0.0,
    val overview: String = "",
    val sharedTraits: List<String> = emptyList(),
    val keyDifferences: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

// Combined result with coin info for API consumers
data class ComparisonResultWithCoinInfo(
    val compatibilityScore: Double = 0.0,
    val overview: String = "",
    val sharedTraits: List<String> = emptyList(),
    val keyDifferences: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val coinInfo: BackendCoinInfo? = null
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
