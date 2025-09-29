package com.keak.petemotions.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPrefs(
    val isPremium: Boolean = false,
    val language: String = "en", // 'en' | 'ja'
    val hasCompletedOnboarding: Boolean = false,
    val openAiApiKey: String? = null
) {
    companion object {
        fun default(): UserPrefs {
            return UserPrefs()
        }
    }
}

enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語")
}