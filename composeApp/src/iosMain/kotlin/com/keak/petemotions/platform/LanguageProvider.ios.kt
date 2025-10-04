package com.keak.petemotions.platform

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages

actual fun getCurrentLanguage(): String {
    val preferredLanguage = NSLocale.preferredLanguages.firstOrNull() as? String
    if (!preferredLanguage.isNullOrBlank()) {
        val normalized = preferredLanguage
            .replace('_', '-')
            .substringBefore('-')
            .lowercase()
            .ifBlank { null }
        if (normalized != null) {
            return normalized
        }
    }

    val locale = NSLocale.currentLocale()
    val languageCode = locale.languageCode
    return languageCode?.takeIf { it.isNotBlank() }?.lowercase() ?: "en"
}
