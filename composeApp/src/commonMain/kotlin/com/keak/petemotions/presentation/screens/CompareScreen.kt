package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.keak.petemotions.data.api.ComparisonResult
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.loadImageFromBytes
import com.keak.petemotions.presentation.viewmodel.CompareViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.*

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
    viewModel: CompareViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.background(CompareDesignTokens.bgPage),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Results",
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
                            contentDescription = "Back",
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
                                            text = "Comparing...",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "Compare",
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
                                    text = "Clear",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CompareDesignTokens.bgPage)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Pet Selection Row
            item {
                PetSelectionRow(
                    petA = uiState.selectedPetA,
                    petB = uiState.selectedPetB,
                    availablePets = uiState.availablePets,
                    onPetASelected = viewModel::selectPetA,
                    onPetBSelected = viewModel::selectPetB
                )
            }

            // Show comparison content only if both pets are selected
            if (uiState.selectedPetA != null && uiState.selectedPetB != null) {
                if (uiState.isLoading) {
                    item {
                        LoadingShimmer()
                    }
                } else if (uiState.comparisonResult != null) {
                    // Emotion Frequency Section
                    item {
                        EmotionFrequencySection(
                            petA = uiState.selectedPetA!!,
                            petB = uiState.selectedPetB!!,
                            petAEmotions = uiState.petAEmotionStats ?: emptyMap(),
                            petBEmotions = uiState.petBEmotionStats ?: emptyMap()
                        )
                    }

                    // Comparison Results Section
                    item {
                        ComparisonResultsSection(
                            petA = uiState.selectedPetA!!,
                            petB = uiState.selectedPetB!!,
                            comparisonResult = uiState.comparisonResult!!
                        )
                    }
                }
            } else {
                item {
                    EmptyState()
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PetSelectionRow(
    petA: Pet?,
    petB: Pet?,
    availablePets: List<Pet>,
    onPetASelected: (Pet?) -> Unit,
    onPetBSelected: (Pet?) -> Unit
) {
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
            label = "Pet A"
        )

        // Pet B
        PetSelectionCard(
            modifier = Modifier.weight(1f),
            selectedPet = petB,
            availablePets = availablePets,
            onPetSelected = onPetBSelected,
            label = "Pet B"
        )
    }
}

@Composable
fun PetSelectionCard(
    modifier: Modifier = Modifier,
    selectedPet: Pet?,
    availablePets: List<Pet>,
    onPetSelected: (Pet?) -> Unit,
    label: String
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
                        contentDescription = "Select pet",
                        tint = CompareDesignTokens.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name
            Text(
                text = selectedPet?.name ?: "Select $label",
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
            text = { Text(stringResource(Res.string.compare_none)) },
            onClick = {
                onPetSelected(null)
                expanded = false
            }
        )
        availablePets.forEach { pet ->
            DropdownMenuItem(
                text = { Text(pet.name) },
                onClick = {
                    onPetSelected(pet)
                    expanded = false
                }
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
                text = "Emotion Frequency",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val emotions = listOf(
                "Happy" to CompareDesignTokens.barHappy,
                "Sad" to CompareDesignTokens.barSad,
                "Playful" to CompareDesignTokens.barPlayful,
                "Anxious" to CompareDesignTokens.barAnxious
            )

            emotions.forEach { (emotion, color) ->
                EmotionProgressRow(
                    emotion = emotion,
                    petAName = petA.name,
                    petBName = petB.name,
                    petAValue = petAEmotions[emotion.lowercase()] ?: 0f,
                    petBValue = petBEmotions[emotion.lowercase()] ?: 0f,
                    color = color
                )
                if (emotion != emotions.last().first) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun EmotionProgressRow(
    emotion: String,
    petAName: String,
    petBName: String,
    petAValue: Float,
    petBValue: Float,
    color: Color
) {
    Column {
        Text(
            text = emotion,
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
                color = color,
                emotion = emotion
            )

            // Pet B Progress Bar
            EmotionProgressBar(
                modifier = Modifier.weight(0.45f),
                petName = petBName,
                value = petBValue,
                color = color,
                emotion = emotion
            )
        }
    }
}

@Composable
fun EmotionProgressBar(
    modifier: Modifier = Modifier,
    petName: String,
    value: Float,
    color: Color,
    emotion: String
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
    petA: Pet,
    petB: Pet,
    comparisonResult: ComparisonResult
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compatibility Score Card
        CompatibilityScoreCard(
            compatibilityScore = comparisonResult.compatibilityScore,
            overview = comparisonResult.overview
        )

        // Shared Traits Card
        if (comparisonResult.sharedTraits.isNotEmpty()) {
            TraitsCard(
                title = "Shared Traits",
                traits = comparisonResult.sharedTraits,
                cardColor = Color.White,
                iconColor = CompareDesignTokens.barHappy
            )
        }

        // Key Differences Card
        if (comparisonResult.keyDifferences.isNotEmpty()) {
            TraitsCard(
                title = "Key Differences",
                traits = comparisonResult.keyDifferences,
                cardColor = Color.White,
                iconColor = CompareDesignTokens.barSad
            )
        }

        // Recommendations Card
        if (comparisonResult.recommendations.isNotEmpty()) {
            TraitsCard(
                title = "Recommendations",
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
                    text = "Compatibility Score",
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
                        text = "${(compatibilityScore * 100).toInt()}%",
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
                text = "Add two pets to compare.",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = CompareDesignTokens.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select two pets above to see their emotion analysis comparison.",
                style = MaterialTheme.typography.bodyMedium,
                color = CompareDesignTokens.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}