package com.keak.petemotions.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.native.concurrent.ThreadLocal

private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

@ThreadLocal
private var dataStoreInstance: DataStore<Preferences>? = null

@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(): DataStore<Preferences> {
    dataStoreInstance?.let { return it }

    val store = PreferenceDataStoreFactory.createWithPath(
        scope = dataStoreScope,
        produceFile = { providePreferencesPath() }
    )

    dataStoreInstance = store
    return store
}

@OptIn(ExperimentalForeignApi::class)
private fun providePreferencesPath(): Path {
    return try {
        val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        val basePath = requireNotNull(documentDirectory).path!!
        "$basePath/pet_emotions_prefs.preferences_pb".toPath()
    } catch (e: Exception) {
        "/tmp/pet_emotions_prefs.preferences_pb".toPath()
    }
}
