package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.AnalysisResult
import com.keak.petemotions.data.model.CompareHistoryRecord
import kotlinx.coroutines.Dispatchers
import com.keak.petemotions.data.storage.MediaStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnalysisRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val backendApiService: BackendApiService,
    private val mediaStorage: MediaStorage
) : AnalysisRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val ANALYSIS_RECORDS_KEY = stringPreferencesKey("analysis_records_list")
        private val MEDIA_FILES_KEY = stringPreferencesKey("media_files")
        private val COMPARE_HISTORY_KEY = stringPreferencesKey("compare_history")
    }

    override fun getAllAnalysisRecords(): Flow<List<AnalysisRecord>> {
        return dataStore.data.map { preferences ->
            val recordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<AnalysisRecord>>(recordsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun getAnalysisRecordsByPetId(petId: String): Flow<List<AnalysisRecord>> {
        return getAllAnalysisRecords().map { records ->
            records.filter { it.petId == petId }
        }
    }

    override suspend fun getAnalysisRecordById(id: String): AnalysisRecord? = withContext(Dispatchers.Default) {
        println("AnalysisRepositoryImpl: getAnalysisRecordById called with ID: $id")
        return@withContext try {
            val preferences = dataStore.data.first()
            val recordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            println("AnalysisRepositoryImpl: Raw JSON from datastore: $recordsJson")

            val records = json.decodeFromString<List<AnalysisRecord>>(recordsJson)
            println("AnalysisRepositoryImpl: Found ${records.size} total records")

            val result = records.find { it.id == id }
            println("AnalysisRepositoryImpl: Search result for ID $id: ${result?.emotion ?: "NOT FOUND"}")
            result
        } catch (e: Exception) {
            println("AnalysisRepositoryImpl: Error getting record by ID: ${e.message}")
            null
        }
    }

    override suspend fun insertAnalysisRecord(record: AnalysisRecord) {
        dataStore.edit { preferences ->
            val currentRecordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            val currentRecords = try {
                json.decodeFromString<List<AnalysisRecord>>(currentRecordsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRecords = currentRecords + record
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(updatedRecords)
        }
    }

    override suspend fun updateAnalysisRecord(record: AnalysisRecord) {
        dataStore.edit { preferences ->
            val currentRecordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            val currentRecords = try {
                json.decodeFromString<List<AnalysisRecord>>(currentRecordsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRecords = currentRecords.map { if (it.id == record.id) record else it }
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(updatedRecords)
        }
    }

    override suspend fun deleteAnalysisRecord(record: AnalysisRecord) {
        dataStore.edit { preferences ->
            val currentRecordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            val currentRecords = try {
                json.decodeFromString<List<AnalysisRecord>>(currentRecordsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRecords = currentRecords.filter { it.id != record.id }
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(updatedRecords)
        }
    }

    override suspend fun deleteAnalysisRecordById(id: String) {
        dataStore.edit { preferences ->
            val currentRecordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            val currentRecords = try {
                json.decodeFromString<List<AnalysisRecord>>(currentRecordsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRecords = currentRecords.filter { it.id != id }
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(updatedRecords)
        }
    }

    override suspend fun deleteAnalysisRecordsByPetId(petId: String) {
        dataStore.edit { preferences ->
            val currentRecordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            val currentRecords = try {
                json.decodeFromString<List<AnalysisRecord>>(currentRecordsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedRecords = currentRecords.filter { it.petId != petId }
            preferences[ANALYSIS_RECORDS_KEY] = json.encodeToString(updatedRecords)
        }
    }

    override fun getAnalysisRecordsByEmotion(emotion: String): Flow<List<AnalysisRecord>> {
        return getAllAnalysisRecords().map { records ->
            records.filter { it.emotion.equals(emotion, ignoreCase = true) }
        }
    }

    override suspend fun getAnalysisRecordsBetween(startTime: Long, endTime: Long): List<AnalysisRecord> {
        return getAllAnalysisRecords().map { records ->
            records.filter { it.createdAt in startTime..endTime }
        }.let { flow ->
            var result: List<AnalysisRecord> = emptyList()
            flow.collect { result = it }
            result
        }
    }

    override suspend fun getAnalysisRecordsByPetBetween(petId: String, startTime: Long, endTime: Long): List<AnalysisRecord> {
        return getAllAnalysisRecords().map { records ->
            records.filter { it.petId == petId && it.createdAt in startTime..endTime }
        }.let { flow ->
            var result: List<AnalysisRecord> = emptyList()
            flow.collect { result = it }
            result
        }
    }

    override suspend fun getEmotionDistribution(startTime: Long, endTime: Long): Map<String, Int> {
        val records = getAnalysisRecordsBetween(startTime, endTime)
        return records.groupBy { it.emotion }.mapValues { it.value.size }
    }

    override suspend fun getEmotionDistributionByPet(petId: String, startTime: Long, endTime: Long): Map<String, Int> {
        val records = getAnalysisRecordsByPetBetween(petId, startTime, endTime)
        return records.groupBy { it.emotion }.mapValues { it.value.size }
    }

    override suspend fun getAnalysisRecordCount(): Int {
        return getAllAnalysisRecords().map { it.size }.let { flow ->
            var result = 0
            flow.collect { result = it }
            result
        }
    }

    override suspend fun getAnalysisRecordCountByPet(petId: String): Int {
        return getAnalysisRecordsByPetId(petId).map { it.size }.let { flow ->
            var result = 0
            flow.collect { result = it }
            result
        }
    }

    override fun getCompareHistory(): Flow<List<CompareHistoryRecord>> {
        return dataStore.data.map { preferences ->
            val historyJson = preferences[COMPARE_HISTORY_KEY] ?: "[]"
            try {
                json.decodeFromString<List<CompareHistoryRecord>>(historyJson)
            } catch (e: Exception) {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun insertCompareHistoryRecord(record: CompareHistoryRecord) {
        dataStore.edit { preferences ->
            val currentHistoryJson = preferences[COMPARE_HISTORY_KEY] ?: "[]"
            val currentHistory = try {
                json.decodeFromString<List<CompareHistoryRecord>>(currentHistoryJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedHistory = (listOf(record) + currentHistory).distinctBy { it.id }.take(50)
            preferences[COMPARE_HISTORY_KEY] = json.encodeToString(updatedHistory)
        }
    }

    override suspend fun clearCompareHistory() {
        dataStore.edit { preferences ->
            preferences.remove(COMPARE_HISTORY_KEY)
        }
    }

    override suspend fun getCompareHistoryRecord(id: String): CompareHistoryRecord? {
        return try {
            val preferences = dataStore.data.first()
            val historyJson = preferences[COMPARE_HISTORY_KEY] ?: "[]"
            val history = json.decodeFromString<List<CompareHistoryRecord>>(historyJson)
            history.find { it.id == id }
        } catch (e: Exception) {
            println("AnalysisRepositoryImpl: Failed to load compare history record $id: ${e.message}")
            null
        }
    }

    override suspend fun saveMediaFile(mediaBytes: ByteArray, mediaType: String): String {
        val extension = when (mediaType.lowercase()) {
            "video" -> "mp4"
            else -> "jpg"
        }

        return try {
            val savedPath = mediaStorage.save(mediaBytes, extension)
            println("AnalysisRepositoryImpl: Media persisted locally as $savedPath (${mediaBytes.size} bytes)")
            savedPath
        } catch (e: Exception) {
            val fallbackName = "media_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.$extension"
            println("AnalysisRepositoryImpl: Failed to persist media locally: ${e.message}")
            println("AnalysisRepositoryImpl: Falling back to placeholder reference $fallbackName")
            fallbackName
        }
    }

    override suspend fun loadMediaFile(mediaPath: String): ByteArray? {
        return try {
            mediaStorage.load(mediaPath).also { bytes ->
                if (bytes == null) {
                    println("AnalysisRepositoryImpl: No local media found for $mediaPath - showing placeholder")
                } else {
                    println("AnalysisRepositoryImpl: Loaded media ${bytes.size} bytes for $mediaPath")
                }
            }
        } catch (e: Exception) {
            println("AnalysisRepositoryImpl: Failed to load media $mediaPath: ${e.message}")
            null
        }
    }

    override suspend fun analyzeMediaWithAI(mediaBytes: ByteArray, apiKey: String, mediaType: String): Result<AnalysisResult> {
        return try {
            println("AnalysisRepository: Starting backend analysis for ${mediaBytes.size} bytes, type: $mediaType")

            val backendResult = backendApiService.analyzeMedia(mediaBytes, mediaType)

            backendResult.fold(
                onSuccess = { result ->
                    // Convert backend result to internal AnalysisResult format
                    val analysisResult = AnalysisResult(
                        emotion = result.emotion,
                        confidence = result.confidence,
                        summary = result.summary,
                        details = AnalysisResult.AnalysisDetails(
                            bodyLanguage = result.details.bodyLanguage,
                            vocalization = result.details.vocalization,
                            context = result.details.context
                        ),
                        tags = result.tags
                    )
                    println("AnalysisRepository: Backend analysis successful - ${result.emotion} (${result.confidence})")
                    Result.success(analysisResult)
                },
                onFailure = { error ->
                    println("AnalysisRepository: Backend analysis failed: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("AnalysisRepository: Analysis exception: ${e.message}")
            Result.failure(e)
        }
    }
}
