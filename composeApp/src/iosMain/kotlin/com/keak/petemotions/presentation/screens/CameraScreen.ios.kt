package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.*

@Composable
actual fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit {
    return {
        println("iOS: Gallery picker functionality")
        // For now, using mock implementation
        // TODO: Implement native iOS PHPhotoLibrary gallery picker
        val mockImageBytes = createMockImageBytes()
        onImageSelected(mockImageBytes)
    }
}

@Composable
actual fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit
): () -> Unit {
    return {
        println("iOS: Camera capture functionality")
        // For now, using mock implementation
        // TODO: Implement native iOS UIImagePickerController camera
        val mockImageBytes = createMockImageBytes()
        onPhotoCaptured(mockImageBytes)
    }
}

private fun createMockImageBytes(): ByteArray {
    // Create a simple mock JPEG header for testing purposes
    // This simulates a small image file
    return byteArrayOf(
        // JPEG header
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
        // Mock image data (simplified)
        0x01, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
        0x00, 0x00, 0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00,
        // JPEG end marker
        0xFF.toByte(), 0xD9.toByte()
    )
}