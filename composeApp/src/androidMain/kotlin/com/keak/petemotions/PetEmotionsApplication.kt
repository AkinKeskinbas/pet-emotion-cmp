package com.keak.petemotions

import android.app.Application
import com.keak.petemotions.purchase.RevenueCatInit
import com.keak.petemotions.purchase.PlatformKeys
import com.keak.petemotions.data.api.AndroidContext
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.utils.JwtParser
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PetEmotionsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Set application context for backend service
        AndroidContext.setApplicationContext(this)

        // Initialize backend and RevenueCat asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            initializeBackendAndRevenueCat()
        }

        println("PetEmotions: Application initialized")
    }

    private suspend fun initializeBackendAndRevenueCat() {
        try {
            println("PetEmotions: Starting backend registration and RevenueCat initialization...")

            val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

            // Try to get cached token and userId
            var cachedToken = prefs.getString("jwt_token", null)
            var cachedUserId = prefs.getString("user_id", null)

            println("PetEmotions: Cached token exists: ${cachedToken != null}")
            println("PetEmotions: Cached userId exists: ${cachedUserId != null}")

            // If we don't have both, register with backend
            if (cachedToken == null || cachedUserId == null) {
                println("PetEmotions: No cached credentials, registering with backend...")

                // 1. Get or create device ID
                val deviceId = getOrCreateDeviceId()
                println("PetEmotions: Using device ID: $deviceId")

                // 2. Register with backend
                val backendApi = BackendApiService()
                val registerResult = backendApi.register(userId = deviceId)

                if (registerResult.isFailure) {
                    println("PetEmotions: Backend registration failed: ${registerResult.exceptionOrNull()?.message}")
                    println("PetEmotions: Falling back to anonymous RevenueCat initialization")
                    RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = null)
                    return
                }

                val registerResponse = registerResult.getOrThrow()
                cachedToken = registerResponse.token
                println("PetEmotions: Backend registration successful, token received")

                // 3. Parse JWT to extract userId
                cachedUserId = JwtParser.getUserId(cachedToken)

                if (cachedUserId == null) {
                    println("PetEmotions: Failed to parse userId from JWT, using deviceId")
                    cachedUserId = deviceId
                }

                // 4. Cache the token and userId
                prefs.edit()
                    .putString("jwt_token", cachedToken)
                    .putString("user_id", cachedUserId)
                    .apply()
                println("PetEmotions: Credentials cached successfully")
            }

            // 5. Configure RevenueCat with the userId from JWT
            println("PetEmotions: Configuring RevenueCat with userId: $cachedUserId")
            RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = cachedUserId)
            println("PetEmotions: RevenueCat initialized successfully")

        } catch (e: Exception) {
            println("PetEmotions: Initialization failed: ${e.message}")
            println("PetEmotions: Exception type: ${e::class.simpleName}")
            println("PetEmotions: Stack trace: ${e.stackTraceToString()}")

            // Fallback to anonymous RevenueCat
            try {
                RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = null)
                println("PetEmotions: Fallback RevenueCat initialization successful")
            } catch (rcError: Exception) {
                println("PetEmotions: Fallback RevenueCat initialization also failed: ${rcError.message}")
            }
        }
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val existingDeviceId = prefs.getString("device_id", null)

        return if (existingDeviceId != null) {
            println("PetEmotions: Using existing device ID")
            existingDeviceId
        } else {
            val newDeviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newDeviceId).apply()
            println("PetEmotions: Generated new device ID")
            newDeviceId
        }
    }
}