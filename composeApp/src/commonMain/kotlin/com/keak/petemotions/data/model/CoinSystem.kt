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

// Default coin packages
object CoinPackages {
    val STARTER = CoinPackage(
        id = "coins_10",
        coinAmount = 10,
        price = "\$0.99",
        revenueCatProductId = "pet_emotions_coins_10"
    )

    val POPULAR = CoinPackage(
        id = "coins_50",
        coinAmount = 50,
        price = "\$3.99",
        revenueCatProductId = "pet_emotions_coins_50",
        isPopular = true,
        bonusPercentage = 20
    )

    val VALUE = CoinPackage(
        id = "coins_100",
        coinAmount = 100,
        price = "\$6.99",
        revenueCatProductId = "pet_emotions_coins_100",
        bonusPercentage = 30
    )

    val PREMIUM = CoinPackage(
        id = "coins_250",
        coinAmount = 250,
        price = "\$14.99",
        revenueCatProductId = "pet_emotions_coins_250",
        bonusPercentage = 40
    )

    val ALL_PACKAGES = listOf(STARTER, POPULAR, VALUE, PREMIUM)
}
