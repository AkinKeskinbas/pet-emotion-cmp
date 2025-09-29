package com.keak.petemotions.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.*
import platform.UIKit.*
import kotlin.coroutines.resume

actual class PhotoPicker {
    actual suspend fun pickPhoto(): ByteArray? {
        // For now, return null - needs to be implemented with UIImagePickerController
        return null
    }

    actual suspend fun pickVideo(): ByteArray? {
        // For now, return null - needs to be implemented with UIImagePickerController
        return null
    }
}

actual fun createPhotoPicker(): PhotoPicker = PhotoPicker()