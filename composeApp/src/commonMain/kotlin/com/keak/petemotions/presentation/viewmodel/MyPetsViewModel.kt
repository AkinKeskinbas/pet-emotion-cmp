package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.PetRepository
import kotlinx.coroutines.launch

data class MyPetsUiState(
    val isLoading: Boolean = false,
    val pets: List<Pet> = emptyList(),
    val error: UiError? = null
)

class MyPetsViewModel(
    private val petRepository: PetRepository
) : BaseViewModel<MyPetsUiState>(MyPetsUiState()) {

    init {
        loadPets()
    }

    private fun loadPets() {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            try {
                petRepository.getAllPets().collect { pets ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            pets = pets,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = UiError("Failed to load pets: ${e.message}")
                    )
                }
            }
        }
    }

    fun deletePet(pet: Pet) {
        viewModelScope.launch {
            try {
                petRepository.deletePet(pet)
            } catch (e: Exception) {
                updateState {
                    it.copy(error = UiError("Failed to delete pet: ${e.message}"))
                }
            }
        }
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }
}