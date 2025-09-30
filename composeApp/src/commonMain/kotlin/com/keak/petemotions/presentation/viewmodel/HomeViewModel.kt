package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.CoinBalance
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.model.UserPrefs
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.data.service.CoinService
import com.keak.petemotions.data.api.BackendApiService
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val userPrefs: UserPrefs = UserPrefs.default(),
    val pets: List<Pet> = emptyList(),
    val recentAnalyses: List<AnalysisRecord> = emptyList(),
    val coinBalance: CoinBalance = CoinBalance(),
    val error: UiError? = null
)

private data class HomeDataBundle(
    val userPrefs: UserPrefs,
    val pets: List<Pet>,
    val recentAnalyses: List<AnalysisRecord>,
    val coinBalance: CoinBalance
)

class HomeViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val petRepository: PetRepository,
    private val analysisRepository: AnalysisRepository,
    private val coinService: CoinService,
    private val backendApiService: BackendApiService
) : BaseViewModel<HomeUiState>(HomeUiState()) {

    init {
        println("HomeViewModel: Initializing with backendApiService: $backendApiService")
        // Coin service no longer needs initialization - coins are managed by backend
        loadData()
        fetchCoinBalance()
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            try {
                combine(
                    preferencesRepository.getUserPrefsFlow(),
                    petRepository.getAllPets(),
                    analysisRepository.getAllAnalysisRecords()
                ) { userPrefs, pets, analyses ->
                    // Sort analyses by creation date descending (latest first) and take top 5
                    val recentAnalyses = analyses.sortedByDescending { it.createdAt }.take(5)
                    HomeDataBundle(userPrefs, pets, recentAnalyses, CoinBalance())
                }.collect { bundle ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            userPrefs = bundle.userPrefs,
                            pets = bundle.pets,
                            recentAnalyses = bundle.recentAnalyses,
                            coinBalance = bundle.coinBalance,
                            error = null
                        )
                    }
                }

            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = UiError("Failed to load data: ${e.message}")
                    )
                }
            }
        }
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }

    private fun fetchCoinBalance() {
        println("HomeViewModel: fetchCoinBalance() called")
        viewModelScope.launch {
            try {
                // Check if backend is authenticated, if not wait a bit for startup registration
                if (!backendApiService.isAuthenticated()) {
                    println("HomeViewModel: Backend not authenticated yet, waiting 2 seconds for startup registration...")
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds for startup registration
                }

                println("HomeViewModel: About to call backendApiService.getCoinBalance()")
                backendApiService.getCoinBalance().fold(
                    onSuccess = { response ->
                        println("HomeViewModel: Fetched coin balance: ${response.balance}")
                        updateState {
                            it.copy(coinBalance = CoinBalance(balance = response.balance))
                        }
                    },
                    onFailure = { exception ->
                        println("HomeViewModel: Failed to fetch coin balance: ${exception.message}")
                        // If still no auth after waiting, this is expected and we silently fail
                        // Don't show error for coin balance fetch failures to avoid disrupting main UI
                    }
                )
            } catch (e: Exception) {
                println("HomeViewModel: Exception fetching coin balance: ${e.message}")
                // Silent fail for coin balance - don't disrupt main UI
            }
        }
    }

    fun refresh() {
        loadData()
        fetchCoinBalance()
    }
}