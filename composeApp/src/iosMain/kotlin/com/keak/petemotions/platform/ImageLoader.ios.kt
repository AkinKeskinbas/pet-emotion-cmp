package com.keak.petemotions.platform

import androidx.compose.ui.graphics.ImageBitmap
import platform.Foundation.NSData
import platform.UIKit.UIImage

actual fun loadImageFromBytes(bytes: ByteArray): ImageBitmap? {
    return try {
        // For iOS, we'd need to implement UIImage to ImageBitmap conversion
        // For now, return null to use placeholder
        null
    } catch (e: Exception) {
        null
    }
}

actual fun createPlaceholderImage(): ImageBitmap {
    // Create a simple colored bitmap for iOS
    val width = 300
    val height = 200
    val pixels = IntArray(width * height)

    // Create gradient pattern
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = (x * 255 / width).coerceIn(0, 255)
            val g = (y * 255 / height).coerceIn(0, 255)
            val b = 128
            pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    return ImageBitmap(width, height).apply {
        // Note: This is a simplified approach for iOS
        // In practice, you'd use proper iOS image APIs
    }
}