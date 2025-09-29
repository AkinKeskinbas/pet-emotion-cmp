package com.keak.petemotions.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.keak.petemotions.presentation.screens.WelcomeScreen
import com.keak.petemotions.presentation.screens.HomeScreen
import com.keak.petemotions.presentation.screens.CameraScreen
import com.keak.petemotions.presentation.screens.ResultDetailScreen
import com.keak.petemotions.presentation.screens.HistoryScreen
import com.keak.petemotions.presentation.screens.MyPetsScreen
import com.keak.petemotions.presentation.screens.AddEditPetScreen
import com.keak.petemotions.presentation.screens.AdvancedAnalysisScreen
import com.keak.petemotions.presentation.screens.PaywallScreen
import com.keak.petemotions.presentation.screens.PermissionsScreen

@Composable
fun PetEmotionsNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: Any = WelcomeRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
            PaywallScreen(navController)
        }

        composable<PermissionsRoute> {
            PermissionsScreen(
                navController = navController,
                onContinue = {
                    navController.navigate(HomeRoute) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}