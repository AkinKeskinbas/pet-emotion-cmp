package com.keak.petemotions.presentation.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.koin.mp.KoinPlatform.getKoin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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