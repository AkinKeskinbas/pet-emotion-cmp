package com.keak.petemotions.platform

/**
 * Provides the current language code that the user has selected on the device.
 * Platform implementations should return a BCP-47 language tag such as `en` or `ja`.
 */
expect fun getCurrentLanguage(): String
