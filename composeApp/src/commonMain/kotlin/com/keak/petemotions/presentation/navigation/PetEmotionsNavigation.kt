package com.keak.petemotions.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.keak.petemotions.presentation.screens.*

@Composable
fun PetEmotionsNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = SplashRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<SplashRoute> {
            SplashScreen(navController)
        }

        composable<OnboardingRoute> {
            WelcomeScreen(navController)
        }

        composable<WelcomeRoute> {
            WelcomeScreen(navController)
        }

        composable<HomeRoute> {
            HomeScreen(navController)
        }

        composable<CameraRoute> {
            CameraScreen(navController)
        }

        composable<ResultDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ResultDetailRoute>()
            ResultDetailScreen(navController, route.analysisRecordId)
        }

        composable<HistoryRoute> {
            HistoryScreen(navController)
        }

        composable<MyPetsRoute> {
            MyPetsScreen(navController)
        }

        composable<AddEditPetRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AddEditPetRoute>()
            AddEditPetScreen(navController, route.petId)
        }

        composable<AdvancedAnalysisRoute> {
            AdvancedAnalysisScreen(navController)
        }

        composable<PaywallRoute> {
            PaywallRevenueCat(navController)
           // PaywallScreen(navController)
        }

        composable<PermissionsRoute> {
            PermissionsScreen(
                navController = navController,
                onContinue = {
                    navController.navigate(PaywallRoute) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable<CompareRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CompareRoute>()
            CompareScreen(navController, historyRecordId = route.historyRecordId)
        }
    }
}
