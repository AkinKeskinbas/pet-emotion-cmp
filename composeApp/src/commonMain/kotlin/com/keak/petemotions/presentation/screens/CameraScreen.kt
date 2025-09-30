package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.NavController
import com.keak.petemotions.platform.CameraPermissionStatus
import com.keak.petemotions.platform.PermissionType
import com.keak.petemotions.platform.createPlaceholderImage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.navigation.ResultDetailRoute
import com.keak.petemotions.presentation.navigation.PaywallRoute
import com.keak.petemotions.presentation.viewmodel.CameraViewModel
import com.keak.petemotions.presentation.viewmodel.CaptureMode
import com.keak.petemotions.data.model.AnalysisCost
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun rememberGalleryLauncher(
    onImageSelected: (ByteArray) -> Unit
): () -> Unit

@Composable
expect fun rememberCameraLauncher(
    onPhotoCaptured: (ByteArray) -> Unit
): () -> Unit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Show error messages in snackbar
    LaunchedEffect(uiState.error?.message) {
        uiState.error?.let { error ->
            println("CameraScreen: Showing snackbar with message: ${error.message}")
            // Clear error first to prevent re-triggering
            viewModel.dismissError()

            // Use coroutineScope to avoid composition scope issues
            coroutineScope.launch {
                try {
                    val result = snackbarHostState.showSnackbar(
                        message = error.message,
                        duration = SnackbarDuration.Long
                    )
                    println("CameraScreen: Snackbar result: $result")
                } catch (e: Exception) {
                    println("CameraScreen: Error showing snackbar: ${e.message}")
                }
            }
        }
    }

    // Permission launchers
    val cameraPermissionLauncher = rememberPermissionLauncher(PermissionType.CAMERA) { granted ->
        viewModel.onPermissionResult(granted)
    }

    val microphoneLauncher = rememberPermissionLauncher(PermissionType.MICROPHONE) { granted ->
        viewModel.onMicrophonePermissionResult(granted)
    }

    val galleryLauncher = rememberGalleryLauncher { imageBytes ->
        viewModel.analyzeSelectedImage(imageBytes)
    }

    val systemCameraLauncher = rememberCameraLauncher { imageBytes ->
        println("CameraScreen: Camera photo received: ${imageBytes.size} bytes")
        // Store the captured photo directly in state for preview
        viewModel.onPhotoCaptured(imageBytes)
    }

    // Debug permission status
    LaunchedEffect(uiState.cameraPermissionStatus) {
        println("CameraScreen: Camera permission status = ${uiState.cameraPermissionStatus}")
    }

    // Handle navigation events
    LaunchedEffect(uiState.navigationEvent) {
        uiState.navigationEvent?.let { analysisId ->
            println("CameraScreen: Navigating to result with ID: $analysisId")
            navController.navigate(ResultDetailRoute(analysisId))
            viewModel.clearNavigationEvent()
        }
    }

    // Handle paywall navigation for insufficient coins
    LaunchedEffect(uiState.paywallNavigationEvent) {
        if (uiState.paywallNavigationEvent) {
            println("CameraScreen: Auto-opening paywall due to insufficient coins")
            navController.navigate(PaywallRoute)
            viewModel.clearPaywallNavigationEvent()
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
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF424242), // Dark gray
                        contentColor = Color(0xFFFFFFFF),   // White text
                        actionColor = Color(0xFF90CAF9)     // Light blue for actions
                    )
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

            // Capture mode toggle - Video coming soon
            CaptureMode_Toggle(
                captureMode = uiState.captureMode,
                onModeChanged = { mode ->
                    println("CameraScreen: Mode changed to: $mode")
                    if (mode == CaptureMode.VIDEO) {
                        // Show coming soon message for video
                        println("CameraScreen: Video mode selected, showing coming soon message")
                        viewModel.showComingSoonMessage("Video recording coming soon in v2!")
                    } else {
                        println("CameraScreen: Photo mode selected")
                        viewModel.setCaptureMode(mode)
                    }
                }
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
                                    onClick = cameraPermissionLauncher
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
                        uiState.capturedPhotoBytes != null -> {
                            // Show captured photo
                            val imageBitmap = remember(uiState.capturedPhotoBytes) {
                                loadImageFromBytes(uiState.capturedPhotoBytes!!) ?: createPlaceholderImage()
                            }
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Captured photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            // Camera preview placeholder
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
                                        text = "Tap capture to take photo",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animated coin error display
            AnimatedVisibility(
                visible = uiState.coinError != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = EaseOutCubic)
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = EaseInCubic)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.coinError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.dismissCoinError() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls
            if (uiState.capturedPhotoBytes != null) {
                // Photo captured - show retake and analyze buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Retake button
                    OutlinedButton(
                        onClick = {
                            println("CameraScreen: Retake button clicked")
                            viewModel.retakePhoto()
                        },
                        enabled = !uiState.isAnalyzing
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake")
                    }

                    // Analyze button
                    Button(
                        onClick = {
                            println("CameraScreen: Analyze button clicked")
                            viewModel.analyzeCurrentPhoto()
                        },
                        enabled = !uiState.isAnalyzing
                    ) {
                        if (uiState.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing...")
                        } else {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze")
                        }
                    }
                }
            } else {
                // Normal camera controls
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
                            // Don't handle clicks while analyzing
                            if (uiState.isAnalyzing) return@FloatingActionButton

                            println("CameraScreen: Capture button clicked, mode=${uiState.captureMode}")
                            when (uiState.captureMode) {
                                CaptureMode.PHOTO -> {
                                    println("CameraScreen: Photo mode, permission=${uiState.cameraPermissionStatus}")
                                    if (uiState.cameraPermissionStatus == CameraPermissionStatus.GRANTED) {
                                        println("CameraScreen: Opening system camera...")
                                        systemCameraLauncher()
                                    } else {
                                        println("CameraScreen: Requesting camera permission...")
                                        cameraPermissionLauncher()
                                    }
                                }
                                CaptureMode.VIDEO -> {
                                    if (uiState.isRecording) {
                                        viewModel.stopVideoRecording()
                                    } else {
                                        // For video, we need both camera and microphone permissions
                                        when {
                                            uiState.cameraPermissionStatus != CameraPermissionStatus.GRANTED -> cameraPermissionLauncher()
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
                        else if (uiState.isAnalyzing)
                            MaterialTheme.colorScheme.outline
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
                        onClick = {
                            println("CameraScreen: Gallery button clicked")
                            galleryLauncher()
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = !uiState.isAnalyzing
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier.size(32.dp)
                        )
                    }
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
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        SegmentedButtonWithCost(
            selected = captureMode == CaptureMode.PHOTO,
            onClick = { onModeChanged(CaptureMode.PHOTO) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
            text = "Photo",
            cost = AnalysisCost.PHOTO_ANALYSIS
        )

        Spacer(modifier = Modifier.width(16.dp))

        SegmentedButtonWithCost(
            selected = captureMode == CaptureMode.VIDEO,
            onClick = { onModeChanged(CaptureMode.VIDEO) },
            icon = { Icon(Icons.Default.Videocam, contentDescription = null) },
            text = "Video",
            cost = AnalysisCost.VIDEO_ANALYSIS
        )
    }
}

@Composable
fun SegmentedButtonWithCost(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    text: String,
    cost: Int
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
            // Cost indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.padding(start = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cost.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
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