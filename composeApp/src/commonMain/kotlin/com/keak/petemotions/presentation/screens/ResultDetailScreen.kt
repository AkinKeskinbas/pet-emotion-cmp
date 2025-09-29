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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.platform.createPlaceholderImage
import com.keak.petemotions.platform.loadImageFromBytes
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultDetailScreen(
    navController: NavController,
    analysisRecordId: String,
    analysisRepository: AnalysisRepository = koinInject()
) {
    var analysisRecord by remember { mutableStateOf<AnalysisRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(analysisRecordId) {
        try {
            analysisRecord = analysisRepository.getAnalysisRecordById(analysisRecordId)
            if (analysisRecord == null) {
                error = "Analysis record not found"
            }
        } catch (e: Exception) {
            error = "Failed to load analysis: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analysis Result",
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
                    IconButton(onClick = { /* TODO: Implement share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* TODO: Implement retry */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry Analysis")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigateUp() }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            analysisRecord != null -> {
                AnalysisResultContent(
                    modifier = Modifier.padding(paddingValues),
                    analysisRecord = analysisRecord!!
                )
            }
        }
    }
}

@Composable
fun AnalysisResultContent(
    modifier: Modifier = Modifier,
    analysisRecord: AnalysisRecord
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Media preview
        MediaPreviewCard(
            mediaPath = analysisRecord.mediaPath,
            mediaType = analysisRecord.mediaType,
            analysisRepository = koinInject()
        )

        // Emotion result
        EmotionResultCard(analysisRecord.emotion, analysisRecord.confidence)

        // Summary
        SummaryCard(analysisRecord.summary)

        // Detailed analysis
        DetailedAnalysisCard(analysisRecord.detailsBody)

        // Tags
        if (analysisRecord.tags.isNotEmpty()) {
            TagsCard(analysisRecord.tags)
        }

        // Metadata
        MetadataCard(analysisRecord)
    }
}

@Composable
fun MediaPreviewCard(
    mediaPath: String,
    mediaType: String,
    analysisRepository: AnalysisRepository
) {
    var mediaBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaPath) {
        try {
            mediaBytes = analysisRepository.loadMediaFile(mediaPath)
            if (mediaBytes == null) {
                error = "Failed to load media"
            }
        } catch (e: Exception) {
            error = "Error loading media: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                error != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                mediaBytes != null && mediaType == "image" -> {
                    val imageBitmap = remember(mediaBytes) {
                        loadImageFromBytes(mediaBytes!!) ?: createPlaceholderImage()
                    }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                mediaType == "video" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Video Preview (Coming Soon)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    PhotoPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun PhotoPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Photo Preview",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun EmotionResultCard(emotion: String, confidence: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getEmotionEmoji(emotion),
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = emotion,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(confidence * 100).toInt()}% confidence",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
            )
        }
    }
}

@Composable
fun DetailedAnalysisCard(detailsBody: String) {
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
                    Icons.Default.Analytics,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Detailed Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Parse details body and display sections
            val sections = parseDetailsBody(detailsBody)
            sections.forEach { (title, content) ->
                AnalysisSection(title, content)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AnalysisSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}

@Composable
fun TagsCard(tags: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }
        }
    }
}

@Composable
fun MetadataCard(analysisRecord: AnalysisRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Analysis Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            MetadataRow("Analysis ID", analysisRecord.id.take(8) + "...")
            MetadataRow("Media Type", analysisRecord.mediaType.uppercase())
            MetadataRow("Analyzed", formatTimestamp(analysisRecord.createdAt))

            analysisRecord.petId?.let {
                MetadataRow("Pet ID", it.take(8) + "...")
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
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

private fun parseDetailsBody(detailsBody: String): List<Pair<String, String>> {
    // Simple parsing - split by double newlines and try to extract sections
    val sections = mutableListOf<Pair<String, String>>()

    val parts = detailsBody.split("\n\n")
    parts.forEach { part ->
        val lines = part.trim().split("\n")
        if (lines.isNotEmpty()) {
            val title = when {
                part.contains("body", ignoreCase = true) || part.contains("posture", ignoreCase = true) -> "Body Language"
                part.contains("vocal", ignoreCase = true) || part.contains("sound", ignoreCase = true) -> "Vocalization"
                part.contains("context", ignoreCase = true) || part.contains("environment", ignoreCase = true) -> "Context"
                else -> "Observation"
            }
            sections.add(title to part.trim())
        }
    }

    if (sections.isEmpty()) {
        sections.add("Analysis" to detailsBody)
    }

    return sections
}

private fun formatTimestamp(timestamp: Long): String {
    // TODO: Implement proper timestamp formatting with kotlinx-datetime
    return "Recently"
}