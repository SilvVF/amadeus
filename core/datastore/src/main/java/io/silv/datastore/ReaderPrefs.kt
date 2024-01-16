package io.silv.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object ReaderPrefs {

    val layoutDirection = intPreferencesKey("io.silv.reader_reverse_layout")

    val readerOrientation = stringPreferencesKey("io.silv.reader_reverse_layout")

    val showPageNumber = booleanPreferencesKey("io.silv.reader.show_page_number")

    val backgroundColor = intPreferencesKey("io.silv.reader.background_color")

    val fullscreen = booleanPreferencesKey("io.silv.reader.fullscreen")

    val scaleType = intPreferencesKey("io.silv.reader.scale_type")
}
