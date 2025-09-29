package com.keak.petemotions.data.storage

/**
 * Simple abstraction over platform-specific media file persistence used by analysis workflows.
 */
interface MediaStorage {
    suspend fun save(bytes: ByteArray, extension: String): String
    suspend fun load(path: String): ByteArray?
}
