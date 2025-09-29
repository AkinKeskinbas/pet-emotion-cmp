package com.keak.petemotions.presentation.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import org.koin.mp.KoinPlatform.getKoin
import java.io.ByteArrayOutputStream

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

@Composable
actual fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit
): () -> Unit {
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                // Get the captured image from the Intent extras
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    // Convert bitmap to ByteArray
                    val outputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    println("Camera photo captured: ${imageBytes.size} bytes")
                    onPhotoCaptured(imageBytes)
                } else {
                    println("Camera: No image data received")
                }
            } catch (e: Exception) {
                println("Camera error: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("Camera: Capture cancelled or failed")
        }
    }

    return {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
}