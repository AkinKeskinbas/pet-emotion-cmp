package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultDetailViewModel(
    private val analysisRecordId: String,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _analysisRecord = MutableStateFlow<AnalysisRecord?>(null)
    val analysisRecord: StateFlow<AnalysisRecord?> = _analysisRecord.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAnalysisRecord()
    }

    private fun loadAnalysisRecord() {
        viewModelScope.launch {
            try {
                _analysisRecord.value = analysisRepository.getAnalysisRecordById(analysisRecordId)
                if (_analysisRecord.value == null) {
                    _error.value = "Analysis record not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load analysis: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
