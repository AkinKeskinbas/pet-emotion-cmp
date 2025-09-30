package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.navigation.ResultDetailRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    analysisRepository: AnalysisRepository = koinInject(),
    petRepository: PetRepository = koinInject()
) {
    var analysisRecords by remember { mutableStateOf<List<AnalysisRecord>>(emptyList()) }
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var selectedPetFilter by remember { mutableStateOf<Pet?>(null) }
    var selectedEmotionFilter by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        launch {
            analysisRepository.getAllAnalysisRecords().collect { records ->
                analysisRecords = records
                isLoading = false
            }
        }
        launch {
            petRepository.getAllPets().collect { petList ->
                pets = petList
            }
        }
    }

    // Filter records based on selected filters and sort by newest first
    val filteredRecords = remember(analysisRecords, selectedPetFilter, selectedEmotionFilter) {
        analysisRecords
            .filter { record ->
                val petMatch = selectedPetFilter?.let { it.id == record.petId } ?: true
                val emotionMatch = selectedEmotionFilter?.let { it == record.emotion } ?: true
                petMatch && emotionMatch
            }
            .sortedByDescending { it.createdAt }
    }

    val mediaStorage: MediaStorage = koinInject()
    var petAvatarBitmaps by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }

    LaunchedEffect(pets) {
        if (pets.isNotEmpty()) {
            launch {
                val result = mutableMapOf<String, ImageBitmap?>()
                pets.forEach { pet ->
                    val bitmap = pet.avatarPath?.takeIf { it.isNotBlank() }?.let { path ->
                        try {
                            println("HistoryScreen: Loading avatar for pet ${pet.name} from path: $path")
                            val bytes = mediaStorage.load(path)
                            if (bytes != null) {
                                println("HistoryScreen: Loaded ${bytes.size} bytes for pet ${pet.name}")
                                val imageBitmap = loadImageFromBytes(bytes)
                                if (imageBitmap != null) {
                                    println("HistoryScreen: Successfully created ImageBitmap for pet ${pet.name}")
                                } else {
                                    println("HistoryScreen: Failed to create ImageBitmap for pet ${pet.name}")
                                }
                                imageBitmap
                            } else {
                                println("HistoryScreen: No bytes loaded for pet ${pet.name} from path: $path")
                                null
                            }
                        } catch (e: Exception) {
                            println("HistoryScreen: Error loading avatar for pet ${pet.name}: ${e.message}")
                            null
                        }
                    }
                    result[pet.id] = bitmap
                }
                petAvatarBitmaps = result
            }
        }
    }

    val analysisImageBitmaps by produceState<Map<String, ImageBitmap?>>(initialValue = emptyMap(), key1 = filteredRecords) {
        val result = mutableMapOf<String, ImageBitmap?>()
        filteredRecords.forEach { record ->
            if (record.mediaType == "image") {
                val imageBytes = try {
                    analysisRepository.loadMediaFile(record.mediaPath)
                } catch (_: Exception) {
                    null
                }
                val bitmap = imageBytes?.let { bytes -> loadImageFromBytes(bytes) }
                result[record.id] = bitmap
            }
        }
        value = result
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Filters
            item {
                FiltersRow(
                    pets = pets,
                    selectedPetFilter = selectedPetFilter,
                    onPetFilterChanged = { selectedPetFilter = it },
                    selectedEmotionFilter = selectedEmotionFilter,
                    onEmotionFilterChanged = { selectedEmotionFilter = it },
                    onClearFilters = {
                        selectedPetFilter = null
                        selectedEmotionFilter = null
                    }
                )
            }

            // Results count
            item {
                Text(
                    text = "${filteredRecords.size} ${if (filteredRecords.size == 1) "analysis" else "analyses"} found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredRecords.isEmpty()) {
                item {
                    EmptyHistoryState(
                        hasAnalyses = analysisRecords.isNotEmpty(),
                        onNavigateToCamera = {
                            navController.navigate(com.keak.petemotions.presentation.navigation.CameraRoute)
                        }
                    )
                }
            } else {
                items(filteredRecords) { record ->
                    val petForRecord = pets.find { it.id == record.petId }
                    val petAvatar = petForRecord?.let { petAvatarBitmaps[it.id] }
                    val analysisImage = analysisImageBitmaps[record.id]

                    // Priority: Pet avatar first, then analysis image
                    val previewBitmap = petAvatar ?: analysisImage

                    println("HistoryScreen: Record ${record.id} - Pet: ${petForRecord?.name}, HasPetAvatar: ${petAvatar != null}, HasAnalysisImage: ${analysisImage != null}")

                    HistoryItem(
                        analysisRecord = record,
                        petName = petForRecord?.name,
                        previewBitmap = previewBitmap,
                        onClick = {
                            navController.navigate(ResultDetailRoute(record.id))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FiltersRow(
    pets: List<Pet>,
    selectedPetFilter: Pet?,
    onPetFilterChanged: (Pet?) -> Unit,
    selectedEmotionFilter: String?,
    onEmotionFilterChanged: (String?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedPetFilter != null || selectedEmotionFilter != null) {
                    TextButton(onClick = onClearFilters) {
                        Text("Clear All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pet filter
            if (pets.isNotEmpty()) {
                Text(
                    text = "Pet",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onPetFilterChanged(null) },
                            label = { Text("All Pets") },
                            selected = selectedPetFilter == null
                        )
                    }
                    items(pets) { pet ->
                        FilterChip(
                            onClick = { onPetFilterChanged(pet) },
                            label = { Text(pet.name) },
                            selected = selectedPetFilter?.id == pet.id
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Emotion filter
            Text(
                text = "Emotion",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onEmotionFilterChanged(null) },
                        label = { Text("All") },
                        selected = selectedEmotionFilter == null
                    )
                }
                items(listOf("Happy", "Relaxed", "Playful", "Curious", "Alert", "Stressed", "Sad", "Excited")) { emotion ->
                    FilterChip(
                        onClick = { onEmotionFilterChanged(emotion) },
                        label = { Text(emotion) },
                        selected = selectedEmotionFilter == emotion
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    analysisRecord: AnalysisRecord,
    petName: String?,
    previewBitmap: ImageBitmap?,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Media preview or icon
            Card(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = petName?.let { "$it photo" } ?: "Pet photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (analysisRecord.mediaType == "video") {
                                Icons.Default.VideoLibrary
                            } else {
                                Icons.Default.Image
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Emotion chip and pet name
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(analysisRecord.emotion) },
                        leadingIcon = {
                            Text(getEmotionEmoji(analysisRecord.emotion))
                        }
                    )

                    petName?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Summary
                Text(
                    text = analysisRecord.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Timestamp and confidence
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatTimestamp(analysisRecord.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${(analysisRecord.confidence * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmptyHistoryState(
    hasAnalyses: Boolean,
    onNavigateToCamera: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (hasAnalyses) Icons.Default.FilterList else Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (hasAnalyses) "No results found" else "No analyses yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasAnalyses) {
                    "Try adjusting your filters to see more results."
                } else {
                    "Start by taking a photo or video of your pet to analyze their emotions."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasAnalyses) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToCamera) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo")
                }
            }
        }
    }
}

private fun getEmotionEmoji(emotion: String): String {
    return when (emotion.lowercase()) {
        "happy" -> "😊"
        "relaxed" -> "😌"
        "curious" -> "🤔"
        "alert" -> "👀"
        "stressed" -> "😰"
        "playful" -> "😸"
        "sad" -> "😢"
        "excited" -> "🤩"
        else -> "😐"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // TODO: Implement proper timestamp formatting
    return "Recently"
}
