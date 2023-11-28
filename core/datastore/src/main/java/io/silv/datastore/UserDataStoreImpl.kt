package io.silv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.silv.common.coroutine.suspendRunCatching
import io.silv.datastore.model.Filters
import io.silv.datastore.model.ReaderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class UserSettingsStoreImpl(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val dispatchers: io.silv.common.AmadeusDispatchers
): UserSettingsStore {

    override suspend fun updateDefaultFilter(filters: Filters): Boolean = suspendRunCatching {
        withContext(dispatchers.io) {
            dataStore.edit { prefs ->
                prefs[defaultFiltersKey] = json.encodeToString(
                    Filters.serializer(),
                    filters
                )
            }
        }
    }
        .isSuccess


    override suspend fun updateReaderSettings(readerSettings: ReaderSettings) = suspendRunCatching {
        withContext(dispatchers.io) {
            dataStore.edit { prefs ->
                prefs[readerSettingsKey] = json.encodeToString(
                    ReaderSettings.serializer(),
                    readerSettings
                )
            }
        }
    }
        .isSuccess

    override fun observeDefaultFilter(): Flow<Filters> {
        return dataStore.data.map { prefs ->
            json.decodeFromString(
                prefs[defaultFiltersKey] ?: return@map Filters()
            )
        }
            .flowOn(dispatchers.io)
    }

    override fun observeReaderSettings(): Flow<ReaderSettings> {
        return dataStore.data.map { prefs ->
            json.decodeFromString(
                prefs[readerSettingsKey] ?: return@map ReaderSettings()
            )
        }
            .flowOn(dispatchers.io)
    }

    override fun observeLanguage(): Flow<String> {
        return dataStore.data.map { prefs ->
            prefs[readerSettingsKey] ?: "en"
        }
            .flowOn(dispatchers.io)
    }

    override suspend fun updateLanguage(code: String): Boolean = suspendRunCatching {
        withContext(dispatchers.io) {
            dataStore.edit { prefs ->
                prefs[languageCodeKey] = code
            }
        }
    }
        .isSuccess


    companion object {
        private val defaultFiltersKey = stringPreferencesKey("default_filter_key")
        private val languageCodeKey = stringPreferencesKey("language_code_key")
        private val readerSettingsKey = stringPreferencesKey("reader_settings_key")
    }
}