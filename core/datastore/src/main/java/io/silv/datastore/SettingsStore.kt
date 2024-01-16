package io.silv.datastore

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.silv.common.model.AutomaticUpdatePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsStore(context: Context) {

    private val Context.store: DataStore<Preferences> by preferencesDataStore(
        name = "settings_store"
    )
    private val datastore = context.store

    suspend fun update(new: (UserSettings) -> UserSettings): Boolean = runCatching {
        datastore.edit {
            it[settingsKey] = encode(
                new(
                    it[settingsKey]?.let(::decode) ?: UserSettings()
                )
            )
                ?: return@edit
        }
    }
        .isSuccess

    fun observe(): Flow<UserSettings> = datastore.data.map {
        it[settingsKey]
            ?.let(::decode)
            ?: UserSettings()
    }

    private fun encode(settings: UserSettings): String? {
        return try {
            Json.encodeToString(settings)
        }  catch (e: SerializationException) {
            e.printStackTrace()
            null
        }
    }

    private fun decode(string: String): UserSettings {
        return try {
            Json.decodeFromString(string)
        } catch (e: SerializationException) {
            e.printStackTrace()
            return UserSettings()
        }
    }

    companion object {
        private val settingsKey = stringPreferencesKey("settings_key")
    }
}

@Stable
@Serializable
data class UserSettings(
    val updateInterval: AutomaticUpdatePeriod = AutomaticUpdatePeriod.Off
)