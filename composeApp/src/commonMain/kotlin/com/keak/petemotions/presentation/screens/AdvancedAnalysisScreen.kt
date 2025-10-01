package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.jetbrains.compose.resources.stringResource
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalysisScreen(
    navController: NavController,
    analysisRepository: AnalysisRepository = koinInject(),
    petRepository: PetRepository = koinInject()
) {
    var analysisRecords by remember { mutableStateOf<List<AnalysisRecord>>(emptyList()) }
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedTimeRange by remember { mutableStateOf(TimeRange.WEEK) }
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

    // Filter records based on selected pet and time range
    val filteredRecords = remember(analysisRecords, selectedPet, selectedTimeRange) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val timeFilterMs = when (selectedTimeRange) {
            TimeRange.WEEK -> 7 * 24 * 60 * 60 * 1000L
            TimeRange.MONTH -> 30 * 24 * 60 * 60 * 1000L
            TimeRange.THREE_MONTHS -> 90 * 24 * 60 * 60 * 1000L
            TimeRange.YEAR -> 365 * 24 * 60 * 60 * 1000L
            TimeRange.ALL -> Long.MAX_VALUE
        }

        analysisRecords.filter { record ->
            val petMatch = selectedPet?.let { it.id == record.petId } ?: true
            val timeMatch = (now - record.createdAt) <= timeFilterMs
            petMatch && timeMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Advanced Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
//                    IconButton(onClick = { /* TODO: Export functionality */ }) {
//                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
//                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Filters
                item {
                    AnalysisFilters(
                        pets = pets,
                        selectedPet = selectedPet,
                        onPetSelected = { selectedPet = it },
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { selectedTimeRange = it }
                    )
                }

                // Overview Stats
                item {
                    OverviewStatsCard(filteredRecords)
                }

                // Emotion Trends Chart
                item {
                    EmotionTrendsCard(filteredRecords)
                }

                // Emotion Distribution
                item {
                    EmotionDistributionCard(filteredRecords)
                }

                // Pet Comparison (if multiple pets)
                if (pets.size > 1 && selectedPet == null) {
                    item {
                        PetComparisonCard(filteredRecords, pets)
                    }
                }

                // Recent Insights
                item {
                    RecentInsightsCard(filteredRecords.take(5))
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

enum class TimeRange(val displayName: String) {
    WEEK("Last Week"),
    MONTH("Last Month"),
    THREE_MONTHS("Last 3 Months"),
    YEAR("Last Year"),
    ALL("All Time")
}

@Composable
fun AnalysisFilters(
    pets: List<Pet>,
    selectedPet: Pet?,
    onPetSelected: (Pet?) -> Unit,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

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
                            onClick = { onPetSelected(null) },
                            label = { Text(stringResource(Res.string.advanced_all_pets)) },
                            selected = selectedPet == null
                        )
                    }
                    items(pets) { pet ->
                        FilterChip(
                            onClick = { onPetSelected(pet) },
                            label = { Text(pet.name) },
                            selected = selectedPet?.id == pet.id
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Time range filter
            Text(
                text = "Time Range",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimeRange.values()) { timeRange ->
                    FilterChip(
                        onClick = { onTimeRangeSelected(timeRange) },
                        label = { Text(timeRange.displayName) },
                        selected = selectedTimeRange == timeRange
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewStatsCard(records: List<AnalysisRecord>) {
    val totalAnalyses = records.size
    val averageConfidence = if (records.isNotEmpty()) {
        (records.sumOf { it.confidence } / records.size * 100).toInt()
    } else 0

    val mostCommonEmotion = records
        .groupBy { it.emotion }
        .maxByOrNull { it.value.size }
        ?.key ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = totalAnalyses.toString(),
                    label = "Total Analyses"
                )
                StatItem(
                    value = "$averageConfidence%",
                    label = "Avg Confidence"
                )
                StatItem(
                    value = mostCommonEmotion,
                    label = "Most Common"
                )
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EmotionTrendsCard(records: List<AnalysisRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emotion Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TODO: Implement actual chart using Vico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Emotion trends chart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Coming soon with Vico charts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmotionDistributionCard(records: List<AnalysisRecord>) {
    val emotionCounts = records.groupBy { it.emotion }.mapValues { it.value.size }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emotion Distribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (emotionCounts.isNotEmpty()) {
                emotionCounts.entries.sortedByDescending { it.value }.forEach { (emotion, count) ->
                    val percentage = (count.toFloat() / records.size * 100).toInt()
                    EmotionDistributionItem(
                        emotion = emotion,
                        count = count,
                        percentage = percentage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmotionDistributionItem(
    emotion: String,
    count: Int,
    percentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = getEmotionEmoji(emotion),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = emotion,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count ($percentage%)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PetComparisonCard(records: List<AnalysisRecord>, pets: List<Pet>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Compare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pet Comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            pets.forEach { pet ->
                val petRecords = records.filter { it.petId == pet.id }
                val mostCommonEmotion = petRecords
                    .groupBy { it.emotion }
                    .maxByOrNull { it.value.size }
                    ?.key ?: "N/A"

                PetComparisonItem(
                    petName = pet.name,
                    analysisCount = petRecords.size,
                    mostCommonEmotion = mostCommonEmotion
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PetComparisonItem(
    petName: String,
    analysisCount: Int,
    mostCommonEmotion: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = petName.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .size(32.dp)
                .wrapContentSize(Alignment.Center)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = petName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$analysisCount analyses • Mostly $mostCommonEmotion",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun RecentInsightsCard(recentRecords: List<AnalysisRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recent Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentRecords.isEmpty()) {
                Text(
                    text = "No recent analyses available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                recentRecords.forEach { record ->
                    InsightItem(
                        emotion = record.emotion,
                        summary = record.summary,
                        timestamp = record.createdAt
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun InsightItem(
    emotion: String,
    summary: String,
    timestamp: Long
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = getEmotionEmoji(emotion),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                text = formatTimestamp(timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
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