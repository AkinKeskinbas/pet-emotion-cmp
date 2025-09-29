package com.keak.petemotions.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.koin.mp.KoinPlatform.getKoin

actual class PermissionService {
    private val context: Context = getKoin().get()

    actual suspend fun checkPermission(permission: PermissionType): PermissionStatus {
        val androidPermission = when (permission) {
            PermissionType.CAMERA -> Manifest.permission.CAMERA
            PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            PermissionType.PHOTO_LIBRARY -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        return when (ContextCompat.checkSelfPermission(context, androidPermission)) {
            PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            else -> PermissionStatus.DENIED
        }
    }

    actual suspend fun requestPermission(permission: PermissionType): PermissionStatus {
        val current = checkPermission(permission)
        if (current == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }

        val androidPermission = when (permission) {
            PermissionType.CAMERA -> Manifest.permission.CAMERA
            PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            PermissionType.PHOTO_LIBRARY -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }

        // For now, return denied for demo - actual implementation needs activity context
        // This should be implemented properly using ActivityResultLauncher from the UI layer
        return PermissionStatus.DENIED
    }
}

actual fun createPermissionService(): PermissionService = PermissionService()