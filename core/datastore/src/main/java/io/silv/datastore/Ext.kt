package io.silv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlin.jvm.Throws

@Throws(IOException::class, Exception::class)
suspend fun <T> DataStore<Preferences>.set(key: Preferences.Key<T>, value: T) {
    updateData { prefs ->
        val mutPrefs =  prefs.toMutablePreferences()
        mutPrefs[key] = value
        mutPrefs
    }
}

suspend fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? {
    return data.map { prefs ->
        prefs[key]
    }
        .firstOrNull()
}