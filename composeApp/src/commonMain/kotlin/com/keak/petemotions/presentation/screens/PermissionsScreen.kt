package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.platform.PermissionService
import com.keak.petemotions.platform.PermissionStatus
import com.keak.petemotions.platform.PermissionType
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.*

@Composable
expect fun rememberPermissionLauncher(
    permissionType: PermissionType,
    onResult: (Boolean) -> Unit
): () -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    navController: NavController,
    onContinue: () -> Unit = {}
) {
    val permissionService: PermissionService = koinInject()

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var microphonePermissionGranted by remember { mutableStateOf(false) }
    var photoLibraryPermissionGranted by remember { mutableStateOf(false) }

    // Permission launchers
    val cameraLauncher = rememberPermissionLauncher(PermissionType.CAMERA) { granted ->
        cameraPermissionGranted = granted
    }

    val microphoneLauncher = rememberPermissionLauncher(PermissionType.MICROPHONE) { granted ->
        microphonePermissionGranted = granted
    }

    val photoLibraryLauncher = rememberPermissionLauncher(PermissionType.PHOTO_LIBRARY) { granted ->
        photoLibraryPermissionGranted = granted
    }

    // Check permissions on start
    LaunchedEffect(Unit) {
        cameraPermissionGranted = permissionService.checkPermission(PermissionType.CAMERA) == PermissionStatus.GRANTED
        microphonePermissionGranted = permissionService.checkPermission(PermissionType.MICROPHONE) == PermissionStatus.GRANTED
        photoLibraryPermissionGranted = permissionService.checkPermission(PermissionType.PHOTO_LIBRARY) == PermissionStatus.GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Icon
            Card(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(60.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title and description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(Res.string.permissions_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.permissions_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions list
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionItem(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(Res.string.permissions_camera_title),
                    description = stringResource(Res.string.permissions_camera_description),
                    isGranted = cameraPermissionGranted,
                    onRequest = cameraLauncher
                )

                PermissionItem(
                    icon = Icons.Default.Mic,
                    title = stringResource(Res.string.permissions_microphone_title),
                    description = stringResource(Res.string.permissions_microphone_description),
                    isGranted = microphonePermissionGranted,
                    onRequest = microphoneLauncher
                )

                PermissionItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(Res.string.permissions_photo_library_title),
                    description = stringResource(Res.string.permissions_photo_library_description),
                    isGranted = photoLibraryPermissionGranted,
                    onRequest = photoLibraryLauncher
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Privacy note
            Text(
                text = stringResource(Res.string.permissions_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Continue button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = cameraPermissionGranted, // At minimum, camera is required
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.action_continue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!cameraPermissionGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.permissions_camera_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Extra bottom spacing to ensure button is always visible
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isGranted) Icons.Default.Check else icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isGranted) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action button
            if (isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(Res.string.permissions_granted),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(Res.string.permissions_allow))
                }
            }
        }
    }
}