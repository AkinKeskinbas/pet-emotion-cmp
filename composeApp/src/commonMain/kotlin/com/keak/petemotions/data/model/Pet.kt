package com.keak.petemotions.data.model

import com.benasher44.uuid.uuid4
import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    val name: String,
    val breed: String? = null,
    val age: Int? = null,
    val notes: String? = null,
    val avatarPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun create(
            name: String,
            breed: String? = null,
            age: Int? = null,
            notes: String? = null,
            avatarPath: String? = null,
            timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        ): Pet {
            return Pet(
                id = uuid4().toString(),
                name = name,
                breed = breed,
                age = age,
                notes = notes,
                avatarPath = avatarPath,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
