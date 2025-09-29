package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.platform.CameraPermissionStatus
import com.keak.petemotions.platform.PermissionType
import com.keak.petemotions.presentation.navigation.ResultDetailRoute
import com.keak.petemotions.presentation.viewmodel.CameraViewModel
import com.keak.petemotions.presentation.viewmodel.CaptureMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Permission launchers
    val cameraLauncher = rememberPermissionLauncher(PermissionType.CAMERA) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val microphoneLauncher = rememberPermissionLauncher(PermissionType.MICROPHONE) { granted ->
        viewModel.onMicrophonePermissionResult(granted)
    }

    val galleryLauncher = rememberGalleryLauncher { imageBytes ->
        viewModel.analyzeSelectedImage(imageBytes)
    }

    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        uiState.navigationEvent?.let { analysisId ->
            navController.navigate(ResultDetailRoute(analysisId))
            viewModel.clearNavigationEvent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Camera",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hint text
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = when (uiState.captureMode) {
                        CaptureMode.PHOTO -> "Ensure good lighting for best results"
                        CaptureMode.VIDEO -> "Video will automatically stop after 5 seconds"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pet selector
            if (uiState.availablePets.isNotEmpty()) {
                PetSelector(
                    pets = uiState.availablePets,
                    selectedPet = uiState.selectedPet,
                    onPetSelected = viewModel::selectPet
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Capture mode toggle
            CaptureMode_Toggle(
                captureMode = uiState.captureMode,
                onModeChanged = viewModel::setCaptureMode
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Camera preview placeholder or permission request
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.cameraPermissionStatus != CameraPermissionStatus.GRANTED -> {
                            // Camera permission not granted
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NoPhotography,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Camera permission required",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = cameraLauncher
                                ) {
                                    Text("Grant Camera Permission")
                                }
                            }
                        }
                        uiState.captureMode == CaptureMode.VIDEO &&
                        uiState.microphonePermissionStatus != CameraPermissionStatus.GRANTED -> {
                            // Microphone permission not granted for video
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Microphone permission required for video",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = microphoneLauncher
                                ) {
                                    Text("Grant Microphone Permission")
                                }
                            }
                        }
                        uiState.isAnalyzing -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Analyzing your pet's emotions...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        else -> {
                            // TODO: Implement actual camera preview
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (uiState.isRecording && uiState.recordingTimeRemaining > 0) {
                                    // Recording countdown
                                    Card(
                                        modifier = Modifier.size(80.dp),
                                        shape = RoundedCornerShape(40.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${uiState.recordingTimeRemaining}",
                                                style = MaterialTheme.typography.headlineLarge,
                                                color = MaterialTheme.colorScheme.onError,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Recording...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Camera Preview",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flip camera button
                IconButton(
                    onClick = { /* TODO: Implement camera flip */ },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Capture button
                FloatingActionButton(
                    onClick = {
                        when (uiState.captureMode) {
                            CaptureMode.PHOTO -> {
                                if (uiState.cameraPermissionStatus == CameraPermissionStatus.GRANTED) {
                                    viewModel.capturePhoto()
                                } else {
                                    cameraLauncher()
                                }
                            }
                            CaptureMode.VIDEO -> {
                                if (uiState.isRecording) {
                                    viewModel.stopVideoRecording()
                                } else {
                                    // For video, we need both camera and microphone permissions
                                    when {
                                        uiState.cameraPermissionStatus != CameraPermissionStatus.GRANTED -> cameraLauncher()
                                        uiState.microphonePermissionStatus != CameraPermissionStatus.GRANTED -> microphoneLauncher()
                                        else -> viewModel.startVideoRecording()
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = if (uiState.isRecording)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = when {
                            uiState.captureMode == CaptureMode.PHOTO -> Icons.Default.CameraAlt
                            uiState.isRecording -> Icons.Default.Stop
                            else -> Icons.Default.Videocam
                        },
                        contentDescription = when {
                            uiState.captureMode == CaptureMode.PHOTO -> "Take Photo"
                            uiState.isRecording -> "Stop Recording"
                            else -> "Start Recording"
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Gallery button
                IconButton(
                    onClick = galleryLauncher,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show proper error dialog or snackbar
            viewModel.dismissError()
        }
    }
}

@Composable
fun PetSelector(
    pets: List<com.keak.petemotions.data.model.Pet>,
    selectedPet: com.keak.petemotions.data.model.Pet?,
    onPetSelected: (com.keak.petemotions.data.model.Pet?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Pet (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "None" option
                FilterChip(
                    onClick = { onPetSelected(null) },
                    label = { Text("None") },
                    selected = selectedPet == null
                )

                // Pet options
                pets.forEach { pet ->
                    FilterChip(
                        onClick = { onPetSelected(pet) },
                        label = { Text(pet.name) },
                        selected = selectedPet?.id == pet.id
                    )
                }
            }
        }
    }
}

@Composable
fun CaptureMode_Toggle(
    captureMode: CaptureMode,
    onModeChanged: (CaptureMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        SegmentedButton(
            selected = captureMode == CaptureMode.PHOTO,
            onClick = { onModeChanged(CaptureMode.PHOTO) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
            text = "Photo"
        )

        Spacer(modifier = Modifier.width(16.dp))

        SegmentedButton(
            selected = captureMode == CaptureMode.VIDEO,
            onClick = { onModeChanged(CaptureMode.VIDEO) },
            icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
            text = "Video"
        )
    }
}

@Composable
fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: String
) {
    val colors = if (selected) {
        ButtonDefaults.filledTonalButtonColors()
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    if (selected) {
        FilledTonalButton(
            onClick = onClick,
            colors = colors
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            colors = colors
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}