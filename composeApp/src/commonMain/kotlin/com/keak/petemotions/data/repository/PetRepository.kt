package com.keak.petemotions.data.repository

import com.keak.petemotions.data.model.Pet
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    fun getAllPets(): Flow<List<Pet>>
    suspend fun getPetById(id: String): Pet?
    suspend fun insertPet(pet: Pet)
    suspend fun updatePet(pet: Pet)
    suspend fun deletePet(pet: Pet)
    suspend fun deletePetById(id: String)
    suspend fun getPetCount(): Int
}