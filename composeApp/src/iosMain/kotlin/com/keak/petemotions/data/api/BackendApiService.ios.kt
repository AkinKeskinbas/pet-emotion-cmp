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

actual fun getStoredBackendToken(): String? {
    return try {
        val userDefaults = NSUserDefaults.standardUserDefaults
        userDefaults.stringForKey("backend_auth_token")
    } catch (e: Exception) {
        println("Backend: Failed to get stored backend token from NSUserDefaults: ${e.message}")
        null
    }
}

actual fun setStoredBackendToken(token: String?) {
    try {
        val userDefaults = NSUserDefaults.standardUserDefaults
        if (token != null) {
            userDefaults.setObject(token, forKey = "backend_auth_token")
        } else {
            userDefaults.removeObjectForKey("backend_auth_token")
        }
        userDefaults.synchronize()
    } catch (e: Exception) {
        println("Backend: Failed to set backend token in NSUserDefaults: ${e.message}")
    }
}