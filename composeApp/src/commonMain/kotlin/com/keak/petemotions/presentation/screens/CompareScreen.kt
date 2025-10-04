package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.keak.petemotions.data.api.ComparisonResultWithCoinInfo
import com.keak.petemotions.data.model.CompareHistoryRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.viewmodel.CompareViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.action_back
import petemotions.composeapp.generated.resources.compare_action_clear
import petemotions.composeapp.generated.resources.compare_action_compare
import petemotions.composeapp.generated.resources.compare_comparing
import petemotions.composeapp.generated.resources.compare_compatibility_score
import petemotions.composeapp.generated.resources.compare_emotion_empty
import petemotions.composeapp.generated.resources.compare_emotion_frequency
import petemotions.composeapp.generated.resources.compare_empty_description
import petemotions.composeapp.generated.resources.compare_empty_title
import petemotions.composeapp.generated.resources.compare_pair_heading
import petemotions.composeapp.generated.resources.compare_prompt_button
import petemotions.composeapp.generated.resources.compare_prompt_description
import petemotions.composeapp.generated.resources.compare_prompt_title
import petemotions.composeapp.generated.resources.compare_section_key_differences
import petemotions.composeapp.generated.resources.compare_section_recommendations
import petemotions.composeapp.generated.resources.compare_section_shared_traits
import petemotions.composeapp.generated.resources.compare_select_pet
import petemotions.composeapp.generated.resources.compare_select_pet_content_description
import petemotions.composeapp.generated.resources.compare_title
import petemotions.composeapp.generated.resources.compare_error_dismiss
import petemotions.composeapp.generated.resources.compare_pet_a
import petemotions.composeapp.generated.resources.compare_pet_b
import petemotions.composeapp.generated.resources.emotion_alert
import petemotions.composeapp.generated.resources.emotion_anxious
import petemotions.composeapp.generated.resources.emotion_curious
import petemotions.composeapp.generated.resources.emotion_excited
import petemotions.composeapp.generated.resources.emotion_happy
import petemotions.composeapp.generated.resources.emotion_playful
import petemotions.composeapp.generated.resources.emotion_relaxed
import petemotions.composeapp.generated.resources.emotion_sad
import petemotions.composeapp.generated.resources.emotion_stressed
import kotlin.math.max

// Design tokens from compare.md
object CompareDesignTokens {
    val bgPage = Color(0xFFFFF8F2)
    val textPrimary = Color(0xFF111111)
    val textSecondary = Color(0xFF4A4A4A)
    val divider = Color(0xFFEAE6E2)
    val chipCardBg = Color(0xFFFFE9D6)
    val buttonPrimaryBg = Color(0xFFFFA447)
    val buttonPrimaryBgPressed = Color(0xFFFF8C2A)
    val buttonText = Color(0xFFFFFFFF)
    val barTrack = Color(0xFFD8D3CF)
    val barHappy = Color(0xFF4CC36B)
    val barSad = Color(0xFF6892F2)
    val barPlayful = Color(0xFFF2C64B)
    val barAnxious = Color(0xFFF27C7C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    navController: NavController,
    historyRecordId: String? = null,
    viewModel: CompareViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val selectedPetA = uiState.selectedPetA
    val selectedPetB = uiState.selectedPetB
    val comparisonResult = uiState.comparisonResult
    val historyRecord = uiState.historyRecord

    LaunchedEffect(historyRecordId) {
        historyRecordId?.let { viewModel.showComparisonFromHistory(it) }
    }

    LaunchedEffect(comparisonResult) {
        println("CompareScreen: comparisonResult updated -> ${comparisonResult?.compatibilityScore}")
    }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.background(CompareDesignTokens.bgPage),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.compare_title),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CompareDesignTokens.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                            tint = CompareDesignTokens.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CompareDesignTokens.bgPage
                )
            )
        },
        bottomBar = {
            if (uiState.selectedPetA != null && uiState.selectedPetB != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CompareDesignTokens.bgPage,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Compare button with coin indicator
                            Button(
                                onClick = { viewModel.startComparison() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CompareDesignTokens.buttonPrimaryBg,
                                    contentColor = CompareDesignTokens.buttonText
                                ),
                                shape = RoundedCornerShape(24.dp),
                                enabled = !uiState.isLoading
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = CompareDesignTokens.buttonText
                                        )
                                        Text(
                                            text = stringResource(Res.string.compare_comparing),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(Res.string.compare_action_compare),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Coin indicator
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color.White.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "🪙",
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "5",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CompareDesignTokens.buttonText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Clear button
                            OutlinedButton(
                                onClick = { viewModel.clearSelection() },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, CompareDesignTokens.buttonPrimaryBg),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CompareDesignTokens.buttonPrimaryBg
                                )
                            ) {
                                Text(
                                    text = stringResource(Res.string.compare_action_clear),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CompareDesignTokens.bgPage)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            uiState.error?.let { error ->
                ErrorCard(
                    message = error.message,
                    onDismiss = viewModel::dismissError
                )
            }

            PetSelectionRow(
                petA = selectedPetA,
                petB = selectedPetB,
                availablePets = uiState.availablePets,
                onPetASelected = viewModel::selectPetA,
                onPetBSelected = viewModel::selectPetB,
                fallbackPetAName = historyRecord?.petAName,
                fallbackPetBName = historyRecord?.petBName
            )

            when {
                comparisonResult != null -> {
                    if (selectedPetA != null && selectedPetB != null &&
                        !uiState.petAEmotionStats.isNullOrEmpty() && !uiState.petBEmotionStats.isNullOrEmpty()
                    ) {
                        EmotionFrequencySection(
                            petA = selectedPetA,
                            petB = selectedPetB,
                            petAEmotions = uiState.petAEmotionStats ?: emptyMap(),
                            petBEmotions = uiState.petBEmotionStats ?: emptyMap()
                        )
                    }

                    ComparisonResultsSection(
                        petA = selectedPetA,
                        petB = selectedPetB,
                        historyRecord = historyRecord,
                        comparisonResult = comparisonResult
                    )
                }

                uiState.isLoading -> LoadingShimmer()

                selectedPetA == null || selectedPetB == null -> EmptyState()

                else -> PromptToCompareAgain(onCompare = viewModel::startComparison)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PetSelectionRow(
    petA: Pet?,
    petB: Pet?,
    availablePets: List<Pet>,
    onPetASelected: (Pet?) -> Unit,
    onPetBSelected: (Pet?) -> Unit,
    fallbackPetAName: String? = null,
    fallbackPetBName: String? = null
) {
    val petALabel = stringResource(Res.string.compare_pet_a)
    val petBLabel = stringResource(Res.string.compare_pet_b)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Pet A
        PetSelectionCard(
            modifier = Modifier.weight(1f),
            selectedPet = petA,
            availablePets = availablePets,
            onPetSelected = onPetASelected,
            label = petALabel,
            fallbackName = fallbackPetAName,
            excludePet = petB // Exclude Pet B from Pet A's dropdown
        )

        // Pet B
        PetSelectionCard(
            modifier = Modifier.weight(1f),
            selectedPet = petB,
            availablePets = availablePets,
            onPetSelected = onPetBSelected,
            label = petBLabel,
            fallbackName = fallbackPetBName,
            excludePet = petA // Exclude Pet A from Pet B's dropdown
        )
    }
}

@Composable
fun PetSelectionCard(
    modifier: Modifier = Modifier,
    selectedPet: Pet?,
    availablePets: List<Pet>,
    onPetSelected: (Pet?) -> Unit,
    label: String,
    fallbackName: String? = null,
    excludePet: Pet? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val mediaStorage: MediaStorage = koinInject()
    var avatarBitmap by remember(selectedPet?.avatarPath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(selectedPet?.avatarPath) {
        avatarBitmap = selectedPet?.avatarPath?.let { path ->
            try {
                mediaStorage.load(path)?.let { bytes ->
                    loadImageFromBytes(bytes)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CompareDesignTokens.chipCardBg
        ),
        onClick = { expanded = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(CompareDesignTokens.barTrack),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap!!,
                        contentDescription = selectedPet?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (selectedPet != null) {
                    Text(
                        text = selectedPet.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CompareDesignTokens.textPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(Res.string.compare_select_pet_content_description),
                        tint = CompareDesignTokens.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            val placeholder = "${stringResource(Res.string.compare_select_pet)} $label"
            val displayName = selectedPet?.name ?: fallbackName ?: placeholder
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // Dropdown for pet selection
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("None") },
            onClick = {
                onPetSelected(null)
                expanded = false
            }
        )
        availablePets.forEach { pet ->
            val isExcluded = excludePet != null && pet.id == excludePet.id
            DropdownMenuItem(
                text = {
                    Text(
                        text = pet.name,
                        color = if (isExcluded) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                onClick = {
                    if (!isExcluded) {
                        onPetSelected(pet)
                        expanded = false
                    }
                },
                enabled = !isExcluded
            )
        }
    }
}

@Composable
fun EmotionFrequencySection(
    petA: Pet,
    petB: Pet,
    petAEmotions: Map<String, Float>,
    petBEmotions: Map<String, Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(Res.string.compare_emotion_frequency),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val combinedEmotions = (petAEmotions.keys + petBEmotions.keys)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            if (combinedEmotions.isEmpty()) {
                Text(
                    text = stringResource(Res.string.compare_emotion_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CompareDesignTokens.textSecondary
                )
            } else {
                val emotionEntries = combinedEmotions
                    .sortedByDescending { key ->
                        max(petAEmotions[key] ?: 0f, petBEmotions[key] ?: 0f)
                    }
                    .mapIndexed { index, emotionKey ->
                        val displayLabel = emotionDisplayName(emotionKey)
                        val color = emotionColorFor(emotionKey, index)
                        val petAValue = (petAEmotions[emotionKey] ?: 0f).coerceIn(0f, 1f)
                        val petBValue = (petBEmotions[emotionKey] ?: 0f).coerceIn(0f, 1f)
                        EmotionEntry(displayLabel, color, petAValue, petBValue)
                    }
                    .filter { entry -> entry.petAValue > 0f || entry.petBValue > 0f }

                if (emotionEntries.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.compare_emotion_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CompareDesignTokens.textSecondary
                    )
                } else {
                    emotionEntries.forEachIndexed { index, entry ->
                        EmotionProgressRow(
                            emotionLabel = entry.displayLabel,
                            petAName = petA.name,
                            petBName = petB.name,
                            petAValue = entry.petAValue,
                            petBValue = entry.petBValue,
                            color = entry.color
                        )

                        if (index != emotionEntries.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionProgressRow(
    emotionLabel: String,
    petAName: String,
    petBName: String,
    petAValue: Float,
    petBValue: Float,
    color: Color
) {
    Column {
        Text(
            text = emotionLabel,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            color = CompareDesignTokens.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pet A Progress Bar
            EmotionProgressBar(
                modifier = Modifier.weight(0.45f),
                petName = petAName,
                value = petAValue,
                color = color
            )

            // Pet B Progress Bar
            EmotionProgressBar(
                modifier = Modifier.weight(0.45f),
                petName = petBName,
                value = petBValue,
                color = color
            )
        }
    }
}

@Composable
fun EmotionProgressBar(
    modifier: Modifier = Modifier,
    petName: String,
    value: Float,
    color: Color
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = petName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = CompareDesignTokens.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = CompareDesignTokens.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    CompareDesignTokens.barTrack,
                    RoundedCornerShape(5.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .background(
                        color,
                        RoundedCornerShape(5.dp)
                    )
            )
        }
    }
}

@Composable
fun ComparisonResultsSection(
    petA: Pet?,
    petB: Pet?,
    historyRecord: CompareHistoryRecord?,
    comparisonResult: ComparisonResultWithCoinInfo
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val petAName = petA?.name ?: historyRecord?.petAName ?: stringResource(Res.string.compare_pet_a)
        val petBName = petB?.name ?: historyRecord?.petBName ?: stringResource(Res.string.compare_pet_b)

        Text(
            text = stringResource(Res.string.compare_pair_heading, petAName, petBName),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Compatibility Score Card
        CompatibilityScoreCard(
            compatibilityScore = comparisonResult.compatibilityScore,
            overview = comparisonResult.overview
        )

        // Shared Traits Card
        if (comparisonResult.sharedTraits.isNotEmpty()) {
            TraitsCard(
                title = stringResource(Res.string.compare_section_shared_traits),
                traits = comparisonResult.sharedTraits,
                cardColor = Color.White,
                iconColor = CompareDesignTokens.barHappy
            )
        }

        // Key Differences Card
        if (comparisonResult.keyDifferences.isNotEmpty()) {
            TraitsCard(
                title = stringResource(Res.string.compare_section_key_differences),
                traits = comparisonResult.keyDifferences,
                cardColor = Color.White,
                iconColor = CompareDesignTokens.barSad
            )
        }

        // Recommendations Card
        if (comparisonResult.recommendations.isNotEmpty()) {
            TraitsCard(
                title = stringResource(Res.string.compare_section_recommendations),
                traits = comparisonResult.recommendations,
                cardColor = CompareDesignTokens.chipCardBg,
                iconColor = CompareDesignTokens.buttonPrimaryBg
            )
        }
    }
}

@Composable
fun CompatibilityScoreCard(
    compatibilityScore: Double,
    overview: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.compare_compatibility_score),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CompareDesignTokens.textPrimary
                )

                // Score badge
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CompareDesignTokens.buttonPrimaryBg
                    )
                ) {
                    Text(
                        text = "${(compatibilityScore / 5.0 * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CompareDesignTokens.buttonText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = CompareDesignTokens.textSecondary,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun TraitsCard(
    title: String,
    traits: List<String>,
    cardColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(iconColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CompareDesignTokens.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            traits.forEach { trait ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = CompareDesignTokens.textSecondary,
                        modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                    )

                    Text(
                        text = trait,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = CompareDesignTokens.textSecondary,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (trait != traits.last()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun LoadingShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .background(
                            CompareDesignTokens.barTrack,
                            RoundedCornerShape(5.dp)
                        )
                )
                if (it < 3) Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
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

private val fallbackEmotionColors = listOf(
    CompareDesignTokens.barPlayful,
    CompareDesignTokens.barAnxious,
    CompareDesignTokens.barSad,
    CompareDesignTokens.buttonPrimaryBg
)

private fun emotionColorFor(emotion: String, index: Int): Color {
    val normalized = emotion.trim().lowercase()
    return when (normalized) {
        "happy", "joy", "joyful" -> CompareDesignTokens.barHappy
        "sad" -> CompareDesignTokens.barSad
        "playful", "play" -> CompareDesignTokens.barPlayful
        "anxious", "anxiety" -> CompareDesignTokens.barAnxious
        else -> fallbackEmotionColors[index % fallbackEmotionColors.size]
    }
}

private data class EmotionEntry(
    val displayLabel: String,
    val color: Color,
    val petAValue: Float,
    val petBValue: Float
)

@Composable
fun PromptToCompareAgain(onCompare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CompareDesignTokens.chipCardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.compare_prompt_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.compare_prompt_description),
                style = MaterialTheme.typography.bodyMedium,
                color = CompareDesignTokens.textSecondary,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCompare,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompareDesignTokens.buttonPrimaryBg,
                    contentColor = CompareDesignTokens.buttonText
                )
            ) {
                Text(
                    text = stringResource(Res.string.compare_prompt_button),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFB42318)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A271A),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.compare_error_dismiss),
                    tint = Color(0xFF7A271A)
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CompareDesignTokens.chipCardBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CompareDesignTokens.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.compare_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.compare_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = CompareDesignTokens.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
