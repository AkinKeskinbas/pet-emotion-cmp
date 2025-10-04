package com.keak.petemotions.presentation.screens

// TODO: RevenueCat Paywall - Currently using Adapty instead
// This implementation is kept for future reference
// To use this, switch back in PaywallScreen.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.model.CoinPackage
import com.keak.petemotions.data.service.CoinPackageOption
import com.keak.petemotions.data.service.CoinService
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val coinService: CoinService = koinInject()
    val backendApiService: BackendApiService = koinInject()

    var availablePackages by remember { mutableStateOf<List<CoinPackageOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isPurchasing by remember { mutableStateOf(false) }
    var purchasingPackageId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Load packages from RevenueCat
    LaunchedEffect(Unit) {
        isLoading = true
        coinService.getAvailablePackages().fold(
            onSuccess = { packages ->
                availablePackages = packages
                println("PaywallBottomSheet: Loaded ${packages.size} packages")
            },
            onFailure = { exception ->
                error = "Failed to load packages: ${exception.message}"
                println("PaywallBottomSheet: Failed to load packages: ${exception.message}")
            }
        )
        isLoading = false
    }

    // Purchase function with receipt validation
    fun purchasePackage(option: CoinPackageOption) {
        val productId = option.coinPackage.revenueCatProductId

        isPurchasing = true
        purchasingPackageId = productId
        error = null

        coroutineScope.launch {
            try {
                println("=== PURCHASE STARTING ===")
                println("Product ID: $productId")
                println("Product Price: ${option.coinPackage.price}")
                println("========================")

                val purchaseResult = coinService.purchaseCoinsWithProductId(productId)

                purchaseResult.fold(
                    onSuccess = { result ->
                        println("PaywallBottomSheet: RevenueCat purchase successful, validating with backend...")

                        val validationResult = backendApiService.validatePurchase(
                            platform = result.platform,
                            receipt = result.receipt ?: "",
                            transactionId = result.transactionId,
                            productId = result.productId
                        )

                        validationResult.fold(
                            onSuccess = { validation ->
                                if (validation.success) {
                                    println("PaywallBottomSheet: Backend validation successful - ${validation.coinsAdded} coins added")
                                    onPurchaseSuccess()
                                    onDismiss()
                                } else {
                                    error = validation.error ?: "Purchase validation failed"
                                }
                            },
                            onFailure = { exception ->
                                error = "Validation failed: ${exception.message}"
                                println("PaywallBottomSheet: Backend validation failed: ${exception.message}")
                            }
                        )
                    },
                    onFailure = { exception ->
                        error = "Purchase failed: ${exception.message}"
                        println("PaywallBottomSheet: RevenueCat purchase failed: ${exception.message}")
                        println("PaywallBottomSheet: Exception details: ${exception}")
                        exception.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                error = "Purchase error: ${e.message}"
                println("PaywallBottomSheet: Purchase error: ${e.message}")
            } finally {
                isPurchasing = false
                purchasingPackageId = null
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error as snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                duration = SnackbarDuration.Short
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header with close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Hero section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "PREMIUM",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main title
                Text(
                    text = "Get More Coins",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Analyze unlimited pet emotions with our coin packages",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Features section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "What you get:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                FeatureItem(
                    icon = "📸",
                    title = "Photo Analysis",
                    description = "1 coin per photo analysis"
                )

                FeatureItem(
                    icon = "🎥",
                    title = "Video Analysis",
                    description = "5 coins per video analysis"
                )

                FeatureItem(
                    icon = "🔄",
                    title = "Pet Comparison",
                    description = "5 coins per comparison"
                )

                FeatureItem(
                    icon = "📊",
                    title = "Advanced Insights",
                    description = "1 coins for detailed analytics"
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Coin packages
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Choose your package:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show loading or error state
                when {
                    isLoading -> {
                        repeat(3) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            if (it < 2) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    error != null -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error ?: "Failed to load packages",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        // Dynamic coin packages from product IDs
                        availablePackages.forEachIndexed { index, packageOption ->
                            val coinPackage = packageOption.coinPackage
                            CoinPackageCard(
                                coinPackage = coinPackage,
                                isPopular = coinPackage.isPopular,
                                isLoading = isPurchasing && purchasingPackageId == coinPackage.revenueCatProductId,
                                enabled = !isPurchasing, // Always enabled since we use direct product IDs
                                onClick = { purchasePackage(packageOption) }
                            )
                            if (index < availablePackages.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Footer
            Text(
                text = "Secure payment • Cancel anytime • No subscription",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        // Snackbar host for error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CoinPackageCard(
    coinPackage: CoinPackage,
    isPopular: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!enabled) Modifier.alpha(0.6f) else Modifier),
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (isPopular) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isPopular) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Badges at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, end = 12.dp, start = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // Bonus badge - only show if we have actual coin data
                if (coinPackage.bonusPercentage > 0 && coinPackage.coinAmount > 0) {
                    val bonusAmount = coinPackage.coinAmount * coinPackage.bonusPercentage / 100
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "+$bonusAmount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (isPopular) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                // Popular badge
                if (isPopular) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "POPULAR",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🪙",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (coinPackage.coinAmount > 0) {
                                "${coinPackage.coinAmount}"
                            } else {
                                // Show the product identifier instead if no coin amount extracted
                                coinPackage.revenueCatProductId.replace("pet_emotions_", "").replace("_", " ").replaceFirstChar { it.uppercase() }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                }

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Text(
                        text = coinPackage.price,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
