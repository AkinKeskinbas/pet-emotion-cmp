package com.keak.petemotions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>,
    val temperature: Double = 0.2,
    val response_format: Map<String, String> = mapOf("type" to "json_object")
)

@Serializable
data class Message(
    val role: String,
    val content: List<Content>
)

@Serializable
data class Content(
    val type: String,
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String
)

// Analysis result from OpenAI
@Serializable
data class AnalysisResult(
    val emotion: String,
    val confidence: Double,
    val summary: String,
    val details: AnalysisDetails,
    val tags: List<String>
)

@Serializable
data class AnalysisDetails(
    val bodyLanguage: String,
    val vocalization: String,
    val context: String
)