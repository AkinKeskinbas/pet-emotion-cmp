package com.keak.petemotions.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request models
@Serializable
data class RegisterRequest(
    val name: String? = null,
    val species: String? = null,
    @SerialName("userId") val userId: String
)

@Serializable
data class RegisterResponse(
    val token: String,
    @SerialName("expiresIn") val expiresIn: Long? = null,
    @SerialName("expiresAtEpochSeconds") val expiresAtEpochSeconds: Long? = null
)

@Serializable
data class AnalyzeJsonRequest(
    @SerialName("image_base64") val imageBase64: String
)

// Response models
@Serializable
data class AnalysisResponseWrapper(
    val result: BackendAnalysisResult
)

@Serializable
data class BackendAnalysisResult(
    val emotion: String = "",
    val confidence: Double = 0.0,
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val details: BackendAnalysisDetails = BackendAnalysisDetails(),
    val coinInfo: BackendCoinInfo? = null // Backend'den gelen coin bilgisi
)

@Serializable
data class BackendCoinInfo(
    val costPaid: Int,                 // Bu işlem için ödenen coin miktarı
    val remainingBalance: Int,         // Kalan coin bakiyesi
    val operationType: String          // İşlem türü (analysis, comparison, etc.)
)

@Serializable
data class BackendAnalysisDetails(
    val bodyLanguage: String = "",
    val vocalization: String = "",
    val context: String = ""
)

@Serializable
data class ErrorResponse(
    val error: ErrorBody
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: ErrorDetails
)

@Serializable
data class ErrorDetails(
    @SerialName("httpStatus") val httpStatus: Int? = null, // Optional since OpenAI may not always include this
    @SerialName("openaiRequestId") val openaiRequestId: String? = null,
    val hint: String? = null
)

// Purchase validation models
@Serializable
data class PurchaseValidationRequest(
    val platform: String, // "ios" or "android"
    val receipt: String,   // Base64 encoded receipt data
    val transactionId: String,
    val productId: String,
    val revenueCatUserId: String // Full RevenueCat User ID with prefix
)

@Serializable
data class PurchaseValidationResponse(
    val success: Boolean,
    val coinsAdded: Int? = null,
    val newBalance: Int? = null,
    val productId: String? = null,
    val transactionId: String? = null,
    val error: String? = null
)

@Serializable
data class CoinBalanceResponse(
    val balance: Int
)
