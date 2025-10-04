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
import com.keak.petemotions.platform.CameraService
import com.keak.petemotions.platform.createCameraService
import com.keak.petemotions.presentation.viewmodel.*
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { createDataStore() }

    single<PetRepository> { PetRepositoryImpl(get()) }
    single<AnalysisRepository> { AnalysisRepositoryImpl(get(), get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
   // single<UserPrefsRepository> { UserPrefsRepositoryImpl(get()) }
    single<PermissionService> { createPermissionService() }
    single<CameraService> { createCameraService() }
    single { BackendApiService() }

    // RevenueCat service - KMP implementation
    single<RevenueCatService> { RevenueCatServiceImpl() }
    single<CoinService> {
        val coinService = CoinServiceImpl(get())
        // Initialize coin service with data from RevenueCat on startup when first accessed
        coinService
    }

    // ViewModels
    viewModel { SplashViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { CameraViewModel(get(), get(), get(), get(), get()) }
    viewModel { CompareViewModel(get(), get(), get()) }
    viewModel { MyPetsViewModel(get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { (petId: String?) -> AddEditPetViewModel(petId, get(), get()) }
    viewModel { (analysisRecordId: String) -> ResultDetailViewModel(analysisRecordId, get()) }
}
