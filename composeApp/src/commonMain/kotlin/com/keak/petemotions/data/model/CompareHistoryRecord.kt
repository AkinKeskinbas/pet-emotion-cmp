package com.keak.petemotions.data.model

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

@Serializable
data class CompareHistoryRecord(
    val id: String,
    val petAId: String?,
    val petAName: String,
    val petBId: String?,
    val petBName: String,
    val compatibilityScore: Double,
    val overview: String,
    val sharedTraits: List<String>,
    val keyDifferences: List<String>,
    val recommendations: List<String>,
    val createdAt: Long
) {
    companion object {
        fun create(
            petAId: String?,
            petAName: String,
            petBId: String?,
            petBName: String,
            compatibilityScore: Double,
            overview: String,
            sharedTraits: List<String>,
            keyDifferences: List<String>,
            recommendations: List<String>,
            timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ): CompareHistoryRecord {
            return CompareHistoryRecord(
                id = uuid4().toString(),
                petAId = petAId,
                petAName = petAName,
                petBId = petBId,
                petBName = petBName,
                compatibilityScore = compatibilityScore,
                overview = overview,
                sharedTraits = sharedTraits,
                keyDifferences = keyDifferences,
                recommendations = recommendations,
                createdAt = timestamp
            )
        }
    }
}
