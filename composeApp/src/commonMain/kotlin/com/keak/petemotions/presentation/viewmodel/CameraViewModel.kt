package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.MediaType
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.platform.CameraService
import com.keak.petemotions.platform.CameraPermissionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CameraUiState(
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val captureMode: CaptureMode = CaptureMode.PHOTO,
    val selectedPet: Pet? = null,
    val availablePets: List<Pet> = emptyList(),
    val analysisResult: AnalysisRecord? = null,
    val error: UiError? = null,
    val navigationEvent: String? = null,
    val cameraPermissionStatus: CameraPermissionStatus = CameraPermissionStatus.NOT_REQUESTED,
    val microphonePermissionStatus: CameraPermissionStatus = CameraPermissionStatus.NOT_REQUESTED,
    val isRecording: Boolean = false,
    val recordingTimeRemaining: Int = 0
)

enum class CaptureMode {
    PHOTO, VIDEO
}

class CameraViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository,
    private val preferencesRepository: PreferencesRepository,
    private val cameraService: CameraService
) : BaseViewModel<CameraUiState>(CameraUiState()) {

    private var recordingTimerJob: Job? = null

    companion object {
        private const val VIDEO_DURATION_SECONDS = 5
    }

    init {
        loadPets()
        checkPermissions()
        observeRecordingState()
    }

    private fun loadPets() {
        viewModelScope.launch {
            petRepository.getAllPets().collect { pets ->
                updateState { it.copy(availablePets = pets) }
            }
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val cameraStatus = cameraService.checkCameraPermission()
            val microphoneStatus = cameraService.checkMicrophonePermission()
            updateState {
                it.copy(
                    cameraPermissionStatus = cameraStatus,
                    microphonePermissionStatus = microphoneStatus
                )
            }
        }
    }

    private fun observeRecordingState() {
        viewModelScope.launch {
            cameraService.isRecording.collect { isRecording ->
                updateState { it.copy(isRecording = isRecording) }
            }
        }
    }

    fun setCaptureMode(mode: CaptureMode) {
        updateState { it.copy(captureMode = mode) }
    }

    fun selectPet(pet: Pet?) {
        updateState { it.copy(selectedPet = pet) }
    }

    fun requestCameraPermission() {
        viewModelScope.launch {
            val granted = cameraService.requestCameraPermission()
            val status = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
            updateState { it.copy(cameraPermissionStatus = status) }
        }
    }

    fun requestMicrophonePermission() {
        viewModelScope.launch {
            val granted = cameraService.requestMicrophonePermission()
            val status = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
            updateState { it.copy(microphonePermissionStatus = status) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        val status = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
        updateState { it.copy(cameraPermissionStatus = status) }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        val status = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
        updateState { it.copy(microphonePermissionStatus = status) }
    }

    fun showComingSoonMessage(message: String) {
        updateState { it.copy(error = UiError(message)) }
    }

    fun analyzeSelectedImage(imageBytes: ByteArray) {
        // Set capture mode to photo for selected images
        updateState { it.copy(captureMode = CaptureMode.PHOTO) }
        // Use the existing analyzeMedia function
        analyzeMedia(imageBytes)
    }

    fun capturePhoto() {
        viewModelScope.launch {
            if (_uiState.value.cameraPermissionStatus != CameraPermissionStatus.GRANTED) {
                updateState { it.copy(error = UiError("Camera permission required")) }
                return@launch
            }

            cameraService.capturePhoto().fold(
                onSuccess = { mediaBytes ->
                    analyzeMedia(mediaBytes)
                },
                onFailure = { exception ->
                    updateState { it.copy(error = UiError("Failed to capture photo: ${exception.message}")) }
                }
            )
        }
    }

    fun startVideoRecording() {
        viewModelScope.launch {
            if (_uiState.value.cameraPermissionStatus != CameraPermissionStatus.GRANTED) {
                updateState { it.copy(error = UiError("Camera permission required")) }
                return@launch
            }

            cameraService.startVideoRecording().fold(
                onSuccess = {
                    // Start countdown timer
                    startRecordingTimer()
                },
                onFailure = { exception ->
                    updateState { it.copy(error = UiError("Failed to start recording: ${exception.message}")) }
                }
            )
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            for (timeRemaining in VIDEO_DURATION_SECONDS downTo 1) {
                updateState { it.copy(recordingTimeRemaining = timeRemaining) }
                delay(1000)
            }
            // Auto-stop recording after 5 seconds
            updateState { it.copy(recordingTimeRemaining = 0) }
            stopVideoRecording()
        }
    }

    fun stopVideoRecording() {
        recordingTimerJob?.cancel()
        viewModelScope.launch {
            updateState { it.copy(recordingTimeRemaining = 0) }
            cameraService.stopVideoRecording().fold(
                onSuccess = { mediaBytes ->
                    analyzeMedia(mediaBytes)
                },
                onFailure = { exception ->
                    updateState { it.copy(error = UiError("Failed to stop recording: ${exception.message}")) }
                }
            )
        }
    }

    fun analyzeMedia(mediaBytes: ByteArray) {
        viewModelScope.launch {
            updateState { it.copy(isAnalyzing = true, error = null) }

            try {
                val userPrefs = preferencesRepository.getUserPrefs()
                val apiKey = userPrefs.openAiApiKey

                if (apiKey.isNullOrBlank()) {
                    updateState {
                        it.copy(
                            isAnalyzing = false,
                            error = UiError("OpenAI API key not configured. Please set it in settings.")
                        )
                    }
                    return@launch
                }

                // Save media file
                val mediaType = if (_uiState.value.captureMode == CaptureMode.PHOTO) {
                    MediaType.IMAGE.value
                } else {
                    MediaType.VIDEO.value
                }

                val mediaPath = analysisRepository.saveMediaFile(mediaBytes, mediaType)

                // Analyze with AI
                val analysisResult = analysisRepository.analyzeMediaWithAI(mediaBytes, apiKey)

                analysisResult.fold(
                    onSuccess = { result ->
                        // Create analysis record
                        val record = AnalysisRecord.create(
                            petId = _uiState.value.selectedPet?.id,
                            mediaPath = mediaPath,
                            mediaType = mediaType,
                            emotion = result.emotion,
                            confidence = result.confidence,
                            summary = result.summary,
                            detailsBody = "${result.details.bodyLanguage}\n\n${result.details.vocalization}\n\n${result.details.context}",
                            tags = result.tags
                        )

                        // Save to database
                        analysisRepository.insertAnalysisRecord(record)

                        updateState {
                            it.copy(
                                isAnalyzing = false,
                                analysisResult = record,
                                navigationEvent = record.id
                            )
                        }
                    },
                    onFailure = { exception ->
                        updateState {
                            it.copy(
                                isAnalyzing = false,
                                error = UiError("Analysis failed: ${exception.message}")
                            )
                        }
                    }
                )

            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isAnalyzing = false,
                        error = UiError("Failed to analyze media: ${e.message}")
                    )
                }
            }
        }
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }

    fun clearNavigationEvent() {
        updateState { it.copy(navigationEvent = null) }
    }
}