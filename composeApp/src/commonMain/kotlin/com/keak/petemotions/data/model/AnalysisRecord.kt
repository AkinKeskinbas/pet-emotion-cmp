package com.keak.petemotions.data.model

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

@Serializable
data class AnalysisRecord(
    val id: String,
    val petId: String? = null,
    val mediaPath: String,
    val mediaType: String, // 'image' | 'video'
    val emotion: String, // canonical label: 'Happy', 'Relaxed', 'Playful', etc.
    val confidence: Double, // 0.0..1.0
    val summary: String,
    val detailsBody: String,
    val tags: List<String>, // e.g., ['morning','indoors']
    val createdAt: Long
) {
    companion object {
        fun create(
            petId: String? = null,
            mediaPath: String,
            mediaType: String,
            emotion: String,
            confidence: Double,
            summary: String,
            detailsBody: String,
            tags: List<String> = emptyList(),
            timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ): AnalysisRecord {
            return AnalysisRecord(
                id = uuid4().toString(),
                petId = petId,
                mediaPath = mediaPath,
                mediaType = mediaType,
                emotion = emotion,
                confidence = confidence,
                summary = summary,
                detailsBody = detailsBody,
                tags = tags,
                createdAt = timestamp
            )
        }
    }
}

enum class EmotionType(val displayName: String) {
    HAPPY("Happy"),
    RELAXED("Relaxed"),
    CURIOUS("Curious"),
    ALERT("Alert"),
    STRESSED("Stressed"),
    PLAYFUL("Playful"),
    SAD("Sad"),
    EXCITED("Excited")
}

enum class MediaType(val value: String) {
    IMAGE("image"),
    VIDEO("video")
}