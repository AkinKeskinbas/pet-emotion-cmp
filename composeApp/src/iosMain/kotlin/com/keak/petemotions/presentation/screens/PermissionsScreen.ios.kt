package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.*
import com.keak.petemotions.platform.PermissionService
import com.keak.petemotions.platform.PermissionStatus
import com.keak.petemotions.platform.PermissionType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun rememberPermissionLauncher(
    permissionType: PermissionType,
    onResult: (Boolean) -> Unit
): () -> Unit {
    val permissionService: PermissionService = koinInject()
    val scope = rememberCoroutineScope()

    return {
        scope.launch {
            val result = permissionService.requestPermission(permissionType)
            val isGranted = result == PermissionStatus.GRANTED
            onResult(isGranted)

            // Re-check permission status after request to ensure state is updated
            if (!isGranted) {
                kotlinx.coroutines.delay(500) // Small delay to ensure iOS system dialog is dismissed
                val recheckResult = permissionService.checkPermission(permissionType)
                onResult(recheckResult == PermissionStatus.GRANTED)
            }
        }
    }
}