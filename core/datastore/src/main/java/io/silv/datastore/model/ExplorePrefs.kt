package io.silv.datastore.model

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object ExplorePrefs {
    val gridCellsPrefKey = intPreferencesKey("io.silv.grid_cells_key")
    const val gridCellsDefault = 2

    val cardTypePrefKey = stringPreferencesKey("io.silv.card_types_key")

    val showSeasonalListPrefKey = booleanPreferencesKey("io.silv.show_seasonal_lists")
    const val showSeasonalDefault = false
}

object ReaderPrefs {
    val layoutDirection = intPreferencesKey("io.silv.reader_reverse_layout")
    const val reverseLayoutDefault = 1
}

