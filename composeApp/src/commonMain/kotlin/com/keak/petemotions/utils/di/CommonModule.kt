package com.keak.petemotions.utils.di

import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.AnalysisRepositoryImpl
import com.keak.petemotions.data.repository.OpenAIService
import com.keak.petemotions.data.repository.OpenAIServiceImpl
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PetRepositoryImpl
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.data.repository.PreferencesRepositoryImpl
import com.keak.petemotions.data.repository.createDataStore
import com.keak.petemotions.platform.PermissionService
import com.keak.petemotions.platform.createPermissionService
import com.keak.petemotions.presentation.viewmodel.AddEditPetViewModel
import com.keak.petemotions.presentation.viewmodel.CameraViewModel
import com.keak.petemotions.presentation.viewmodel.HomeViewModel
import com.keak.petemotions.presentation.viewmodel.MyPetsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { createDataStore() }

    single<OpenAIService> { OpenAIServiceImpl() }
    single<PetRepository> { PetRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<PermissionService> { createPermissionService() }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { CameraViewModel(get(), get(), get(), get()) }
    viewModel { MyPetsViewModel(get()) }
    viewModel { (petId: String?) -> AddEditPetViewModel(petId, get()) }
}
