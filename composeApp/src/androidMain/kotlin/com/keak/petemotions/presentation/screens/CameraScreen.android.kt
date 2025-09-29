package com.keak.petemotions.presentation.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import org.koin.mp.KoinPlatform.getKoin

@Composable
actual fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit {
    val context: Context = getKoin().get()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    println("Gallery image selected: ${bytes.size} bytes")
                    onImageSelected(bytes)
                }
            } catch (e: Exception) {
                println("Error loading gallery image: ${e.message}")
                e.printStackTrace()
            }
        } ?: println("No image selected from gallery")
    }

    return {
        launcher.launch("image/*")
    }
}