package com.keak.petemotions.presentation.screens

import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.presentation.navigation.HomeRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PaywallScreen(
    navController: NavController,
    preferencesRepository: PreferencesRepository = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()

    PaywallBottomSheet(
        onDismiss = {
            coroutineScope.launch {
                preferencesRepository.setHasCompletedOnboarding(true)
                navController.navigate(HomeRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        },
        onPurchaseSuccess = {
            coroutineScope.launch {
                preferencesRepository.setHasCompletedOnboarding(true)
                navController.navigate(HomeRoute) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
            }
        }
    )
}