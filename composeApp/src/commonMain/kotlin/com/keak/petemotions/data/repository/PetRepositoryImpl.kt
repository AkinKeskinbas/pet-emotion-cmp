package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.keak.petemotions.data.model.Pet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PetRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PetRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val PETS_KEY = stringPreferencesKey("pets_list")
    }

    override fun getAllPets(): Flow<List<Pet>> {
        return dataStore.data.map { preferences ->
            val petsJson = preferences[PETS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<Pet>>(petsJson)
            } catch (e: Exception) {
                emptyList()
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getPetById(id: String): Pet? = withContext(Dispatchers.Default) {
        return@withContext dataStore.data.map { preferences ->
            val petsJson = preferences[PETS_KEY] ?: "[]"
            try {
                val pets = json.decodeFromString<List<Pet>>(petsJson)
                pets.find { it.id == id }
            } catch (e: Exception) {
                null
            }
        }.first()
    }

    override suspend fun insertPet(pet: Pet) {
        try {
            dataStore.edit { preferences ->
                val currentPetsJson = preferences[PETS_KEY] ?: "[]"
                val currentPets = try {
                    json.decodeFromString<List<Pet>>(currentPetsJson)
                } catch (e: Exception) {
                    emptyList()
                }

                val updatedPets = currentPets + pet
                try {
                    preferences[PETS_KEY] = json.encodeToString(updatedPets)
                } catch (e: Exception) {
                    throw Exception("Failed to serialize pet data: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to save pet: ${e.message}")
        }
    }

    override suspend fun updatePet(pet: Pet) {
        dataStore.edit { preferences ->
            val currentPetsJson = preferences[PETS_KEY] ?: "[]"
            val currentPets = try {
                json.decodeFromString<List<Pet>>(currentPetsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPets = currentPets.map { if (it.id == pet.id) pet else it }
            preferences[PETS_KEY] = json.encodeToString(updatedPets)
        }
    }

    override suspend fun deletePet(pet: Pet) {
        dataStore.edit { preferences ->
            val currentPetsJson = preferences[PETS_KEY] ?: "[]"
            val currentPets = try {
                json.decodeFromString<List<Pet>>(currentPetsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPets = currentPets.filter { it.id != pet.id }
            preferences[PETS_KEY] = json.encodeToString(updatedPets)
        }
    }

    override suspend fun deletePetById(id: String) {
        dataStore.edit { preferences ->
            val currentPetsJson = preferences[PETS_KEY] ?: "[]"
            val currentPets = try {
                json.decodeFromString<List<Pet>>(currentPetsJson)
            } catch (e: Exception) {
                emptyList()
            }

            val updatedPets = currentPets.filter { it.id != id }
            preferences[PETS_KEY] = json.encodeToString(updatedPets)
        }
    }

    override suspend fun getPetCount(): Int {
        return dataStore.data.map { preferences ->
            val petsJson = preferences[PETS_KEY] ?: "[]"
            try {
                val pets = json.decodeFromString<List<Pet>>(petsJson)
                pets.size
            } catch (e: Exception) {
                0
            }
        }.first()
    }
}