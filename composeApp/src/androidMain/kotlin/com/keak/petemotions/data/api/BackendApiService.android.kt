package com.keak.petemotions.data.api

import android.content.Context

actual fun getStoredRevenueCatUserId(): String? {
    return try {
        // Get the application context from the static reference
        val context = AndroidContext.getApplicationContext()
        val prefs = context.getSharedPreferences("revenuecat_prefs", Context.MODE_PRIVATE)
        prefs.getString("user_id", null)
    } catch (e: Exception) {
        println("Backend: Failed to get stored RevenueCat user ID from SharedPreferences: ${e.message}")
        null
    }
}

actual fun getStoredBackendToken(): String? {
    return try {
        val context = AndroidContext.getApplicationContext()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.getString("backend_auth_token", null)
    } catch (e: Exception) {
        println("Backend: Failed to get stored backend token from SharedPreferences: ${e.message}")
        null
    }
}

actual fun setStoredBackendToken(token: String?) {
    try {
        val context = AndroidContext.getApplicationContext()
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (token != null) {
                putString("backend_auth_token", token)
            } else {
                remove("backend_auth_token")
            }
            apply()
        }
    } catch (e: Exception) {
        println("Backend: Failed to set backend token in SharedPreferences: ${e.message}")
    }
}

// Helper object to store application context
object AndroidContext {
    private var applicationContext: Context? = null

    fun setApplicationContext(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getApplicationContext(): Context {
        return applicationContext ?: throw IllegalStateException("Application context not set")
    }
}