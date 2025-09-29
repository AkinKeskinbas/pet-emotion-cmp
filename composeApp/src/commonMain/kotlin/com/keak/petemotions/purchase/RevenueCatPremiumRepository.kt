package com.keak.petemotions.purchase

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.models.CacheFetchPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RevenueCatPremiumRepository(
    private val entitlementId: String = Entitlements.PREMIUM,
    private val purchases: Purchases = Purchases.sharedInstance,
    private val io: CoroutineDispatcher = Dispatchers.Default
) : PremiumRepository {


    private val _state = MutableStateFlow<PremiumState>(PremiumState.Loading)
    override val state: StateFlow<PremiumState> = _state.asStateFlow()


    override suspend fun refresh(fetchPolicy: CacheFetchPolicy) = withContext(io) {
        try {
            val info = purchases.awaitCustomerInfo(fetchPolicy)
            val isPremium = info.entitlements.all[entitlementId]?.isActive == true
            _state.value = if (isPremium) PremiumState.Premium else PremiumState.Free
        } catch (t: Throwable) {
            // Handle "No active account" errors gracefully
            when {
                t.message?.contains("No active account") == true -> {
                    // Default to Free state when no App Store account (normal in simulator)
                    _state.value = PremiumState.Free
                    println("RevenueCat: No active account (simulator/testing) - defaulting to Free")
                }
                t.message?.contains("ASDErrorDomain") == true -> {
                    // App Store Service Domain errors - also default to Free
                    _state.value = PremiumState.Free
                    println("RevenueCat: App Store Service error - defaulting to Free")
                }
                else -> {
                    _state.value = PremiumState.Error(t.message ?: "Unknown error")
                }
            }
        }
    }
}