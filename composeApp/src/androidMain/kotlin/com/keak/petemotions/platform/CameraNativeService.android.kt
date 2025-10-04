package com.keak.petemotions.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.ByteArrayOutputStream

actual class CameraNativeService(
    private val context: Context,
    private val cameraLauncher: ActivityResultLauncher<Intent>?,
    private val galleryLauncher: ActivityResultLauncher<String>?,
    private val videoGalleryLauncher: ActivityResultLauncher<String>?,
    private val videoRecorderLauncher: ActivityResultLauncher<Intent>?
) {
    private var currentCameraCallback: ((ByteArray?) -> Unit)? = null
    private var currentGalleryCallback: ((ByteArray?) -> Unit)? = null
    private var currentVideoGalleryCallback: ((ByteArray?) -> Unit)? = null
    private var currentVideoRecordCallback: ((ByteArray?) -> Unit)? = null

    actual fun openCamera(onResult: (ByteArray?) -> Unit) {
        currentCameraCallback = onResult
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher?.launch(intent)
    }

    actual fun openGallery(onResult: (ByteArray?) -> Unit) {
        currentGalleryCallback = onResult
        galleryLauncher?.launch("image/*")
    }

    actual fun openVideoGallery(onResult: (ByteArray?) -> Unit) {
        currentVideoGalleryCallback = onResult
        videoGalleryLauncher?.launch("video/*")
    }

    actual fun recordVideo(onResult: (ByteArray?) -> Unit) {
        currentVideoRecordCallback = onResult
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoRecorderLauncher?.launch(intent)
    }

    actual fun hasCamera(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }

    actual fun hasGalleryPermission(): Boolean {
        return true // Gallery access doesn't need special permission on Android
    }

    actual fun requestCameraPermission(onResult: (Boolean) -> Unit) {
        // Android uses manifest permissions, so this is handled at install time
        onResult(true)
    }

    actual fun requestGalleryPermission(onResult: (Boolean) -> Unit) {
        // Gallery access doesn't need runtime permission
        onResult(true)
    }

    fun handleCameraResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val imageBitmap = result.data?.extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val imageBytes = outputStream.toByteArray()
                    println("Android Native: Camera photo captured: ${imageBytes.size} bytes")
                    currentCameraCallback?.invoke(imageBytes)
                } else {
                    println("Android Native: No image data received")
                    currentCameraCallback?.invoke(null)
                }
            } catch (e: Exception) {
                println("Android Native: Camera error: ${e.message}")
                currentCameraCallback?.invoke(null)
            }
        } else {
            println("Android Native: Camera capture cancelled")
            currentCameraCallback?.invoke(null)
        }
        currentCameraCallback = null
    }

    fun handleGalleryResult(uri: Uri?) {
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    println("Android Native: Gallery image selected: ${bytes.size} bytes")
                    currentGalleryCallback?.invoke(bytes)
                }
            } catch (e: Exception) {
                println("Android Native: Error loading gallery image: ${e.message}")
                currentGalleryCallback?.invoke(null)
            }
        } ?: run {
            println("Android Native: No image selected from gallery")
            currentGalleryCallback?.invoke(null)
        }
        currentGalleryCallback = null
    }

    fun handleVideoGalleryResult(uri: Uri?) {
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    println("Android Native: Gallery video selected: ${bytes.size} bytes")
                    currentVideoGalleryCallback?.invoke(bytes)
                }
            } catch (e: Exception) {
                println("Android Native: Error loading gallery video: ${e.message}")
                currentVideoGalleryCallback?.invoke(null)
            }
        } ?: run {
            println("Android Native: No video selected from gallery")
            currentVideoGalleryCallback?.invoke(null)
        }
        currentVideoGalleryCallback = null
    }

    fun handleVideoRecordResult(result: androidx.activity.result.ActivityResult) {
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            try {
                val videoUri = result.data?.data
                videoUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        println("Android Native: Video recorded: ${bytes.size} bytes")
                        currentVideoRecordCallback?.invoke(bytes)
                    }
                } ?: run {
                    println("Android Native: No video URI received")
                    currentVideoRecordCallback?.invoke(null)
                }
            } catch (e: Exception) {
                println("Android Native: Video record error: ${e.message}")
                currentVideoRecordCallback?.invoke(null)
            }
        } else {
            println("Android Native: Video recording cancelled")
            currentVideoRecordCallback?.invoke(null)
        }
        currentVideoRecordCallback = null
    }
}

@Composable
actual fun rememberCameraNativeService(): CameraNativeService {
    val context = LocalContext.current
    var service by remember { mutableStateOf<CameraNativeService?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        service?.handleCameraResult(result)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        service?.handleGalleryResult(uri)
    }

    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        service?.handleVideoGalleryResult(uri)
    }

    val videoRecorderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        service?.handleVideoRecordResult(result)
    }

    LaunchedEffect(Unit) {
        service = CameraNativeService(
            context,
            cameraLauncher,
            galleryLauncher,
            videoGalleryLauncher,
            videoRecorderLauncher
        )
    }

    return service ?: CameraNativeService(context, null, null, null, null)
}