package com.keak.petemotions.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform.getKoin
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class AndroidCameraService : CameraService {

    private val context: Context = getKoin().get()

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentFacing = MutableStateFlow(CameraFacing.BACK)
    override val currentFacing: StateFlow<CameraFacing> = _currentFacing.asStateFlow()

    private val _isFlashEnabled = MutableStateFlow(false)
    override val isFlashEnabled: StateFlow<Boolean> = _isFlashEnabled.asStateFlow()

    override suspend fun checkCameraPermission(): CameraPermissionStatus {
        return when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> CameraPermissionStatus.GRANTED
            else -> CameraPermissionStatus.DENIED
        }
    }

    override suspend fun requestCameraPermission(): Boolean {
        // Note: Actual permission request should be handled by the UI layer
        // This is a placeholder - the UI should handle permission requests
        return checkCameraPermission() == CameraPermissionStatus.GRANTED
    }

    override suspend fun checkMicrophonePermission(): CameraPermissionStatus {
        return when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> CameraPermissionStatus.GRANTED
            else -> CameraPermissionStatus.DENIED
        }
    }

    override suspend fun requestMicrophonePermission(): Boolean {
        return checkMicrophonePermission() == CameraPermissionStatus.GRANTED
    }

    override suspend fun capturePhoto(): Result<ByteArray> {
        println("AndroidCameraService: capturePhoto called")
        return try {
            // For now, return a placeholder image that can be properly processed
            val placeholderBytes = generatePlaceholderImage()
            println("AndroidCameraService: Generated placeholder image with ${placeholderBytes.size} bytes")
            Result.success(placeholderBytes)
        } catch (e: Exception) {
            println("AndroidCameraService: Error generating placeholder image: ${e.message}")
            Result.failure(e)
        }
    }

    private fun generatePlaceholderImage(): ByteArray {
        // Create a proper JPEG image that can be decoded by BitmapFactory
        val width = 300
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create gradient pattern
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255 / width).coerceIn(0, 255)
                val g = (y * 255 / height).coerceIn(0, 255)
                val b = 128
                pixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to JPEG bytes
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return outputStream.toByteArray()
    }

    override suspend fun startVideoRecording(): Result<Unit> {
        return try {
            _isRecording.value = true
            // TODO: Implement actual video recording using CameraX
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopVideoRecording(): Result<ByteArray> {
        return try {
            _isRecording.value = false
            // TODO: Implement actual video stop and return bytes
            val dummyVideoData = ByteArray(2048) { 0xAA.toByte() }
            Result.success(dummyVideoData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun switchCamera(facing: CameraFacing) {
        _currentFacing.value = facing
        // TODO: Implement actual camera switch
    }

    override fun isFlashAvailable(): Boolean {
        // TODO: Check if device has flash
        return true
    }

    override fun setFlashEnabled(enabled: Boolean) {
        _isFlashEnabled.value = enabled
        // TODO: Implement actual flash control
    }
}

actual fun createCameraService(): CameraService = AndroidCameraService()