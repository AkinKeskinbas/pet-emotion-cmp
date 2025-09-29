package com.keak.petemotions.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
        return try {
            suspendCancellableCoroutine { continuation ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        // Image capture use case
                        val imageCapture = ImageCapture.Builder().build()

                        // Image analyzer for getting bytes
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            val buffer = imageProxy.planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            imageProxy.close()

                            // Convert YUV to simple RGB bytes (placeholder)
                            continuation.resume(Result.success(bytes))
                        }

                        // For now, use a simple placeholder that works
                        val dummyImageBytes = generatePlaceholderImage()
                        continuation.resume(Result.success(dummyImageBytes))

                    } catch (e: Exception) {
                        continuation.resume(Result.failure(e))
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generatePlaceholderImage(): ByteArray {
        // Generate a simple 100x100 RGB image for testing
        val width = 100
        val height = 100
        val imageData = ByteArray(width * height * 3) // RGB

        // Create a simple gradient pattern
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * 3
                imageData[index] = (x * 255 / width).toByte()     // R
                imageData[index + 1] = (y * 255 / height).toByte() // G
                imageData[index + 2] = 128.toByte()                // B
            }
        }

        return imageData
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