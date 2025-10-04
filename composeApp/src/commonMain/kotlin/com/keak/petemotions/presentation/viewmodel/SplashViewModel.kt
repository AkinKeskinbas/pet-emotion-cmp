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
        if (backendApiService.isAuthenticated()) {
            println("SplashViewModel: Backend already authenticated")
            return
        }

        println("SplashViewModel: Attempting backend registration")
        backendApiService.register()
            .onFailure { error ->
                println("SplashViewModel: Backend registration failed: ${error.message}")
                throw error
            }
    }
}
