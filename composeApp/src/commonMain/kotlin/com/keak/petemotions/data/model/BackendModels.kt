package com.keak.petemotions.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request models
@Serializable
data class RegisterRequest(
    val name: String? = null,
    val species: String? = null
)

@Serializable
data class RegisterResponse(
    val token: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("expires_at_epoch_seconds") val expiresAtEpochSeconds: Long
)

@Serializable
data class AnalyzeJsonRequest(
    @SerialName("image_base64") val imageBase64: String
)

// Response models
@Serializable
data class BackendAnalysisResult(
    val emotion: String,
    val confidence: Double,
    val summary: String,
    val tags: List<String>,
    val details: BackendAnalysisDetails
)

@Serializable
data class BackendAnalysisDetails(
    @SerialName("body_language") val bodyLanguage: String,
    val vocalization: String,
    val context: String
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
    @SerialName("http_status") val httpStatus: Int,
    @SerialName("openai_request_id") val openaiRequestId: String? = null,
    val hint: String? = null
)