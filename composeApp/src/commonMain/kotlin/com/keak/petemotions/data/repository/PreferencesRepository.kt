package com.keak.petemotions.data.repository

import com.keak.petemotions.data.model.UserPrefs
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    suspend fun getUserPrefs(): UserPrefs
    fun getUserPrefsFlow(): Flow<UserPrefs>
    suspend fun updateUserPrefs(userPrefs: UserPrefs)
    suspend fun setIsPremium(isPremium: Boolean)
    suspend fun setLanguage(language: String)
    suspend fun setHasCompletedOnboarding(completed: Boolean)
    fun hasCompletedOnboarding(): Flow<Boolean>
    suspend fun setOpenAiApiKey(apiKey: String?)
}