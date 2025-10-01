package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.*
import com.keak.petemotions.platform.rememberCameraNativeService

@Composable
actual fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit {
    val cameraService = rememberCameraNativeService()
    val latestCallback = rememberUpdatedState(onImageSelected)

    return remember(cameraService) {
        {
            cameraService.openGallery { bytes ->
                if (bytes != null) {
                    println("iOS: Gallery image selected: ${bytes.size} bytes")
                    latestCallback.value(bytes)
                } else {
                    println("iOS: Gallery picker cancelled or failed")
                }
            }
        }
    }
}

@Composable
actual fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit
): () -> Unit {
    val cameraService = rememberCameraNativeService()
    val latestCallback = rememberUpdatedState(onPhotoCaptured)

    return remember(cameraService) {
        {
            if (!cameraService.hasCamera()) {
                println("iOS: Camera not available on this device")
            } else {
                cameraService.openCamera { bytes ->
                    if (bytes != null) {
                        println("iOS: Camera photo captured: ${bytes.size} bytes")
                        latestCallback.value(bytes)
                    } else {
                        println("iOS: Camera capture cancelled or failed")
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberVideoLauncher(
    onVideoSelected: (ByteArray) -> Unit
): () -> Unit {
    val cameraService = rememberCameraNativeService()
    val latestCallback = rememberUpdatedState(onVideoSelected)

    return remember(cameraService) {
        {
            cameraService.openVideoGallery { bytes ->
                if (bytes != null) {
                    println("iOS: Gallery video selected: ${bytes.size} bytes")
                    latestCallback.value(bytes)
                } else {
                    println("iOS: Video gallery picker cancelled or failed")
                }
            }
        }
    }
}
