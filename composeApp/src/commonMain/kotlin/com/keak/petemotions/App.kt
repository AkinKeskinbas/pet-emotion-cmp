package com.keak.petemotions

import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.keak.petemotions.presentation.navigation.PetEmotionsNavigation
import com.keak.petemotions.presentation.theme.PetEmotionsTheme
import com.keak.petemotions.utils.di.commonModule
import org.koin.compose.KoinMultiplatformApplication
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

        // Note: Backend registration and RevenueCat initialization are now handled
        // platform-specifically in PetEmotionsApplication.kt (Android) and IOSApp.kt (iOS)
        // before the UI is shown, ensuring all services are ready when the app starts

        PetEmotionsTheme {
            PetEmotionsNavigation(
                navController = navController
            )
        }
    }
}
