package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val destination: SplashDestination? = null,
    val errorMessage: String? = null
)

sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object Home : SplashDestination
}

class SplashViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backendApiService: BackendApiService
) : BaseViewModel<SplashUiState>(SplashUiState()) {

    init {
        evaluateStartupState()
    }

    fun evaluateStartupState() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null, destination = null) }

            try {
                ensureBackendAuthentication()

                val hasCompletedOnboarding = preferencesRepository
                    .hasCompletedOnboarding()
                    .first()

                val destination = if (hasCompletedOnboarding) {
                    SplashDestination.Home
                } else {
                    SplashDestination.Onboarding
                }

                updateState {
                    it.copy(
                        isLoading = false,
                        destination = destination,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                println("SplashViewModel: Failed to resolve startup state: ${e.message}")
                updateState {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Something went wrong"
                    )
                }
            }
        }
    }

    fun onNavigationHandled() {
        updateState { it.copy(destination = null) }
    }

    private suspend fun ensureBackendAuthentication() {
        // Backend registration already happens in Application/IOSApp initialization
        // Just verify authentication status here
        if (backendApiService.isAuthenticated()) {
            println("SplashViewModel: Backend already authenticated")
            return
        }

        // If not authenticated, this is an error state
        // The app should have already registered in Application/IOSApp init
        println("SplashViewModel: WARNING - Backend not authenticated after app initialization")
        println("SplashViewModel: This should not happen as registration occurs in Application/IOSApp")

        // Don't register again here - it already happened in app init
        // If we're not authenticated at this point, something went wrong earlier
        throw IllegalStateException("Backend authentication not initialized properly")
    }
}
