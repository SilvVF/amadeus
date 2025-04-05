package io.silv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

suspend fun <T> DataStore<Preferences>.set(key: Preferences.Key<T>, value: T) {
    edit { prefs ->
        prefs[key] = value
    }
}

suspend fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    return data.map { prefs ->
        prefs[key]
    }
        .firstOrNull()
}