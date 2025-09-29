package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<UiState>(initialState: UiState) : ViewModel() {

    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    protected fun updateState(update: (UiState) -> UiState) {
        _uiState.value = update(_uiState.value)
    }
}

data class UiError(
    val message: String,
    val action: (() -> Unit)? = null
)