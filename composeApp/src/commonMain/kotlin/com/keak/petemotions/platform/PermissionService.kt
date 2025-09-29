package com.keak.petemotions.platform

enum class PermissionType {
    CAMERA,
    MICROPHONE,
    PHOTO_LIBRARY
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_DETERMINED
}

expect class PermissionService {
    suspend fun checkPermission(permission: PermissionType): PermissionStatus
    suspend fun requestPermission(permission: PermissionType): PermissionStatus
}

expect fun createPermissionService(): PermissionService