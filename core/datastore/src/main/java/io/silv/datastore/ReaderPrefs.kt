package io.silv.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object ReaderPrefs {

    val defaultReadingMode = intPreferencesKey("io.silv.reader.reading_mode_key")

    val defaultOrientationType = intPreferencesKey("io.silv.reader.reader_orientation_key")

    val removeAfterReadSlots = intPreferencesKey("io.silv.reader.remove_after_read")

    val layoutDirection = intPreferencesKey("io.silv.reader_reverse_layout")

    val showPageNumber = booleanPreferencesKey("io.silv.reader.show_page_number")

    val backgroundColor = intPreferencesKey("io.silv.reader.background_color")

    val fullscreen = booleanPreferencesKey("io.silv.reader.fullscreen")
}
