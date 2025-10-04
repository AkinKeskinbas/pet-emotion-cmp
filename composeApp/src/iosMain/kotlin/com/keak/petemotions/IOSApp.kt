package com.keak.petemotions

import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.purchase.PlatformKeys
import com.keak.petemotions.purchase.RevenueCatInit
import com.keak.petemotions.utils.JwtParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID

/**
 * Initialize backend and RevenueCat for iOS
 * Call this from Swift before showing the UI
 * This is synchronous and must complete before UI loads
 */
suspend fun initializeIOSApp() {
    println("iOS: Starting app initialization...")

    try {
        initializeBackendAndRevenueCat()
    } catch (e: Exception) {
        println("iOS: Initialization error: ${e.message}")
        e.printStackTrace()

        // Fallback to anonymous RevenueCat
        try {
            RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = null)
            println("iOS: Fallback RevenueCat initialization successful")
        } catch (rcError: Exception) {
            println("iOS: Fallback RevenueCat initialization also failed: ${rcError.message}")
        }
    }
}

/**
 * Synchronous wrapper for Swift - blocks until initialization is complete
 */
fun initializeIOSAppBlocking() {
    println("iOS: Starting blocking app initialization...")
    kotlinx.coroutines.runBlocking {
        initializeIOSApp()
    }
    println("iOS: Blocking initialization complete")
}

private suspend fun initializeBackendAndRevenueCat() {
    println("iOS: Starting backend registration and RevenueCat initialization...")

    val userDefaults = NSUserDefaults.standardUserDefaults

    // Try to get cached token and userId
    var cachedToken = userDefaults.stringForKey("jwt_token")
    var cachedUserId = userDefaults.stringForKey("user_id")

    println("iOS: Cached token exists: ${cachedToken != null}")
    println("iOS: Cached userId exists: ${cachedUserId != null}")

    // If we don't have both, register with backend
    if (cachedToken == null || cachedUserId == null) {
        println("iOS: No cached credentials, registering with backend...")

        // 1. Get or create device ID
        val deviceId = getOrCreateDeviceId()
        println("iOS: Using device ID: $deviceId")

        // 2. Register with backend
        val backendApi = BackendApiService()
        val registerResult = backendApi.register(userId = deviceId)

        if (registerResult.isFailure) {
            println("iOS: Backend registration failed: ${registerResult.exceptionOrNull()?.message}")
            println("iOS: Falling back to anonymous RevenueCat initialization")
            RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = null)
            return
        }

        val registerResponse = registerResult.getOrThrow()
        cachedToken = registerResponse.token
        println("iOS: Backend registration successful, token received")

        // 3. Parse JWT to extract userId
        cachedUserId = JwtParser.getUserId(cachedToken)

        if (cachedUserId == null) {
            println("iOS: Failed to parse userId from JWT, using deviceId")
            cachedUserId = deviceId
        }

        // 4. Cache the token and userId
        userDefaults.setObject(cachedToken, forKey = "jwt_token")
        userDefaults.setObject(cachedUserId, forKey = "user_id")
        userDefaults.setObject(cachedToken, forKey = "backend_auth_token") // For BackendApiService
        userDefaults.synchronize()
        println("iOS: Credentials cached successfully")
    }

    // 5. Configure RevenueCat with the userId from JWT
    println("iOS: Configuring RevenueCat with userId: $cachedUserId")
    RevenueCatInit.configure(PlatformKeys.revenuecatApiKey, appUserId = cachedUserId)
    println("iOS: RevenueCat initialized successfully")
}

private fun getOrCreateDeviceId(): String {
    val userDefaults = NSUserDefaults.standardUserDefaults
    val existingDeviceId = userDefaults.stringForKey("device_id")

    return if (existingDeviceId != null) {
        println("iOS: Using existing device ID")
        existingDeviceId
    } else {
        val newDeviceId = NSUUID().UUIDString()
        userDefaults.setObject(newDeviceId, forKey = "device_id")
        userDefaults.synchronize()
        println("iOS: Generated new device ID")
        newDeviceId
    }
}
