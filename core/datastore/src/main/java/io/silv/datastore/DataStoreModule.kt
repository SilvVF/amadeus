package io.silv.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import io.silv.common.DependencyAccessor

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@DependencyAccessor
lateinit var dataStoreDeps: DataStoreModule

interface DataStoreModule {

     val dataStore: DataStore<Preferences>

     val downloadStore: DownloadStore

     val settingsStore: SettingsStore
}

class DataStoreModuleImpl(context: Context): DataStoreModule {

    override val dataStore: DataStore<Preferences> = context.dataStore

    override val settingsStore: SettingsStore by lazy {
        SettingsStore(context)
    }

    override val downloadStore: DownloadStore by lazy {
        DownloadStore(context)
    }
}
