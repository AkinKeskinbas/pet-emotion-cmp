package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.AnalysisResult
import kotlinx.coroutines.flow.Flow
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
        return getAllAnalysisRecords().map { records ->
            records.find { it.id == id }
        }.let { flow ->
            var result: AnalysisRecord? = null
            flow.collect { result = it }
            result
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
        // For now, we'll generate a unique filename but not actually save to file system
        // This would be handled by MediaStorage in a real implementation
        val fileName = "media_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}.${if (mediaType == "image") "jpg" else "mp4"}"
        return fileName
    }

    override suspend fun loadMediaFile(mediaPath: String): ByteArray? {
        // This would load from MediaStorage in a real implementation
        return null
    }

    override suspend fun analyzeMediaWithAI(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult> {
        return openAIService.analyzeMedia(mediaBytes, apiKey)
    }
}