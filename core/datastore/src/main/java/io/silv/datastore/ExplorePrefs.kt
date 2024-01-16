package io.silv.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey


object ExplorePrefs {
    val gridCellsPrefKey = intPreferencesKey("io.silv.explore_grid_cells_key")
    const val gridCellsDefault = 2

    val cardTypePrefKey = stringPreferencesKey("io.silv.explore_card_types_key")

    val useListPrefKey = booleanPreferencesKey("io.silv.explore_use_list")
}


