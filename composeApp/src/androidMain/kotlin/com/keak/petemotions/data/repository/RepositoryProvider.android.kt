package com.keak.petemotions.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.mp.KoinPlatform.getKoin

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pet_emotions_prefs")

actual fun createDataStore(): DataStore<Preferences> {
    val context = getKoin().get<Context>()
    return context.dataStore
}