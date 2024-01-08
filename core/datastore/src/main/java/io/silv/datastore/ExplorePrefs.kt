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


object FilterPrefs {
    val gridCellsPrefKey = intPreferencesKey("io.silv.filter_grid_cells_key")
    const val gridCellsDefault = 2

    val cardTypePrefKey = stringPreferencesKey("io.silv.filter_card_types_key")

    val useListPrefKey = booleanPreferencesKey("io.silv.filter_use_list")
}

object LibraryPrefs {
    val gridCellsPrefKey = intPreferencesKey("io.silv.lib_grid_cells_key")
    const val gridCellsDefault = 2

    val cardTypePrefKey = stringPreferencesKey("io.silv.lib_card_types_key")

    val animatePlacementPrefKey = booleanPreferencesKey("io.silv.animate_placement_key")

    val useListPrefKey = booleanPreferencesKey("io.silv.library_use_list")
}