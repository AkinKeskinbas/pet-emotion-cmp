package com.keak.petemotions.data.api

import platform.Foundation.NSUserDefaults
import platform.Foundation.NSString
import platform.Foundation.create

actual fun getStoredRevenueCatUserId(): String? {
    return try {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.stringForKey("revenuecat_user_id")
    } catch (e: Exception) {
        println("Backend: Failed to get stored RevenueCat user ID from NSUserDefaults: ${e.message}")
        null
    }
}