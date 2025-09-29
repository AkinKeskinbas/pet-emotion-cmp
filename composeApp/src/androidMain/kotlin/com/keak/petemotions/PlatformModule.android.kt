package com.keak.petemotions

import com.keak.petemotions.data.storage.AndroidMediaStorage
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.CameraService
import com.keak.petemotions.platform.createCameraService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModuleWithContext(): Module = module {
    single<MediaStorage> {
        AndroidMediaStorage(androidContext())
    }
    single<CameraService> { createCameraService() }
}
