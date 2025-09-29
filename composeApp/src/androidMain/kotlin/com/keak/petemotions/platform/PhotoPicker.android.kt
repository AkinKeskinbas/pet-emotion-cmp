package com.keak.petemotions.platform

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform.getKoin
import kotlin.coroutines.resume

actual class PhotoPicker {
    private val context: Context = getKoin().get()

    actual suspend fun pickPhoto(): ByteArray? {
        // For now, return null - needs to be implemented with activity result launcher
        // This should be handled from the UI layer using PhotoPicker contract
        return null
    }

    actual suspend fun pickVideo(): ByteArray? {
        // For now, return null - needs to be implemented with activity result launcher
        return null
    }

    private suspend fun readBytesFromUri(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            null
        }
    }
}

actual fun createPhotoPicker(): PhotoPicker = PhotoPicker()