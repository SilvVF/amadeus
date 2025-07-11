package io.silv.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.io.path.Path

class PreferenceMutableState<K, S>(
    private val settingsStore: SettingsStore,
    private val key: Preferences.Key<K>,
    private val transform: (S) -> K,
    private val flow: StateFlow<S>,
    private val scope: CoroutineScope,
) : MutableState<S> {

    private val state = mutableStateOf(flow.value)

    init {
        scope.launch {
            flow.collect { state.value = it }
        }
    }

    override var value: S
        get() = state.value
        set(value) {
            scope.launch {
                settingsStore.edit { prefs ->
                    prefs[key] = transform(value)
                }
            }
        }

    override fun component1(): S = state.value

    override fun component2(): (S) -> Unit = { value = it }
}
