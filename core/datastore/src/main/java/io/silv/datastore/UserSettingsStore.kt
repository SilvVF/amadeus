package io.silv.datastore

import io.silv.datastore.model.Filters
import io.silv.datastore.model.ReaderSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsStore {

    suspend fun updateDefaultFilter(filters: Filters): Boolean

    suspend fun updateReaderSettings(readerSettings: ReaderSettings): Boolean

    fun observeDefaultFilter(): Flow<Filters>

    fun observeReaderSettings(): Flow<ReaderSettings>

    fun observeLanguage(): Flow<String>

    suspend fun updateLanguage(code: String): Boolean
}