package com.keak.petemotions.purchase

import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import kotlinx.coroutines.flow.StateFlow

interface PremiumRepository {
val state: StateFlow<PremiumState>
/**
* Refreshes CustomerInfo and updates [state].
* Use [CacheFetchPolicy.FETCH_CURRENT] when you need a guaranteed fresh value (e.g. on app start
* or right after a purchase/restore), otherwise the default is fine for most checks.
*/
suspend fun refresh(fetchPolicy: CacheFetchPolicy = CacheFetchPolicy.CACHED_OR_FETCHED)
}
sealed interface PremiumState {
    data object Loading : PremiumState
    data object Free : PremiumState
    data object Premium : PremiumState
    data class Error(val message: String) : PremiumState
}
object Entitlements {
    const val PREMIUM = "premium" // e.g., "pro", "plus", etc.
}