package com.keak.petemotions.purchase

import kotlin.concurrent.Volatile

object PremiumChecker {
    @Volatile
    var stateProvider: () -> PremiumState = { PremiumState.Loading }

    /** Her okuduğunda canlı state'e bakar */
    val isPremium: Boolean
        get() = stateProvider() is PremiumState.Premium

    /** İstersen tamamını da eriş: */
    val currentState: PremiumState
        get() = stateProvider()
}