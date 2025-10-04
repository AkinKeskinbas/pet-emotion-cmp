package com.keak.petemotions.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.keak.petemotions.data.api.InsufficientCoinsException
import com.keak.petemotions.data.model.AnalysisCost
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.MediaType
import com.keak.petemotions.data.model.Pet
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.data.service.CoinService
import com.keak.petemotions.platform.CameraService
import com.keak.petemotions.platform.CameraPermissionStatus
import kotlinx.coroutines.CoroutineExceptionHandler
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
    val recordingTimeRemaining: Int = 0,
    val capturedPhotoBytes: ByteArray? = null,
    val coinError: String? = null, // Insufficient coins error message
    val paywallNavigationEvent: Boolean = false // Trigger to auto-open paywall
)

enum class CaptureMode {
    PHOTO, VIDEO
}

class CameraViewModel(
    private val analysisRepository: AnalysisRepository,
    private val petRepository: PetRepository,
    private val preferencesRepository: PreferencesRepository,
    private val cameraService: CameraService,
    private val coinService: CoinService
) : BaseViewModel<CameraUiState>(CameraUiState()) {

    private var recordingTimerJob: Job? = null
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        println("CameraViewModel: Unhandled coroutine error -> ${throwable.message}")
        throwable.printStackTrace()
        updateState { state ->
            state.copy(error = UiError(throwable.message ?: "Unexpected camera error"))
        }
    }

    companion object {
        private const val VIDEO_DURATION_SECONDS = 5
    }

    init {
        loadPets()
        checkPermissions()
        observeRecordingState()
    }

    private fun loadPets() {
        viewModelScope.launch(errorHandler) {
            petRepository.getAllPets().collect { pets ->
                updateState { it.copy(availablePets = pets) }
            }
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch(errorHandler) {
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
        viewModelScope.launch(errorHandler) {
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
        viewModelScope.launch(errorHandler) {
            val granted = cameraService.requestCameraPermission()
            val status = if (granted) CameraPermissionStatus.GRANTED else CameraPermissionStatus.DENIED
            updateState { it.copy(cameraPermissionStatus = status) }
        }
    }

    fun requestMicrophonePermission() {
        viewModelScope.launch(errorHandler) {
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
        println("ViewModel: showComingSoonMessage called with: $message")
        updateState { it.copy(error = UiError(message)) }
        println("ViewModel: Error state updated with coming soon message")
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }

    fun analyzeSelectedImage(imageBytes: ByteArray) {
        println("ViewModel: analyzeSelectedImage called with ${imageBytes.size} bytes")
        // Set capture mode to photo for selected images and store the image
        updateState {
            it.copy(
                captureMode = CaptureMode.PHOTO,
                capturedPhotoBytes = imageBytes  // Store gallery image for preview consistency
            )
        }
        // Use the existing analyzeMedia function
        analyzeMedia(imageBytes)
    }

    fun analyzeSelectedVideo(videoBytes: ByteArray) {
        println("ViewModel: analyzeSelectedVideo called with ${videoBytes.size} bytes")
        // Set capture mode to video for selected videos
        updateState {
            it.copy(
                captureMode = CaptureMode.VIDEO,
                capturedPhotoBytes = null  // Clear any photo preview
            )
        }
        // Use the existing analyzeMedia function
        analyzeMedia(videoBytes)
    }

    fun capturePhoto() {
        println("ViewModel: capturePhoto called")
        viewModelScope.launch(errorHandler) {
            if (_uiState.value.cameraPermissionStatus != CameraPermissionStatus.GRANTED) {
                println("ViewModel: Camera permission not granted")
                updateState { it.copy(error = UiError("Camera permission required")) }
                return@launch
            }

            println("ViewModel: Calling camera service...")
            cameraService.capturePhoto().fold(
                onSuccess = { mediaBytes ->
                    println("ViewModel: Photo captured, ${mediaBytes.size} bytes")
                    // Store captured photo in state for immediate display
                    updateState { it.copy(capturedPhotoBytes = mediaBytes) }
                },
                onFailure = { exception ->
                    println("ViewModel: Photo capture failed: ${exception.message}")
                    updateState { it.copy(error = UiError("Failed to capture photo: ${exception.message}")) }
                }
            )
        }
    }

    fun retakePhoto() {
        println("ViewModel: retakePhoto called")
        updateState { it.copy(capturedPhotoBytes = null) }
    }

    fun analyzeCurrentPhoto() {
        println("ViewModel: analyzeCurrentPhoto called")
        _uiState.value.capturedPhotoBytes?.let { photoBytes ->
            analyzeMedia(photoBytes)
        } ?: run {
            updateState { it.copy(error = UiError("No photo to analyze")) }
        }
    }

    fun onPhotoCaptured(photoBytes: ByteArray) {
        println("ViewModel: onPhotoCaptured called with ${photoBytes.size} bytes")
        updateState { it.copy(capturedPhotoBytes = photoBytes) }
    }

    fun startVideoRecording() {
        viewModelScope.launch(errorHandler) {
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
        recordingTimerJob = viewModelScope.launch(errorHandler) {
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
        viewModelScope.launch(errorHandler) {
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
        println("ViewModel: analyzeMedia called with ${mediaBytes.size} bytes")
        viewModelScope.launch(errorHandler) {
            updateState { it.copy(isAnalyzing = true, error = null) }

            try {
                println("ViewModel: Starting backend analysis...")

                // Save media file
                val mediaType = if (_uiState.value.captureMode == CaptureMode.PHOTO) {
                    MediaType.IMAGE.value
                } else {
                    MediaType.VIDEO.value
                }

                val mediaPath = analysisRepository.saveMediaFile(mediaBytes, mediaType)
                println("ViewModel: Media saved to: $mediaPath")

                // Analyze with backend (no API key needed anymore)
                val analysisResult = analysisRepository.analyzeMediaWithAI(mediaBytes, "", mediaType)

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
                            detailsBody = "Body Language\n${result.details.bodyLanguage}\n\nVocalization\n${result.details.vocalization}\n\nContext\n${result.details.context}",
                            tags = result.tags
                        )

                        // Save to database
                        analysisRepository.insertAnalysisRecord(record)

                        // Backend handles coin deduction automatically
                        updateState {
                            it.copy(
                                isAnalyzing = false,
                                analysisResult = record,
                                navigationEvent = record.id
                            )
                        }

                        // Backend analysis result doesn't contain coin info yet
                        // This will be added when backend implements coin tracking
                        println("CameraViewModel: Analysis completed successfully")
                    },
                    onFailure = { exception: Throwable ->
                        when (exception) {
                            is InsufficientCoinsException -> {
                                val coinErrorMessage = exception.message ?: "Insufficient coins for analysis"
                                println("CameraViewModel: Insufficient coins error: $coinErrorMessage")
                                updateState {
                                    it.copy(
                                        isAnalyzing = false,
                                        coinError = coinErrorMessage,
                                        paywallNavigationEvent = true
                                    )
                                }
                            }
                            else -> {
                                val errorMessage = exception.message ?: "Analysis failed. Please try again."
                                updateState {
                                    it.copy(
                                        isAnalyzing = false,
                                        error = UiError(errorMessage)
                                    )
                                }
                            }
                        }
                    }
                )

            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isAnalyzing = false,
                        error = UiError(e.message ?: "Something went wrong. Please try again.")
                    )
                }
            }
        }
    }

    fun dismissError() {
        updateState { it.copy(error = null) }
    }

    fun dismissCoinError() {
        updateState { it.copy(coinError = null) }
    }

    fun clearPaywallNavigationEvent() {
        updateState { it.copy(paywallNavigationEvent = false) }
    }

    fun clearNavigationEvent() {
        println("ViewModel: clearNavigationEvent called - clearing photo and resetting state")
        updateState {
            it.copy(
                navigationEvent = null,
                capturedPhotoBytes = null, // Clear captured photo after analysis
                analysisResult = null      // Clear analysis result for fresh start
            )
        }
    }

}
