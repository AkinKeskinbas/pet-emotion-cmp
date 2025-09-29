package com.keak.petemotions.data.repository

import com.keak.petemotions.data.model.AnalysisRecord
import com.keak.petemotions.data.model.AnalysisResult
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    fun getAllAnalysisRecords(): Flow<List<AnalysisRecord>>
    suspend fun getAnalysisRecordById(id: String): AnalysisRecord?
    fun getAnalysisRecordsByPetId(petId: String): Flow<List<AnalysisRecord>>
    fun getAnalysisRecordsByEmotion(emotion: String): Flow<List<AnalysisRecord>>
    suspend fun getAnalysisRecordsBetween(startTime: Long, endTime: Long): List<AnalysisRecord>
    suspend fun getAnalysisRecordsByPetBetween(petId: String, startTime: Long, endTime: Long): List<AnalysisRecord>
    suspend fun getEmotionDistribution(startTime: Long, endTime: Long): Map<String, Int>
    suspend fun getEmotionDistributionByPet(petId: String, startTime: Long, endTime: Long): Map<String, Int>
    suspend fun insertAnalysisRecord(record: AnalysisRecord)
    suspend fun updateAnalysisRecord(record: AnalysisRecord)
    suspend fun deleteAnalysisRecord(record: AnalysisRecord)
    suspend fun deleteAnalysisRecordById(id: String)
    suspend fun deleteAnalysisRecordsByPetId(petId: String)
    suspend fun getAnalysisRecordCount(): Int
    suspend fun getAnalysisRecordCountByPet(petId: String): Int

    // Media and AI analysis
    suspend fun saveMediaFile(mediaBytes: ByteArray, mediaType: String): String
    suspend fun loadMediaFile(mediaPath: String): ByteArray?
    suspend fun analyzeMediaWithAI(mediaBytes: ByteArray, apiKey: String): Result<AnalysisResult>
}