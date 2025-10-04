package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.CompareHistoryRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.navigation.ResultDetailRoute
import com.keak.petemotions.presentation.navigation.CompareRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: com.keak.petemotions.presentation.viewmodel.HistoryViewModel = org.koin.compose.viewmodel.koinViewModel()
) {
    val analysisRecords by viewModel.filteredRecords.collectAsState()
    val compareHistoryRecords by viewModel.compareHistory.collectAsState()
    val pets by viewModel.pets.collectAsState()
    val selectedPetFilter by viewModel.selectedPetFilter.collectAsState()
    val selectedEmotionFilter by viewModel.selectedEmotionFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredRecords = analysisRecords

    val mediaStorage: MediaStorage = koinInject()
    val analysisRepository: com.keak.petemotions.data.repository.AnalysisRepository = koinInject()
    var petAvatarBitmaps by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }

    LaunchedEffect(pets) {
        if (pets.isNotEmpty()) {
            withContext(Dispatchers.Default) {
                val bitmaps = pets.map { pet ->
                    async {
                        val bitmap = pet.avatarPath?.takeIf { it.isNotBlank() }?.let { path ->
                            try {
                                val bytes = mediaStorage.load(path)
                                bytes?.let { loadImageFromBytes(it) }
                            } catch (e: Exception) {
                                println("HistoryScreen: Error loading avatar for pet ${pet.name}: ${e.message}")
                                null
                            }
                        }
                        pet.id to bitmap
                    }
                }.awaitAll()

                withContext(Dispatchers.Main) {
                    petAvatarBitmaps = bitmaps.toMap()
                }
            }
        }
    }

    val analysisImageBitmaps by produceState<Map<String, ImageBitmap?>>(initialValue = emptyMap(), key1 = filteredRecords) {
        withContext(Dispatchers.Default) {
            val bitmaps = filteredRecords.filter { it.mediaType == "image" }.map { record ->
                async {
                    val bitmap = try {
                        val imageBytes = analysisRepository.loadMediaFile(record.mediaPath)
                        imageBytes?.let { loadImageFromBytes(it) }
                    } catch (e: Exception) {
                        null
                    }
                    record.id to bitmap
                }
            }.awaitAll()

            value = bitmaps.toMap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.history_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.action_back))
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
                    onPetFilterChanged = { viewModel.setSelectedPetFilter(it) },
                    selectedEmotionFilter = selectedEmotionFilter,
                    onEmotionFilterChanged = { viewModel.setSelectedEmotionFilter(it) },
                    onClearFilters = {
                        viewModel.setSelectedPetFilter(null)
                        viewModel.setSelectedEmotionFilter(null)
                    }
                )
            }

            // Results count
            item {
                val countText = if (filteredRecords.size == 1) {
                    "${filteredRecords.size} ${stringResource(Res.string.history_analysis_count_single)}"
                } else {
                    "${filteredRecords.size} ${stringResource(Res.string.history_analysis_count_plural)}"
                }
                Text(
                    text = countText,
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

            if (compareHistoryRecords.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    Text(
                        text = stringResource(Res.string.history_compare_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(compareHistoryRecords) { record ->
                    val petAAvatar = record.petAId?.let { petAvatarBitmaps[it] }
                    val petBAvatar = record.petBId?.let { petAvatarBitmaps[it] }

                    CompareHistoryCard(
                        record = record,
                        petABitmap = petAAvatar,
                        petBBitmap = petBAvatar,
                        onClick = {
                            navController.navigate(CompareRoute(historyRecordId = record.id)) {
                                launchSingleTop = true
                            }
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
fun CompareHistoryCard(
    record: CompareHistoryRecord,
    petABitmap: ImageBitmap?,
    petBBitmap: ImageBitmap?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetAvatar(petABitmap, record.petAName)
                    Icon(Icons.Default.CompareArrows, contentDescription = null)
                    PetAvatar(petBBitmap, record.petBName)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${(record.compatibilityScore * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = stringResource(Res.string.compare_pair_heading, record.petAName, record.petBName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = record.overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            CompareTraitsSection(
                header = stringResource(Res.string.history_compare_shared_traits),
                traits = record.sharedTraits
            )

            CompareTraitsSection(
                header = stringResource(Res.string.history_compare_key_differences),
                traits = record.keyDifferences
            )

            CompareTraitsSection(
                header = stringResource(Res.string.history_compare_recommendations),
                traits = record.recommendations
            )

            Text(
                text = "${stringResource(Res.string.history_compare_timestamp)} ${formatTimestamp(record.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PetAvatar(bitmap: ImageBitmap?, petName: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = petName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = petName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareTraitsSection(header: String, traits: List<String>) {
    val validTraits = traits.filter { it.isNotBlank() }
    if (validTraits.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        validTraits.forEach { trait ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                    text = trait,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
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
                    text = stringResource(Res.string.history_filters),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedPetFilter != null || selectedEmotionFilter != null) {
                    TextButton(onClick = onClearFilters) {
                        Text(stringResource(Res.string.action_clear_all))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pet filter
            if (pets.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.history_pet_filter),
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
                            label = { Text(stringResource(Res.string.history_all_pets)) },
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
                text = stringResource(Res.string.history_emotion_filter),
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
                        label = { Text(stringResource(Res.string.history_all_emotions)) },
                        selected = selectedEmotionFilter == null
                    )
                }
                items(listOf("Happy", "Relaxed", "Playful", "Curious", "Alert", "Stressed", "Sad", "Excited")) { emotion ->
                    FilterChip(
                        onClick = { onEmotionFilterChanged(emotion) },
                        label = { Text(emotionDisplayName(emotion)) },
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
                        label = { Text(emotionDisplayName(analysisRecord.emotion)) },
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
                text = if (hasAnalyses) stringResource(Res.string.history_empty_title_filtered) else stringResource(Res.string.history_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasAnalyses) {
                    stringResource(Res.string.history_empty_description_filtered)
                } else {
                    stringResource(Res.string.history_empty_description)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasAnalyses) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToCamera) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.home_take_photo))
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

@Composable
private fun emotionDisplayName(emotion: String): String {
    val normalized = emotion.trim().lowercase()
    val resId = when (normalized) {
        "happy", "joy", "joyful" -> Res.string.emotion_happy
        "relaxed", "calm" -> Res.string.emotion_relaxed
        "playful", "play" -> Res.string.emotion_playful
        "curious" -> Res.string.emotion_curious
        "alert" -> Res.string.emotion_alert
        "stressed", "stress" -> Res.string.emotion_stressed
        "sad" -> Res.string.emotion_sad
        "excited" -> Res.string.emotion_excited
        "anxious", "anxiety" -> Res.string.emotion_anxious
        else -> null
    }

    return if (resId != null) {
        stringResource(resId)
    } else {
        emotion.ifBlank { "-" }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
