package com.keak.petemotions.platform

import androidx.compose.runtime.Composable

expect class CameraNativeService {
    fun openCamera(onResult: (ByteArray?) -> Unit)
    fun openGallery(onResult: (ByteArray?) -> Unit)
    fun openVideoGallery(onResult: (ByteArray?) -> Unit)
    fun recordVideo(onResult: (ByteArray?) -> Unit)
    fun hasCamera(): Boolean
    fun hasGalleryPermission(): Boolean
    fun requestCameraPermission(onResult: (Boolean) -> Unit)
    fun requestGalleryPermission(onResult: (Boolean) -> Unit)
}

@Composable
expect fun rememberCameraNativeService(): CameraNativeService