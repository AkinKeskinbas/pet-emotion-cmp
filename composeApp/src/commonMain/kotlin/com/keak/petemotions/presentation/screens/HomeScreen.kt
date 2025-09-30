package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.presentation.navigation.*
import com.keak.petemotions.presentation.viewmodel.HomeViewModel
import com.keak.petemotions.presentation.viewmodel.HomeUiState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PetEmotions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Coin Balance Display
                    CoinBalanceChip(
                        coinBalance = uiState.coinBalance.balance,
                        onClick = { navController.navigate(PaywallRoute) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { navController.navigate(MyPetsRoute) }) {
                        Icon(Icons.Default.Pets, contentDescription = "My Pets")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
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

            // Coin Balance Card
            item {
                CoinBalanceCard(
                    coinBalance = uiState.coinBalance.balance,
                    onClick = { navController.navigate(PaywallRoute) }
                )
            }

            // Premium banner or shortcuts
            item {
                if (uiState.userPrefs.isPremium) {
                    PremiumShortcuts(navController)
                } else {
                    FreeTierBanner(navController)
                }
            }

            // Quick Actions
            item {
                QuickActions(navController, uiState.userPrefs.isPremium)
            }

            // Recent Analyses
            if (uiState.recentAnalyses.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Analyses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.recentAnalyses) { analysis ->
                    RecentAnalysisCard(
                        analysis = analysis,
                        onClick = {
                            navController.navigate(ResultDetailRoute(analysis.id))
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Handle error
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or dialog
            viewModel.dismissError()
        }
    }
}

@Composable
fun PremiumShortcuts(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Premium Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShortcutButton(
                    icon = Icons.Default.CameraAlt,
                    text = "Camera",
                    onClick = { navController.navigate(CameraRoute) }
                )
                ShortcutButton(
                    icon = Icons.Default.History,
                    text = "History",
                    onClick = { navController.navigate(HistoryRoute) }
                )
                ShortcutButton(
                    icon = Icons.Default.Analytics,
                    text = "Analysis",
                    onClick = { navController.navigate(AdvancedAnalysisRoute) }
                )
            }
        }
    }
}

@Composable
fun FreeTierBanner(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Start analyzing your pet's emotions!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { navController.navigate(CameraRoute) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take Photo")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { navController.navigate(PaywallRoute) }
            ) {
                Text("Unlock Unlimited Analysis")
            }
        }
    }
}

@Composable
fun QuickActions(navController: NavController, isPremium: Boolean) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CameraAlt,
            title = "Camera",
            subtitle = "Capture moment",
            onClick = { navController.navigate(CameraRoute) }
        )
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.History,
            title = "History",
            subtitle = "View past analyses",
            onClick = { navController.navigate(HistoryRoute) }
        )
    }

    if (isPremium) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Analytics,
                title = "Analytics",
                subtitle = "Advanced insights",
                onClick = { navController.navigate(AdvancedAnalysisRoute) }
            )
            ActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Pets,
                title = "My Pets",
                subtitle = "Manage pets",
                onClick = { navController.navigate(MyPetsRoute) }
            )
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ShortcutButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, contentDescription = text)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun RecentAnalysisCard(
    analysis: com.keak.petemotions.data.model.AnalysisRecord,
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
            // Emotion badge
            AssistChip(
                onClick = { },
                label = { Text(analysis.emotion) },
                leadingIcon = {
                    Text(getEmotionEmoji(analysis.emotion))
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = analysis.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Text(
                    text = formatTimestamp(analysis.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details"
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = true,
            onClick = { navController.navigate(HomeRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Compare, contentDescription = "Compare") },
            label = { Text("Compare") },
            selected = false,
            onClick = { navController.navigate(CompareRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("History") },
            selected = false,
            onClick = { navController.navigate(HistoryRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
            label = { Text("Analytics") },
            selected = false,
            onClick = { navController.navigate(AdvancedAnalysisRoute) }
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

@Composable
fun CoinBalanceChip(coinBalance: Int, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🪙",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = coinBalance.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun CoinBalanceCard(coinBalance: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Coins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Use coins to analyze your pet's emotions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🪙",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = coinBalance.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    // TODO: Implement proper timestamp formatting
    return "Recently"
}