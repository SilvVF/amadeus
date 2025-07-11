package io.silv.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.silv.common.log.logcat
import io.silv.common.model.AppTheme
import io.silv.common.model.AutomaticUpdatePeriod
import io.silv.common.model.CardType
import io.silv.common.model.ReaderLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SettingsStore(
    dataStore: DataStore<Preferences>
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val store = dataStore

    val initialized = MutableStateFlow(false)

    private val initialValues: Preferences by lazy {
        runBlocking { store.data.first() }
    }

    fun load() = scope.launch {
        initialValues.let {
            initialized.value = true
        }
    }

    val theme by preferenceStateFlow(Keys.theme, AppTheme.SYSTEM_DEFAULT) { AppTheme.entries[it] }
    val updateInterval by preferenceStateFlow(Keys.updateInterval, AutomaticUpdatePeriod.Off) { AutomaticUpdatePeriod.entries[it] }

    val libraryGridCells by preferenceStateFlow(Keys.LibraryPrefs.gridCellsPrefKey, 2)
    val libraryCardType by preferenceStateFlow(Keys.LibraryPrefs.cardTypePrefKey, CardType.Compact) { CardType.entries[it] }
    val libraryAnimatePlacementPrefKey by preferenceStateFlow(Keys.LibraryPrefs.animatePlacementPrefKey, true)
    val libraryUseList by preferenceStateFlow(Keys.LibraryPrefs.useListPrefKey, false)

    val filterGridCells by preferenceStateFlow(Keys.FilterPrefs.gridCellsPrefKey, 2)
    val filterCardType by preferenceStateFlow(Keys.FilterPrefs.cardTypePrefKey, CardType.Compact) { CardType.entries[it] }
    val filterUseList by preferenceStateFlow(Keys.FilterPrefs.useListPrefKey, false)

    val exploreGridCells by preferenceStateFlow(Keys.ExplorePrefs.gridCellsPrefKey, 2)
    val exploreCardType by preferenceStateFlow(Keys.ExplorePrefs.cardTypePrefKey, CardType.Compact) { CardType.entries[it] }
    val exploreUseList by preferenceStateFlow(Keys.ExplorePrefs.useListPrefKey, false)

    val readingMode by preferenceStateFlow(Keys.ReaderPrefs.defaultReadingMode, 0)
    val orientationType by preferenceStateFlow(Keys.ReaderPrefs.defaultOrientationType, 0)
    val removeAfterReadSlots by preferenceStateFlow(Keys.ReaderPrefs.removeAfterReadSlots, -1)
    val layoutDirection by preferenceStateFlow(Keys.ReaderPrefs.layoutDirection, ReaderLayout.PagedLTR) { ReaderLayout.entries[it] }
    val showPageNumber by preferenceStateFlow(Keys.ReaderPrefs.showPageNumber, true)
    val backgroundColor by preferenceStateFlow(Keys.ReaderPrefs.backgroundColor, 0x000000)
    val fullscreen by preferenceStateFlow(Keys.ReaderPrefs.fullscreen, false)

    suspend fun edit(
        transform: suspend (MutablePreferences) -> Unit
    ): Preferences? {
        return try {
            store.edit(transform)
        } catch (e: Exception) {
            logcat { e.stackTraceToString() }
            null
        }
    }

    private fun <KeyType> preferenceStateFlow(
        key: Preferences.Key<KeyType>,
        defaultValue: KeyType,
    ): Lazy<StateFlow<KeyType>> {
        return preferenceStateFlow(key, defaultValue) { it }
    }

    private fun <KeyType, StateType> preferenceStateFlow(
        key: Preferences.Key<KeyType>,
        defaultValue: StateType,
        transform: ((KeyType) -> StateType?),
    ): Lazy<StateFlow<StateType>> = lazy {
        val initialValue = initialValues[key]?.let(transform) ?: defaultValue
        val stateFlow = MutableStateFlow(initialValue)
        scope.launch {
            store.data
                .map { preferences -> preferences[key]?.let(transform) ?: defaultValue }
                .collect(stateFlow::emit)
        }
        stateFlow
    }
}