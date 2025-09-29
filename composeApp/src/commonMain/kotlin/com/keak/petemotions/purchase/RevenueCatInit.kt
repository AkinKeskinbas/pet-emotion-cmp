package com.keak.petemotions.purchase

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

object RevenueCatInit {
    /** Her platformda bir kez çağrılmalı */
    fun configure(apiKey: String, appUserId: String? = null) {
        try {
            Purchases.configure(apiKey) {
                appUserId?.let { appUserId(it) }
                // Configuration for sandbox/testing environments
                // Simulator and TestFlight builds may have limited App Store functionality
            }
        } catch (e: Exception) {
            // Log error but don't crash app - RevenueCat errors are non-critical
            println("RevenueCat configuration error (non-critical): ${e.message}")
        }
    }
}