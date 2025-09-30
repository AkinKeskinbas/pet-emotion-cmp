package com.keak.petemotions

import android.app.Application
import com.keak.petemotions.purchase.RevenueCatInit
import com.keak.petemotions.purchase.PlatformKeys
import com.keak.petemotions.data.api.AndroidContext
import android.content.Context

class PetEmotionsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set application context for backend service
        AndroidContext.setApplicationContext(this)

        // Initialize RevenueCat for Android
        try {
            println("PetEmotions: Initializing RevenueCat for Android...")
            println("PetEmotions: API Key: ${PlatformKeys.revenuecatApiKey}")

            // Generate consistent user ID for RevenueCat
            val userId = getOrCreateRevenueCatUserId()
            println("PetEmotions: Using RevenueCat user ID: $userId")

            RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, userId)
            println("PetEmotions: RevenueCat initialized successfully for Android")
        } catch (e: Exception) {
            println("PetEmotions: RevenueCat initialization failed: ${e.message}")
            println("PetEmotions: Exception type: ${e::class.simpleName}")
            println("PetEmotions: Stack trace: ${e.stackTraceToString()}")
            e.cause?.let { cause ->
                println("PetEmotions: Underlying cause: ${cause.message}")
            }
        }

        println("PetEmotions: Application initialized")
    }

    private fun getOrCreateRevenueCatUserId(): String {
        val prefs = getSharedPreferences("revenuecat_prefs", Context.MODE_PRIVATE)
        val existingUserId = prefs.getString("user_id", null)

        return if (existingUserId != null) {
            println("PetEmotions: Using existing RevenueCat user ID")
            existingUserId
        } else {
            // Generate a user ID that matches what RevenueCat expects
            val newUserId = "\$RCAnonymousID:" + java.util.UUID.randomUUID().toString()
            prefs.edit().putString("user_id", newUserId).apply()
            println("PetEmotions: Generated new RevenueCat user ID")
            newUserId
        }
    }
}