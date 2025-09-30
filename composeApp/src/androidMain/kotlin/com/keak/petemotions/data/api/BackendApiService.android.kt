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