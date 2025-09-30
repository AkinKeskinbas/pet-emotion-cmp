package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.storage.MediaStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.benasher44.uuid.uuid4

data class AddEditPetUiState(
    val pet: Pet? = null,
    val name: String = "",
    val breed: String? = null,
    val age: Int? = null,
    val notes: String? = null,
    val avatarPath: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val isSelectingPhoto: Boolean = false
)

class AddEditPetViewModel(
    private val petId: String?,
    private val petRepository: PetRepository,
    private val mediaStorage: MediaStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditPetUiState())
    val uiState: StateFlow<AddEditPetUiState> = _uiState.asStateFlow()

    init {
        if (petId != null) {
            loadPet()
        }
    }

    private fun loadPet() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val pet = petRepository.getPetById(petId!!)
                if (pet != null) {
                    _uiState.value = _uiState.value.copy(
                        pet = pet,
                        name = pet.name,
                        breed = pet.breed,
                        age = pet.age,
                        notes = pet.notes,
                        avatarPath = pet.avatarPath,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Pet not found",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load pet: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "Pet name is required" else null
        )
    }

    fun updateBreed(breed: String) {
        _uiState.value = _uiState.value.copy(
            breed = breed.ifBlank { null }
        )
    }

    fun updateAge(age: Int?) {
        _uiState.value = _uiState.value.copy(age = age)
    }

    fun updateNotes(notes: String) {
        _uiState.value = _uiState.value.copy(
            notes = notes.ifBlank { null }
        )
    }

    fun updateAvatarPath(avatarPath: String?) {
        _uiState.value = _uiState.value.copy(avatarPath = avatarPath)
    }

    fun onAvatarSelected(photoBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSelectingPhoto = true, error = null)

            try {
                val avatarPath = mediaStorage.save(photoBytes, "jpg")
                _uiState.value = _uiState.value.copy(
                    avatarPath = avatarPath,
                    isSelectingPhoto = false
                )
            } catch (throwable: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save photo: ${throwable.message}",
                    isSelectingPhoto = false
                )
            }
        }
    }

    fun savePet() {
        val currentState = _uiState.value

        // Validate input
        if (currentState.name.isBlank()) {
            _uiState.value = currentState.copy(nameError = "Pet name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)

            try {
                val pet = if (petId != null) {
                    // Update existing pet
                    currentState.pet!!.copy(
                        name = currentState.name.trim(),
                        breed = currentState.breed?.trim(),
                        age = currentState.age,
                        notes = currentState.notes?.trim(),
                        avatarPath = currentState.avatarPath,
                        updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    )
                } else {
                    // Create new pet
                    try {
                        Pet(
                            id = uuid4().toString(),
                            name = currentState.name.trim(),
                            breed = currentState.breed?.trim(),
                            age = currentState.age,
                            notes = currentState.notes?.trim(),
                            avatarPath = currentState.avatarPath,
                            createdAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                            updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                        )
                    } catch (e: Exception) {
                        throw Exception("Failed to create pet data: ${e.message}")
                    }
                }

                if (petId != null) {
                    petRepository.updatePet(pet)
                } else {
                    petRepository.insertPet(pet)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to save pet: ${e.message}"
                )
            }
        }
    }

    fun deletePet() {
        if (petId == null || _uiState.value.pet == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                petRepository.deletePet(_uiState.value.pet!!)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to delete pet: ${e.message}"
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
