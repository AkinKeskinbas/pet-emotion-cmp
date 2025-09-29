package com.keak.petemotions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.presentation.navigation.PetEmotionsNavigation
import com.keak.petemotions.presentation.navigation.WelcomeRoute
import com.keak.petemotions.presentation.navigation.HomeRoute
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

        // Check if onboarding is completed
        val hasCompletedOnboarding by preferencesRepository.hasCompletedOnboarding().collectAsState(false)

        MaterialTheme {
            PetEmotionsNavigation(
                navController = navController,
                startDestination = if (hasCompletedOnboarding) HomeRoute else WelcomeRoute
            )
        }
    }
}