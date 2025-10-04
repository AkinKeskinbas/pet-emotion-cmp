package com.keak.petemotions.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.keak.petemotions.presentation.navigation.HomeRoute
import com.keak.petemotions.presentation.navigation.OnboardingRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import petemotions.composeapp.generated.resources.Res
import petemotions.composeapp.generated.resources.app_name
import petemotions.composeapp.generated.resources.pawpaw
import petemotions.composeapp.generated.resources.splash_loading_error
import petemotions.composeapp.generated.resources.splash_try_again
import com.keak.petemotions.presentation.viewmodel.SplashDestination
import com.keak.petemotions.presentation.viewmodel.SplashViewModel
import com.keak.petemotions.presentation.viewmodel.SplashUiState
import kotlinx.coroutines.delay
import petemotions.composeapp.generated.resources.splash_tagline

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var hasMinDisplayTimePassed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1500)
        hasMinDisplayTimePassed = true
    }

    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    // Animation for paw icon
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Navigate after checking onboarding status
    LaunchedEffect(uiState.destination, hasMinDisplayTimePassed) {
        if (!hasMinDisplayTimePassed) return@LaunchedEffect

        when (uiState.destination) {
            SplashDestination.Onboarding -> {
                navController.navigate(OnboardingRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                viewModel.onNavigationHandled()
            }

            SplashDestination.Home -> {
                navController.navigate(HomeRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                viewModel.onNavigationHandled()
            }

            null -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value }
            ) {
                // Animated paw icon
                Image(
                    painter = painterResource(Res.drawable.pawpaw),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App name
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline
                Text(
                    text = stringResource(Res.string.splash_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = uiState.errorMessage == null,
                    enter = fadeIn(animationSpec = tween(500)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                AnimatedVisibility(
                    visible = uiState.errorMessage != null,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.splash_loading_error, uiState.errorMessage!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.evaluateStartupState() }) {
                            Text(text = stringResource(Res.string.splash_try_again))
                        }
                    }
                }
            }
        }
    }
}
