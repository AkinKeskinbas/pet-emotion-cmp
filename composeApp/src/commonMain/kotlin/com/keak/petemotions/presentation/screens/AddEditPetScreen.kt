package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.viewmodel.AddEditPetViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.pet_name_label
import petemotions.composeapp.generated.resources.pet_name_placeholder
import petemotions.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPetScreen(
    navController: NavController,
    petId: String? = null,
    viewModel: AddEditPetViewModel = koinViewModel { parametersOf(petId) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val galleryLauncher = rememberGalleryLauncher { imageBytes ->
        viewModel.onAvatarSelected(imageBytes)
    }

    LaunchedEffect(uiState.error) {
        val errorMessage = uiState.error ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
        } catch (_: Exception) {
            // Ignore snackbar exceptions to avoid crashing the screen
        }
        viewModel.dismissError()
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            navController.navigateUp()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (petId == null) stringResource(Res.string.add_pet_title) else stringResource(Res.string.edit_pet_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.savePet() },
                        enabled = uiState.name.isNotBlank() && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(Res.string.action_save))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar section
            PetAvatarSection(
                avatarPath = uiState.avatarPath,
                petName = uiState.name,
                isSelecting = uiState.isSelectingPhoto,
                onSelectAvatar = {
                    galleryLauncher()
                }
            )

            // Name field
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(Res.string.pet_name_label)) },
                placeholder = { Text(stringResource(Res.string.pet_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Pets, contentDescription = null)
                },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                singleLine = true
            )

            // Breed field
            OutlinedTextField(
                value = uiState.breed ?: "",
                onValueChange = viewModel::updateBreed,
                label = { Text(stringResource(Res.string.pet_breed_label)) },
                placeholder = { Text(stringResource(Res.string.pet_breed_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Category, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                ),
                singleLine = true
            )

            // Age field
            OutlinedTextField(
                value = uiState.age?.toString() ?: "",
                onValueChange = { value ->
                    value.toIntOrNull()?.let { viewModel.updateAge(it) }
                        ?: if (value.isEmpty()) { viewModel.updateAge(null) } else { /* ignore invalid input */ }
                },
                label = { Text(stringResource(Res.string.pet_age_label)) },
                placeholder = { Text(stringResource(Res.string.pet_age_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            // Notes field
            OutlinedTextField(
                value = uiState.notes ?: "",
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(Res.string.pet_notes_label)) },
                placeholder = { Text(stringResource(Res.string.pet_notes_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                leadingIcon = {
                    Icon(Icons.Default.Note, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                )
            )

            // Delete button for existing pets
            if (petId != null) {
                Spacer(modifier = Modifier.height(16.dp))

                var showDeleteDialog by remember { mutableStateOf(false) }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.my_pets_delete_pet))
                }

                // Delete confirmation dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringResource(Res.string.pet_delete_dialog_title)) },
                        text = {
                            Text("${stringResource(Res.string.pet_delete_dialog_message_with_name)} ${uiState.name}${stringResource(Res.string.pet_delete_with_records_question)}")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePet()
                                    showDeleteDialog = false
                                }
                            ) {
                                Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringResource(Res.string.action_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PetAvatarSection(
    avatarPath: String?,
    petName: String,
    isSelecting: Boolean = false,
    onSelectAvatar: () -> Unit
) {
    val mediaStorage: MediaStorage = koinInject()
    val avatarBitmap by produceState<ImageBitmap?>(initialValue = null, avatarPath, isSelecting) {
        if (isSelecting) {
            value = null
            return@produceState
        }

        value = if (avatarPath.isNullOrBlank()) {
            null
        } else {
            runCatching { mediaStorage.load(avatarPath) }
                .getOrNull()
                ?.let { bytes ->
                    loadImageFromBytes(bytes)
                }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            onClick = onSelectAvatar,
            enabled = !isSelecting,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isSelecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap!!,
                        contentDescription = stringResource(Res.string.pet_photo_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (avatarPath != null) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (petName.isNotBlank()) {
                    Text(
                        text = petName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.pet_add_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onSelectAvatar, enabled = !isSelecting) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(Res.string.pet_change_photo))
        }
    }
}
