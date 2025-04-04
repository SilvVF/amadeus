package io.silv.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.common.DependencyAccessor

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@DependencyAccessor
lateinit var dataStoreDeps: DataStoreDependencies

abstract class DataStoreDependencies {

    abstract val context: Application

    val dataStore: DataStore<Preferences> = context.dataStore

    val settingsStore: SettingsStore by lazy {
        SettingsStore(context)
    }

    val downloadStore: DownloadStore by lazy {
        DownloadStore(context)
    }
}
