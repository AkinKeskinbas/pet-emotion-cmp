package com.keak.petemotions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisResult(
    val emotion: String,
    val confidence: Double,
    val summary: String,
    val details: AnalysisDetails,
    val tags: List<String>
) {
    @Serializable
    data class AnalysisDetails(
        val bodyLanguage: String,
        val vocalization: String,
        val context: String
    )
}