package com.keak.petemotions.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.*
import platform.Photos.*
import kotlin.coroutines.resume

actual class PermissionService {

    actual suspend fun checkPermission(permission: PermissionType): PermissionStatus {
        return when (permission) {
            PermissionType.CAMERA -> {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
                    AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
                    AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> PermissionStatus.DENIED
                    else -> PermissionStatus.NOT_DETERMINED
                }
            }
            PermissionType.MICROPHONE -> {
                when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)) {
                    AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
                    AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> PermissionStatus.DENIED
                    else -> PermissionStatus.NOT_DETERMINED
                }
            }
            PermissionType.PHOTO_LIBRARY -> {
                when (PHPhotoLibrary.authorizationStatus()) {
                    PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> PermissionStatus.GRANTED
                    PHAuthorizationStatusDenied, PHAuthorizationStatusRestricted -> PermissionStatus.DENIED
                    else -> PermissionStatus.NOT_DETERMINED
                }
            }
        }
    }

    actual suspend fun requestPermission(permission: PermissionType): PermissionStatus {
        val current = checkPermission(permission)
        if (current == PermissionStatus.GRANTED) {
            return PermissionStatus.GRANTED
        }

        return when (permission) {
            PermissionType.CAMERA -> requestCameraPermission()
            PermissionType.MICROPHONE -> requestMicrophonePermission()
            PermissionType.PHOTO_LIBRARY -> requestPhotoLibraryPermission()
        }
    }

    private suspend fun requestCameraPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                continuation.resume(
                    if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
                )
            }
        }
    }

    private suspend fun requestMicrophonePermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
                continuation.resume(
                    if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
                )
            }
        }
    }

    private suspend fun requestPhotoLibraryPermission(): PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorization { status ->
                val permissionStatus = when (status) {
                    PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> PermissionStatus.GRANTED
                    else -> PermissionStatus.DENIED
                }
                continuation.resume(permissionStatus)
            }
        }
    }
}

actual fun createPermissionService(): PermissionService = PermissionService()