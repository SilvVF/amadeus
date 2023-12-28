package io.silv.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MangaDexUserStore(
    private val dataStore: DataStore<Preferences>,
) {

    fun observeUserIds(): Flow<List<String>> =
        dataStore.data.map {
            it[userListPrefKey]?.let(::decode) ?: emptyList()
        }

    suspend fun updateUserIds(block: (List<String>) -> List<String>) {
        dataStore.edit {
            it[userListPrefKey] =
                block(
                    it[userListPrefKey]?.let(::decode) ?: emptyList()
                )
                    .let(::encode)
        }
    }


    companion object {

        private fun decode(string: String): List<String> = Json.decodeFromString(string)

        private fun encode(list: List<String>): String = Json.encodeToString(list)

        @Composable
        fun collectUserIdsAsState(): MutableState<List<String>> {
            return userListPrefKey.collectAsState(
                defaultValue = emptyList(),
                convert = ::decode,
                store = ::encode
            )
        }

        private val userListPrefKey = stringPreferencesKey("USER_LIST_PREF_KEY")
    }
}
