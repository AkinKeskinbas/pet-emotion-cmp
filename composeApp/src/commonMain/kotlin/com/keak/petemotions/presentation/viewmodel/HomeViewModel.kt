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
import kotlinx.datetime.Clock

data class HomeUiState(
    val isLoading: Boolean = false,
    val isCoinBalanceLoading: Boolean = false,
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
        loadData()
        // Fetch coin balance in background without blocking UI
        viewModelScope.launch {
            fetchCoinBalanceAsync()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            try {
                combine(
                    preferencesRepository.getUserPrefsFlow(),
                    petRepository.getAllPets(),
                    analysisRepository.getAllAnalysisRecords(),
                    preferencesRepository.getCoinBalance()
                ) { userPrefs, pets, analyses, coinBalance ->
                    // Sort analyses by creation date descending (latest first) and take top 5
                    val recentAnalyses = analyses.sortedByDescending { it.createdAt }.take(5)
                    HomeDataBundle(userPrefs, pets, recentAnalyses, coinBalance)
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

    private suspend fun fetchCoinBalanceAsync() {
        try {
            updateState { it.copy(isCoinBalanceLoading = true) }
            println("HomeViewModel: Fetching coin balance in background...")
            backendApiService.getCoinBalance().fold(
                onSuccess = { response ->
                    println("HomeViewModel: Fetched coin balance: ${response.balance}")
                    val updatedBalance = CoinBalance(
                        balance = response.balance,
                        lastUpdated = Clock.System.now().toEpochMilliseconds()
                    )
                    preferencesRepository.saveCoinBalance(updatedBalance)
                    updateState { it.copy(coinBalance = updatedBalance, isCoinBalanceLoading = false) }
                },
                onFailure = { exception ->
                    println("HomeViewModel: Failed to fetch coin balance: ${exception.message}")
                    updateState { it.copy(isCoinBalanceLoading = false) }
                    // Silent fail - don't disrupt main UI
                }
            )
        } catch (e: Exception) {
            println("HomeViewModel: Exception fetching coin balance: ${e.message}")
            updateState { it.copy(isCoinBalanceLoading = false) }
            // Silent fail for coin balance
        }
    }

    // Public function to manually refresh coin balance (e.g., after purchase)
    fun refreshCoinBalance() {
        viewModelScope.launch {
            fetchCoinBalanceAsync()
        }
    }

    fun refresh() {
        loadData()
        refreshCoinBalance()
    }
}
