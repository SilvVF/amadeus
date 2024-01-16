package io.silv.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey



object FilterPrefs {
    val gridCellsPrefKey = intPreferencesKey("io.silv.filter_grid_cells_key")
    const val gridCellsDefault = 2

    val cardTypePrefKey = stringPreferencesKey("io.silv.filter_card_types_key")

    val useListPrefKey = booleanPreferencesKey("io.silv.filter_use_list")
}
