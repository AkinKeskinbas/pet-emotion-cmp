package com.keak.petemotions.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.keak.petemotions.platform.PermissionType

@Composable
actual fun rememberPermissionLauncher(
    permissionType: PermissionType,
    onResult: (Boolean) -> Unit
): () -> Unit {
    val androidPermission = when (permissionType) {
        PermissionType.CAMERA -> Manifest.permission.CAMERA
        PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        PermissionType.PHOTO_LIBRARY -> if (android.os.Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return {
        launcher.launch(androidPermission)
    }
}