package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keak.petemotions.data.model.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    companion object {
        private val IS_PREMIUM_KEY = booleanPreferencesKey("is_premium")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        private val HAS_COMPLETED_ONBOARDING_KEY = booleanPreferencesKey("has_completed_onboarding")
        private val OPENAI_API_KEY = stringPreferencesKey("openai_api_key")
    }

    override suspend fun getUserPrefs(): UserPrefs {
        return getUserPrefsFlow().first()
    }

    override fun getUserPrefsFlow(): Flow<UserPrefs> {
        return dataStore.data.map { preferences ->
            UserPrefs(
                isPremium = preferences[IS_PREMIUM_KEY] ?: false,
                language = preferences[LANGUAGE_KEY] ?: "en",
                hasCompletedOnboarding = preferences[HAS_COMPLETED_ONBOARDING_KEY] ?: false,
                openAiApiKey = preferences[OPENAI_API_KEY]
            )
        }
    }

    override suspend fun updateUserPrefs(userPrefs: UserPrefs) {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = userPrefs.isPremium
            preferences[LANGUAGE_KEY] = userPrefs.language
            preferences[HAS_COMPLETED_ONBOARDING_KEY] = userPrefs.hasCompletedOnboarding
            userPrefs.openAiApiKey?.let { key ->
                preferences[OPENAI_API_KEY] = key
            }
        }
    }

    override suspend fun setIsPremium(isPremium: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM_KEY] = isPremium
        }
    }

    override suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING_KEY] = completed
        }
    }

    override fun hasCompletedOnboarding(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING_KEY] ?: false
        }
    }

    override suspend fun setOpenAiApiKey(apiKey: String?) {
        dataStore.edit { preferences ->
            if (apiKey != null) {
                preferences[OPENAI_API_KEY] = apiKey
            } else {
                preferences.remove(OPENAI_API_KEY)
            }
        }
    }
}