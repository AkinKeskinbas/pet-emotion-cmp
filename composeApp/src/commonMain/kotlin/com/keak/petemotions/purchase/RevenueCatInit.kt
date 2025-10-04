package com.keak.petemotions.purchase

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.configure

object RevenueCatInit {
    /** Her platformda bir kez çağrılmalı */
    fun configure(apiKey: String, appUserId: String? = null) {
        try {
            println("RevenueCat: Starting configuration...")
            println("RevenueCat: API Key: ${apiKey.take(20)}...")
            println("RevenueCat: App User ID: $appUserId")
            println("RevenueCat: Already configured: ${Purchases.isConfigured}")

            if (Purchases.isConfigured) {
                println("RevenueCat: Already configured, skipping")
                return
            }

            Purchases.logLevel = LogLevel.DEBUG
            println("RevenueCat: Setting log level to DEBUG")

            Purchases.configure(apiKey) {
                appUserId?.let {
                    println("RevenueCat: Setting app user ID: $it")
                    appUserId(it)
                }
                // Configuration for sandbox/testing environments
                // Simulator and TestFlight builds may have limited App Store functionality
            }

            println("RevenueCat: Configuration completed")
            println("RevenueCat: isConfigured after setup: ${Purchases.isConfigured}")

            if (Purchases.isConfigured) {
                println("RevenueCat: Verified - SDK is now configured")
                println("RevenueCat: User ID: ${Purchases.sharedInstance.appUserID}")
            } else {
                println("RevenueCat: WARNING - isConfigured still false after configure!")
            }
        } catch (e: Exception) {
            // Log error but don't crash app - RevenueCat errors are non-critical
            println("RevenueCat configuration error (non-critical): ${e.message}")
            println("RevenueCat error stack trace:")
            e.printStackTrace()
        }
    }
}