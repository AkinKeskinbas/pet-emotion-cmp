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
                val emotionMatch = emotionFilter?.let { filter ->
                    normalizeEmotion(record.emotion) == normalizeEmotion(filter)
                } ?: true
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

    private fun normalizeEmotion(emotion: String): String {
        val normalized = emotion.trim().lowercase()
        return when {
            // English
            normalized in listOf("happy", "joy", "joyful") -> "happy"
            normalized in listOf("relaxed", "calm") -> "relaxed"
            normalized in listOf("playful", "play") -> "playful"
            normalized == "curious" -> "curious"
            normalized == "alert" -> "alert"
            normalized in listOf("stressed", "stress") -> "stressed"
            normalized == "sad" -> "sad"
            normalized == "excited" -> "excited"
            normalized in listOf("anxious", "anxiety") -> "anxious"
            // Japanese
            normalized.contains("幸せ") || normalized.contains("嬉し") -> "happy"
            normalized.contains("リラックス") || normalized.contains("落ち着") -> "relaxed"
            normalized.contains("遊び") || normalized.contains("元気") -> "playful"
            normalized.contains("好奇心") || normalized.contains("興味") -> "curious"
            normalized.contains("警戒") || normalized.contains("注意") -> "alert"
            normalized.contains("ストレス") || normalized.contains("緊張") -> "stressed"
            normalized.contains("悲し") -> "sad"
            normalized.contains("興奮") -> "excited"
            normalized.contains("不安") -> "anxious"
            else -> normalized
        }
    }
}
