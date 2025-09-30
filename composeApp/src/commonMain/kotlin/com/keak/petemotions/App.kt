package com.keak.petemotions

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.repository.PreferencesRepository
import kotlinx.coroutines.launch
import com.keak.petemotions.presentation.navigation.PetEmotionsNavigation
import com.keak.petemotions.presentation.navigation.WelcomeRoute
import com.keak.petemotions.presentation.navigation.HomeRoute
import com.keak.petemotions.presentation.theme.PetEmotionsTheme
import com.keak.petemotions.utils.di.commonModule
import org.koin.compose.KoinMultiplatformApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration

// Platform-specific modules with context via expect/actual
expect fun getPlatformModuleWithContext(): org.koin.core.module.Module

@OptIn(KoinExperimentalAPI::class)
@Composable
fun App() {
    KoinMultiplatformApplication(
        config = KoinConfiguration {
            modules(commonModule, getPlatformModuleWithContext())
        }
    ) {
        val navController = rememberNavController()
        val preferencesRepository: PreferencesRepository = koinInject()
        val backendApiService: BackendApiService = koinInject()
        val coroutineScope = rememberCoroutineScope()

        // Check if onboarding is completed
        val hasCompletedOnboarding by preferencesRepository.hasCompletedOnboarding().collectAsState(false)

        // Perform registration on app startup with delay to ensure RevenueCat is configured
        LaunchedEffect(Unit) {
            coroutineScope.launch {
                try {
                    println("App: Waiting for RevenueCat initialization...")
                    // Give RevenueCat time to initialize
                    kotlinx.coroutines.delay(1000) // 1 second delay

                    println("App: Performing registration on startup...")
                    val result = backendApiService.register()
                    result.fold(
                        onSuccess = { response ->
                            println("App: Registration successful on startup")
                        },
                        onFailure = { error ->
                            println("App: Registration failed on startup: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    println("App: Registration error on startup: ${e.message}")
                }
            }
        }

        PetEmotionsTheme {
            PetEmotionsNavigation(
                navController = navController,
                startDestination = if (hasCompletedOnboarding) HomeRoute else WelcomeRoute
            )
        }
    }
}