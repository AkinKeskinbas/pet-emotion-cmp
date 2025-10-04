package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.api.ComparisonResultWithCoinInfo
import com.keak.petemotions.data.api.InsufficientCoinsException
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.CompareHistoryRecord
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
    val comparisonResult: ComparisonResultWithCoinInfo? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val historyRecord: CompareHistoryRecord? = null
)

class CompareViewModel(
    private val petRepository: PetRepository,
    private val analysisRepository: AnalysisRepository,
    private val backendApiService: BackendApiService
) : BaseViewModel<CompareUiState>(CompareUiState()) {

    private var lastComparisonSignature: String? = null
    private var isComparisonInProgress: Boolean = false
    private var preventAutoCompare: Boolean = false
    private var pendingHistoryRecordId: String? = null

    init {
        loadPets()
    }

    private fun loadPets() {
        viewModelScope.launch {
            petRepository.getAllPets().collect { pets ->
                updateState { it.copy(availablePets = pets) }
                pendingHistoryRecordId?.let { recordId ->
                    if (pets.isNotEmpty()) {
                        pendingHistoryRecordId = null
                        showComparisonFromHistory(recordId)
                    }
                }
            }
        }
    }

    fun selectPetA(pet: Pet?) {
        preventAutoCompare = false
        updateState { it.copy(selectedPetA = pet, historyRecord = null) }
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
        preventAutoCompare = false
        updateState { it.copy(selectedPetB = pet, historyRecord = null) }
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

                    // Check if data actually changed to prevent unnecessary updates
                    val currentAnalyses = if (isPetA) uiState.value.petAAnalyses else uiState.value.petBAnalyses
                    val hasChanged = currentAnalyses.size != analyses.size ||
                                    currentAnalyses.zip(analyses).any { (a, b) -> a.id != b.id }

                    if (!hasChanged) return@collect

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

                    // Only trigger comparison if not already in progress
                    if (!isComparisonInProgress) {
                        checkAndCompare()
                    }
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
        if (preventAutoCompare || state.historyRecord != null) return

        if (state.selectedPetA != null &&
            state.selectedPetB != null &&
            state.petAAnalyses.isNotEmpty() &&
            state.petBAnalyses.isNotEmpty()) {
            performComparison(force = false)
        }
    }

    private fun performComparison(force: Boolean) {
        val state = uiState.value
        if (state.selectedPetA == null || state.selectedPetB == null) return
        if (isComparisonInProgress) return

        val signature = buildComparisonSignature(state)
        if (!force && signature != null && signature == lastComparisonSignature) {
            return
        }

        preventAutoCompare = false
        updateState { it.copy(historyRecord = null) }

        viewModelScope.launch {
            isComparisonInProgress = true
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
                        val petA = state.selectedPetA
                        val petB = state.selectedPetB

                        if (petA != null && petB != null) {
                            val historyRecord = CompareHistoryRecord.create(
                                petAId = petA.id,
                                petAName = petA.name,
                                petBId = petB.id,
                                petBName = petB.name,
                                compatibilityScore = comparisonResult.compatibilityScore,
                                overview = comparisonResult.overview,
                                sharedTraits = comparisonResult.sharedTraits,
                                keyDifferences = comparisonResult.keyDifferences,
                                recommendations = comparisonResult.recommendations
                            )
                            runCatching { analysisRepository.insertCompareHistoryRecord(historyRecord) }
                                .onFailure { error ->
                                    println("CompareViewModel: Failed to persist comparison history: ${error.message}")
                                }
                        }

                        updateState {
                            it.copy(
                                comparisonResult = comparisonResult,
                                isLoading = false,
                                historyRecord = null
                            )
                        }

                        if (signature != null) {
                            lastComparisonSignature = signature
                        }
                        preventAutoCompare = true
                        isComparisonInProgress = false

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
                        lastComparisonSignature = null
                        isComparisonInProgress = false
                    }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        error = UiError(e.message ?: "Comparison failed"),
                        isLoading = false
                    )
                }
                lastComparisonSignature = null
                isComparisonInProgress = false
            }
        }
    }

    fun startComparison() {
        val state = uiState.value
        if (state.selectedPetA != null && state.selectedPetB != null) {
            lastComparisonSignature = null
            preventAutoCompare = false
            performComparison(force = true)
        }
    }

    fun clearSelection() {
        updateState {
            CompareUiState(availablePets = it.availablePets)
        }
        lastComparisonSignature = null
        isComparisonInProgress = false
        preventAutoCompare = false
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }

    fun showComparisonFromHistory(recordId: String) {
        viewModelScope.launch {
            val pets = uiState.value.availablePets
            if (pets.isEmpty()) {
                pendingHistoryRecordId = recordId
                return@launch
            }

            pendingHistoryRecordId = null
            preventAutoCompare = true
            isComparisonInProgress = false

            runCatching { analysisRepository.getCompareHistoryRecord(recordId) }
                .onSuccess { record ->
                    if (record != null) {
                        val petA = record.petAId?.let { id -> pets.find { it.id == id } }
                        val petB = record.petBId?.let { id -> pets.find { it.id == id } }

                        val comparisonResult = ComparisonResultWithCoinInfo(
                            compatibilityScore = record.compatibilityScore,
                            overview = record.overview,
                            sharedTraits = record.sharedTraits,
                            keyDifferences = record.keyDifferences,
                            recommendations = record.recommendations,
                            coinInfo = null // No coin info for historical records
                        )

                        updateState {
                            it.copy(
                                selectedPetA = petA,
                                selectedPetB = petB,
                                petAAnalyses = emptyList(),
                                petBAnalyses = emptyList(),
                                petAEmotionStats = null,
                                petBEmotionStats = null,
                                comparisonResult = comparisonResult,
                                isLoading = false,
                                error = null,
                                historyRecord = record
                            )
                        }

                        lastComparisonSignature = null
                    } else {
                        updateState {
                            it.copy(
                                historyRecord = null,
                                comparisonResult = null,
                                isLoading = false,
                                error = UiError("Comparison not found")
                            )
                        }
                    }
                }
                .onFailure { error ->
                    updateState {
                        it.copy(
                            historyRecord = null,
                            comparisonResult = null,
                            isLoading = false,
                            error = UiError(error.message ?: "Failed to load comparison")
                        )
                    }
                }
        }
    }


    private fun buildComparisonSignature(state: CompareUiState): String? {
        val petA = state.selectedPetA ?: return null
        val petB = state.selectedPetB ?: return null
        if (state.petAAnalyses.isEmpty() || state.petBAnalyses.isEmpty()) return null

        val petAKey = buildPetSignature(petA.id ?: petA.name, state.petAAnalyses)
        val petBKey = buildPetSignature(petB.id ?: petB.name, state.petBAnalyses)
        return "$petAKey|$petBKey"
    }

    private fun buildPetSignature(identifier: String, analyses: List<AnalysisRecord>): String {
        val latestTimestamp = analyses.maxOfOrNull { it.createdAt } ?: 0L
        val count = analyses.size
        return "$identifier:$count:$latestTimestamp"
    }
}
