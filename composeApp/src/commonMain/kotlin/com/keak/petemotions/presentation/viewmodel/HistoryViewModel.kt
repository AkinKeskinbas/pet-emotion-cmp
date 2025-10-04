package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.CompareHistoryRecord
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _selectedPetFilter = MutableStateFlow<Pet?>(null)
    val selectedPetFilter: StateFlow<Pet?> = _selectedPetFilter.asStateFlow()

    private val _selectedEmotionFilter = MutableStateFlow<String?>(null)
    val selectedEmotionFilter: StateFlow<String?> = _selectedEmotionFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Cache pets list
    val pets: StateFlow<List<Pet>> = petRepository.getAllPets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Cache all analysis records
    private val allAnalysisRecords: StateFlow<List<AnalysisRecord>> = analysisRepository.getAllAnalysisRecords()
        .onEach { _isLoading.value = false }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val allCompareHistory: StateFlow<List<CompareHistoryRecord>> = analysisRepository.getCompareHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered and sorted records
    val filteredRecords: StateFlow<List<AnalysisRecord>> = combine(
        allAnalysisRecords,
        _selectedPetFilter,
        _selectedEmotionFilter
    ) { records, petFilter, emotionFilter ->
        records
            .filter { record ->
                val petMatch = petFilter?.let { it.id == record.petId } ?: true
                val emotionMatch = emotionFilter?.let { it == record.emotion } ?: true
                petMatch && emotionMatch
            }
            .sortedByDescending { it.createdAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val compareHistory: StateFlow<List<CompareHistoryRecord>> = combine(
        allCompareHistory,
        _selectedPetFilter
    ) { history, petFilter ->
        history
            .filter { record ->
                petFilter?.let { pet ->
                    (record.petAId == pet.id) || (record.petBId == pet.id)
                } ?: true
            }
            .sortedByDescending { it.createdAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSelectedPetFilter(pet: Pet?) {
        _selectedPetFilter.value = pet
    }

    fun setSelectedEmotionFilter(emotion: String?) {
        _selectedEmotionFilter.value = emotion
    }

    fun deleteAnalysisRecord(record: AnalysisRecord) {
        viewModelScope.launch {
            analysisRepository.deleteAnalysisRecord(record)
        }
    }
}
