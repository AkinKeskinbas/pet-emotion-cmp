package com.keak.petemotions.platform

expect class PhotoPicker {
    suspend fun pickPhoto(): ByteArray?
    suspend fun pickVideo(): ByteArray?
}

expect fun createPhotoPicker(): PhotoPicker