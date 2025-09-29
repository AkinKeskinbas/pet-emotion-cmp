package com.keak.petemotions.platform

import kotlinx.coroutines.flow.Flow

enum class CameraFacing {
    FRONT, BACK
}

enum class CameraPermissionStatus {
    GRANTED, DENIED, NOT_REQUESTED
}

interface CameraService {

    suspend fun checkCameraPermission(): CameraPermissionStatus
    suspend fun requestCameraPermission(): Boolean

    suspend fun checkMicrophonePermission(): CameraPermissionStatus
    suspend fun requestMicrophonePermission(): Boolean

    suspend fun capturePhoto(): Result<ByteArray>
    suspend fun startVideoRecording(): Result<Unit>
    suspend fun stopVideoRecording(): Result<ByteArray>

    fun switchCamera(facing: CameraFacing)
    fun isFlashAvailable(): Boolean
    fun setFlashEnabled(enabled: Boolean)

    // Observable states
    val isRecording: Flow<Boolean>
    val currentFacing: Flow<CameraFacing>
    val isFlashEnabled: Flow<Boolean>
}

expect fun createCameraService(): CameraService