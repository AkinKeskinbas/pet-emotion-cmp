package com.keak.petemotions.utils.di

import com.keak.petemotions.data.api.BackendApiService
import com.keak.petemotions.data.repository.AnalysisRepository
import com.keak.petemotions.data.repository.AnalysisRepositoryImpl
import com.keak.petemotions.data.repository.PetRepository
import com.keak.petemotions.data.repository.PetRepositoryImpl
import com.keak.petemotions.data.repository.PreferencesRepository
import com.keak.petemotions.data.repository.PreferencesRepositoryImpl
import com.keak.petemotions.data.repository.createDataStore
import com.keak.petemotions.data.service.CoinService
import com.keak.petemotions.data.service.CoinServiceImpl
import com.keak.petemotions.data.service.RevenueCatService
import com.keak.petemotions.data.service.RevenueCatServiceImpl
import com.keak.petemotions.platform.PermissionService
import com.keak.petemotions.platform.createPermissionService
import com.keak.petemotions.presentation.viewmodel.AddEditPetViewModel
import com.keak.petemotions.presentation.viewmodel.CameraViewModel
import com.keak.petemotions.presentation.viewmodel.CompareViewModel
import com.keak.petemotions.presentation.viewmodel.HomeViewModel
import com.keak.petemotions.presentation.viewmodel.MyPetsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { createDataStore() }

    single<PetRepository> { PetRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<PermissionService> { createPermissionService() }
    single { BackendApiService() }

    // RevenueCat service - KMP implementation
    single<RevenueCatService> { RevenueCatServiceImpl() }
    single<CoinService> {
        val coinService = CoinServiceImpl(get())
        // Initialize coin service with data from RevenueCat on startup when first accessed
        coinService
    }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { CameraViewModel(get(), get(), get(), get(), get()) }
    viewModel { CompareViewModel(get(), get(), get()) }
    viewModel { MyPetsViewModel(get()) }
    viewModel { (petId: String?) -> AddEditPetViewModel(petId, get(), get()) }
}
