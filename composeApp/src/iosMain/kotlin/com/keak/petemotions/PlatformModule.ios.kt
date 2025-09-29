package com.keak.petemotions

import com.keak.petemotions.data.storage.IOSMediaStorage
import com.keak.petemotions.data.storage.MediaStorage
import com.keak.petemotions.platform.CameraService
import com.keak.petemotions.platform.createCameraService
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getPlatformModuleWithContext(): Module = module {
    single<MediaStorage> { IOSMediaStorage() }
    single<CameraService> { createCameraService() }
}
