package com.keak.petemotions.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

actual fun loadImageFromBytes(bytes: ByteArray): ImageBitmap? {
    return runCatching<ImageBitmap> {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.getOrElse { primaryError ->
        println("iOS: Skia failed to decode image bytes (${primaryError.message}), attempting UIKit fallback")

        val nsData = bytes.toNSData()
        val uiImage = UIImage(data = nsData)
        if (uiImage == null) {
            println("iOS: UIKit could not create UIImage from data")
            return null
        }

        val reencodedData = UIImageJPEGRepresentation(uiImage, 0.9) ?: UIImagePNGRepresentation(uiImage)
        if (reencodedData == null) {
            println("iOS: Failed to re-encode UIImage to JPEG/PNG")
            return null
        }

        runCatching<ImageBitmap> {
            Image.makeFromEncoded(reencodedData.toByteArray()).toComposeImageBitmap()
        }.onFailure { fallbackError ->
            println("iOS: Fallback decode also failed: ${fallbackError.message}")
        }.getOrNull()
    }
}

actual fun createPlaceholderImage(): ImageBitmap {
    val width = 300
    val height = 200
    val imageInfo = ImageInfo.makeS32(width, height, ColorAlphaType.PREMUL)
    val surface = Surface.makeRaster(imageInfo)
    surface.canvas.clear(Color.makeARGB(255, 180, 160, 200))
    return surface.makeImageSnapshot().toComposeImageBitmap()
}

private fun ByteArray.toNSData(): NSData {
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    val bytes = ByteArray(length)
    if (length > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length.convert())
        }
    }
    return bytes
}
