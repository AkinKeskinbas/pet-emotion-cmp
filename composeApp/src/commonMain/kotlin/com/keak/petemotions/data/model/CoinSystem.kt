package com.keak.petemotions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoinBalance(
    val balance: Int = 0,
    val lastUpdated: Long = 0,
    val pendingPurchases: List<String> = emptyList() // Transaction IDs being processed
)

@Serializable
data class CoinPackage(
    val id: String,
    val coinAmount: Int,
    val price: String,
    val revenueCatProductId: String,
    val isPopular: Boolean = false,
    val bonusPercentage: Int = 0
)

@Serializable
data class CoinTransaction(
    val id: String,
    val type: TransactionType,
    val amount: Int,
    val reason: String,
    val timestamp: Long,
    val relatedEntityId: String? = null // Analysis ID, purchase ID, etc.
)

enum class TransactionType {
    EARNED,
    SPENT,
    PURCHASED,
    REFUNDED,
    BONUS
}

// Cost definitions
object AnalysisCost {
    const val PHOTO_ANALYSIS = 1 // Photo analysis
    const val VIDEO_ANALYSIS = 5 // Video analysis (higher cost due to complexity)
    const val COMPARISON = 5 // Pet comparison
    const val ADVANCED_INSIGHTS = 3 // Premium insights
    const val BULK_ANALYSIS = 5 // Multiple photos at once

    @Deprecated("Use PHOTO_ANALYSIS or VIDEO_ANALYSIS instead")
    const val BASIC_ANALYSIS = 1 // Keep for compatibility
}

