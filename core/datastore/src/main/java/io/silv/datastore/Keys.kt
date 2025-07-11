package io.silv.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Keys {

    const val GRID_CELLS_DEFAULT = 2

    val updateInterval = intPreferencesKey("io.silv.app.update_interval")
    val theme = intPreferencesKey("io.silv.app.theme")

    object LibraryPrefs {
        val gridCellsPrefKey = intPreferencesKey("io.silv.lib_grid_cells_key")

        val cardTypePrefKey = intPreferencesKey("io.silv.lib_card_types_key_int")

        val animatePlacementPrefKey = booleanPreferencesKey("io.silv.animate_placement_key")

        val useListPrefKey = booleanPreferencesKey("io.silv.library_use_list")
    }

    object FilterPrefs {
        val gridCellsPrefKey = intPreferencesKey("io.silv.filter_grid_cells_key")

        val cardTypePrefKey = intPreferencesKey("io.silv.filter_card_types_key_int")

        val useListPrefKey = booleanPreferencesKey("io.silv.filter_use_list")
    }

    object ExplorePrefs {
        val gridCellsPrefKey = intPreferencesKey("io.silv.explore_grid_cells_key")

        val cardTypePrefKey = intPreferencesKey("io.silv.explore_card_types_key_int")

        val useListPrefKey = booleanPreferencesKey("io.silv.explore_use_list")
    }

    object ReaderPrefs {

        val defaultReadingMode = intPreferencesKey("io.silv.reader.reading_mode_key")

        val defaultOrientationType = intPreferencesKey("io.silv.reader.reader_orientation_key")

        val removeAfterReadSlots = intPreferencesKey("io.silv.reader.remove_after_read")

        val layoutDirection = intPreferencesKey("io.silv.reader_reverse_layout")

        val showPageNumber = booleanPreferencesKey("io.silv.reader.show_page_number")

        val backgroundColor = intPreferencesKey("io.silv.reader.background_color_int")

        val fullscreen = booleanPreferencesKey("io.silv.reader.fullscreen")
    }
}