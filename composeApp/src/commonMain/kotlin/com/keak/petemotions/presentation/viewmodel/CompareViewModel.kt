package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.api.ComparisonResult
import com.keak.petemotions.data.api.InsufficientCoinsException
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import kotlinx.coroutines.launch

data class CompareUiState(
    val availablePets: List<Pet> = emptyList(),
    val selectedPetA: Pet? = null,
    val selectedPetB: Pet? = null,
    val petAAnalyses: List<AnalysisRecord> = emptyList(),
    val petBAnalyses: List<AnalysisRecord> = emptyList(),
    val petAEmotionStats: Map<String, Float>? = null,
    val petBEmotionStats: Map<String, Float>? = null,
    val comparisonResult: ComparisonResult? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null
)

class CompareViewModel(
    private val petRepository: PetRepository,
    private val analysisRepository: AnalysisRepository,
    private val backendApiService: BackendApiService
) : BaseViewModel<CompareUiState>(CompareUiState()) {

    init {
        loadPets()
    }

    private fun loadPets() {
        viewModelScope.launch {
            petRepository.getAllPets().collect { pets ->
                updateState { it.copy(availablePets = pets) }
            }
        }
    }

    fun selectPetA(pet: Pet?) {
        updateState { it.copy(selectedPetA = pet) }
        if (pet != null) {
            loadPetAnalyses(pet, true)
        } else {
            updateState {
                it.copy(
                    petAAnalyses = emptyList(),
                    petAEmotionStats = null,
                    comparisonResult = null
                )
            }
        }
        checkAndCompare()
    }

    fun selectPetB(pet: Pet?) {
        updateState { it.copy(selectedPetB = pet) }
        if (pet != null) {
            loadPetAnalyses(pet, false)
        } else {
            updateState {
                it.copy(
                    petBAnalyses = emptyList(),
                    petBEmotionStats = null,
                    comparisonResult = null
                )
            }
        }
        checkAndCompare()
    }

    private fun loadPetAnalyses(pet: Pet, isPetA: Boolean) {
        viewModelScope.launch {
            try {
                analysisRepository.getAnalysisRecordsByPetId(pet.id).collect { analyses ->
                    val emotionStats = calculateEmotionStats(analyses)

                    if (isPetA) {
                        updateState {
                            it.copy(
                                petAAnalyses = analyses,
                                petAEmotionStats = emotionStats
                            )
                        }
                    } else {
                        updateState {
                            it.copy(
                                petBAnalyses = analyses,
                                petBEmotionStats = emotionStats
                            )
                        }
                    }

                    checkAndCompare()
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(error = UiError("Failed to load analyses for ${pet.name}: ${e.message}"))
                }
            }
        }
    }

    private fun calculateEmotionStats(analyses: List<AnalysisRecord>): Map<String, Float> {
        if (analyses.isEmpty()) return emptyMap()

        val emotionCounts = mutableMapOf<String, Int>()
        val totalAnalyses = analyses.size

        analyses.forEach { analysis ->
            val emotion = analysis.emotion.lowercase()
            emotionCounts[emotion] = (emotionCounts[emotion] ?: 0) + 1
        }

        return emotionCounts.mapValues { (_, count) ->
            count.toFloat() / totalAnalyses.toFloat()
        }
    }

    private fun checkAndCompare() {
        val state = uiState.value
        if (state.selectedPetA != null &&
            state.selectedPetB != null &&
            state.petAAnalyses.isNotEmpty() &&
            state.petBAnalyses.isNotEmpty()) {
            performComparison()
        }
    }

    private fun performComparison() {
        val state = uiState.value
        if (state.selectedPetA == null || state.selectedPetB == null) return

        viewModelScope.launch {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // Convert AnalysisRecord to AnalysisResult for backend API
                val petAResults = state.petAAnalyses.map { record ->
                    com.keak.petemotions.data.model.AnalysisResult(
                        emotion = record.emotion,
                        confidence = record.confidence,
                        summary = record.summary,
                        details = com.keak.petemotions.data.model.AnalysisResult.AnalysisDetails(
                            bodyLanguage = "",
                            vocalization = "",
                            context = ""
                        ),
                        tags = emptyList()
                    )
                }

                val petBResults = state.petBAnalyses.map { record ->
                    com.keak.petemotions.data.model.AnalysisResult(
                        emotion = record.emotion,
                        confidence = record.confidence,
                        summary = record.summary,
                        details = com.keak.petemotions.data.model.AnalysisResult.AnalysisDetails(
                            bodyLanguage = "",
                            vocalization = "",
                            context = ""
                        ),
                        tags = emptyList()
                    )
                }

                backendApiService.comparePetEmotions(petAResults, petBResults)
                    .onSuccess { comparisonResult ->
                        // Backend handles coin deduction automatically
                        updateState {
                            it.copy(
                                comparisonResult = comparisonResult,
                                isLoading = false
                            )
                        }

                        // Log coin info if available
                        comparisonResult.coinInfo?.let { coinInfo ->
                            println("CompareViewModel: Coins spent: ${coinInfo.costPaid}, Remaining: ${coinInfo.remainingBalance}")
                        }
                    }
                    .onFailure { exception ->
                        val errorMessage = when (exception) {
                            is InsufficientCoinsException -> exception.message ?: "Insufficient coins for comparison"
                            else -> exception.message ?: "Failed to compare pets"
                        }

                        updateState {
                            it.copy(
                                error = UiError(errorMessage),
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        error = UiError(e.message ?: "Comparison failed"),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun startComparison() {
        val state = uiState.value
        if (state.selectedPetA != null && state.selectedPetB != null) {
            performComparison()
        }
    }

    fun clearSelection() {
        updateState {
            CompareUiState(availablePets = it.availablePets)
        }
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }
}