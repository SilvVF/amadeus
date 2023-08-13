package io.silv.amadeus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.silv.manga.domain.suspendRunCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")



interface UserSettingsStore {

    suspend fun updateReaderSettings(readerSettings: ReaderSettings): Boolean

    fun observeReaderSettings(): Flow<ReaderSettings>
}


class UserSettingsStoreImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
): UserSettingsStore {

    override suspend fun updateReaderSettings(readerSettings: ReaderSettings) = suspendRunCatching {
        dataStore.edit { prefs ->
            prefs[readerSettingsKey] = json.encodeToString(
                ReaderSettings.serializer(),
                readerSettings
            )
        }
    }
        .isSuccess

    override fun observeReaderSettings(): Flow<ReaderSettings> {
        return dataStore.data.map { prefs ->
            json.decodeFromString(
                prefs[readerSettingsKey] ?: return@map ReaderSettings()
            )
        }
    }


    companion object {
        private val readerSettingsKey = stringPreferencesKey("reader_settings_key")
    }
}