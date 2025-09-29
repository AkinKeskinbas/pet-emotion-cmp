package com.keak.petemotions.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.koin.mp.KoinPlatform.getKoin
import java.io.ByteArrayInputStream

actual fun loadImageFromBytes(bytes: ByteArray): ImageBitmap? {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        bitmap?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

actual fun createPlaceholderImage(): ImageBitmap {
    // Create a simple colored bitmap for Android
    val width = 300
    val height = 200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Create gradient pattern
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val r = (x * 255 / width).coerceIn(0, 255)
            val g = (y * 255 / height).coerceIn(0, 255)
            val b = 128
            pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return bitmap.asImageBitmap()
}