package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.model.UserPrefs
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val userPrefs: UserPrefs = UserPrefs.default(),
    val pets: List<Pet> = emptyList(),
    val recentAnalyses: List<AnalysisRecord> = emptyList(),
    val error: UiError? = null
)

class HomeViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val petRepository: PetRepository,
    private val analysisRepository: AnalysisRepository
) : BaseViewModel<HomeUiState>(HomeUiState()) {

    init {
        loadData()
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
                    Triple(userPrefs, pets, analyses.take(5)) // Show recent 5
                }.collect { (userPrefs, pets, recentAnalyses) ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            userPrefs = userPrefs,
                            pets = pets,
                            recentAnalyses = recentAnalyses,
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

    fun refresh() {
        loadData()
    }
}