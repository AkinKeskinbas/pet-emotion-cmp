package com.keak.petemotions.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.presentation.navigation.PermissionsRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    navController: NavController,
    preferencesRepository: PreferencesRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()

    val onboardingPages = listOf(
        OnboardingPage(
            icon = Icons.Default.Pets,
            title = "Welcome to PetEmotions",
            description = "Discover what your pet is really feeling through the power of AI-driven emotion analysis."
        ),
        OnboardingPage(
            icon = Icons.Default.CameraAlt,
            title = "Capture Their Moments",
            description = "Simply take a photo or record a video of your pet to start understanding their emotional state."
        ),
        OnboardingPage(
            icon = Icons.Default.Analytics,
            title = "Get Detailed Insights",
            description = "Receive comprehensive analysis of your pet's emotions, behavior patterns, and well-being over time."
        ),
        OnboardingPage(
            icon = Icons.Default.Favorite,
            title = "Build Stronger Bonds",
            description = "Use these insights to strengthen your relationship and provide the best care for your furry friends."
        )
    )

    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesRepository.setHasCompletedOnboarding(true)
                            navController.navigate(PermissionsRoute) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                ) {
                    Text("Skip")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Onboarding pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                }
                            )
                    )
                    if (index < onboardingPages.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (pagerState.currentPage == onboardingPages.size - 1) {
                    Arrangement.Center
                } else {
                    Arrangement.SpaceBetween
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < onboardingPages.size - 1) {
                    // Previous button (only show after first page)
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(80.dp))
                    }

                    // Next button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Next")
                    }
                } else {
                    // Get Started button (final page)
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                preferencesRepository.setHasCompletedOnboarding(true)
                                navController.navigate(PermissionsRoute) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        inclusive = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Card(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(60.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
        )
    }
}