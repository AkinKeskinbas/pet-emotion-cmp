package com.keak.petemotions.utils

import com.keak.petemotions.data.model.AnalysisCost

/**
 * Utility object for coin-related display operations
 * Note: Coin checking and consumption is now handled by the backend
 */
object CoinUtils {

    /**
     * Get user-friendly error message for insufficient coins
     * @param cost The cost that couldn't be afforded
     * @param operationType Optional description of the operation type
     * @return User-friendly error message
     */
    fun getInsufficientCoinsMessage(cost: Int, operationType: String? = null): String {
        val operation = operationType ?: "this operation"
        return "Insufficient coins. You need $cost coins for $operation."
    }

    /**
     * Get the cost description for different operation types
     * @param cost The cost amount
     * @return Human-readable cost description
     */
    fun getCostDescription(cost: Int): String = when (cost) {
        AnalysisCost.PHOTO_ANALYSIS -> "Photo Analysis (1 coin)"
        AnalysisCost.VIDEO_ANALYSIS -> "Video Analysis (5 coins)"
        AnalysisCost.COMPARISON -> "Pet Comparison (5 coins)"
        AnalysisCost.ADVANCED_INSIGHTS -> "Advanced Insights (3 coins)"
        AnalysisCost.BULK_ANALYSIS -> "Bulk Analysis (5 coins)"
        else -> "$cost coins"
    }
}

/**
 * Extension function to get user-friendly cost display
 */
fun Int.toCostDisplay(): String = when (this) {
    AnalysisCost.PHOTO_ANALYSIS -> "🪙1"
    AnalysisCost.VIDEO_ANALYSIS -> "🪙5"
    AnalysisCost.COMPARISON -> "🪙5"
    AnalysisCost.ADVANCED_INSIGHTS -> "🪙3"
    AnalysisCost.BULK_ANALYSIS -> "🪙5"
    else -> "🪙$this"
}