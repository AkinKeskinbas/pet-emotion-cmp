package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.*

@Composable
actual fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit {
    // For now, return empty lambda - needs PHPickerViewController implementation
    return {
        // TODO: Implement iOS photo picker
    }
}