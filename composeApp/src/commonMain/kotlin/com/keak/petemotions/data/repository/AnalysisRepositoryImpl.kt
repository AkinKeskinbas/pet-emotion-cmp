package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.AnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform

class AnalysisRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : AnalysisRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val openAIService: OpenAIService by lazy { KoinPlatform.getKoin().get() }

    companion object {
        private val ANALYSIS_RECORDS_KEY = stringPreferencesKey("analysis_records_list")
        private val MEDIA_FILES_KEY = stringPreferencesKey("media_files")
    }

    override fun getAllAnalysisRecords(): Flow<List<AnalysisRecord>> {
        return dataStore.data.map { preferences ->
            val recordsJson = preferences[ANALYSIS_RECORDS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<AnalysisRecord>>(recordsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getAnalysisRecordsByPetId(petId: String): Flow<List<AnalysisRecord>> {
        return getAllAnalysisRecords().map { records ->
            records.filter { it.petId == petId }
        }
    }

    override suspend fun getAnalysisRecordById(id: String): AnalysisRecord? {
        println("AnalysisRepositoryImpl: getAnalysisRecordById called with ID: $id")
        return try {
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

    override suspend fun saveMediaFile(mediaBytes: ByteArray, mediaType: String): String {
        val fileName = "media_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.${if (mediaType == "image") "jpg" else "mp4"}"
        println("AnalysisRepositoryImpl: Saving media file: $fileName (${mediaBytes.size} bytes)")

        // Save media file to DataStore for testing
        dataStore.edit { preferences ->
            val currentFilesJson = preferences[MEDIA_FILES_KEY] ?: "{}"
            val currentFiles = try {
                json.decodeFromString<Map<String, String>>(currentFilesJson)
            } catch (e: Exception) {
                emptyMap()
            }

            // Convert bytes to hex string for storage
            val base64Data = mediaBytes.joinToString("") { byte ->
                byte.toUByte().toString(16).padStart(2, '0')
            }
            val updatedFiles = currentFiles + (fileName to base64Data)
            preferences[MEDIA_FILES_KEY] = json.encodeToString(updatedFiles)
        }

        println("AnalysisRepositoryImpl: Media file saved successfully: $fileName")
        return fileName
    }

    override suspend fun loadMediaFile(mediaPath: String): ByteArray? {
        println("AnalysisRepositoryImpl: Loading media file: $mediaPath")
        return try {
            val preferences = dataStore.data.first()
            val filesJson = preferences[MEDIA_FILES_KEY] ?: "{}"
            val files = json.decodeFromString<Map<String, String>>(filesJson)

            val hexData = files[mediaPath]
            if (hexData != null) {
                // Convert hex string back to bytes
                val bytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                println("AnalysisRepositoryImpl: Media file loaded successfully: $mediaPath (${bytes.size} bytes)")
                bytes
            } else {
                println("AnalysisRepositoryImpl: Media file not found: $mediaPath")
                null
            }
        } catch (e: Exception) {
            println("AnalysisRepositoryImpl: Error loading media file: ${e.message}")
            null
        }
    }

    override suspend fun analyzeMediaWithAI(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        return openAIService.analyzeMedia(mediaBytes, apiKey)
    }
}