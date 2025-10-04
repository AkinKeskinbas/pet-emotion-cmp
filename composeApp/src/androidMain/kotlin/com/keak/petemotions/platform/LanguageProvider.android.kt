package com.keak.petemotions.platform

import java.util.Locale

actual fun getCurrentLanguage(): String {
    val locale = Locale.getDefault()
    val language = locale.language
    return when {
        language.isNullOrBlank() || language.equals("und", ignoreCase = true) -> "en"
        else -> language.lowercase(Locale.ENGLISH)
    }
}
