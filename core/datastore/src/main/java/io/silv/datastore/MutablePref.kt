package io.silv.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.silv.common.PrefsConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PreferenceMutableState<T>(
    defaultValue: T,
    private val dataStore: DataStore<Preferences>,
    private val key: Preferences.Key<T>,
    private val scope: CoroutineScope,
) : MutableState<T> {

    private val state =
        mutableStateOf(defaultValue)

    init {
        dataStore.data.map { it[key] }
            .onEach {
                state.value = it ?: return@onEach
            }
            .launchIn(scope)
    }

    override var value: T
        get() = state.value
        set(value) {
            scope.launch {
                dataStore.set(key, value)
            }
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = { value = it }
}

class PreferenceMutableStateWithConversion<T, V>(
    defaultValue: T,
    private val dataStore: DataStore<Preferences>,
    private val converter: PrefsConverter<T, V>,
    private val key: Preferences.Key<V>,
    private val scope: CoroutineScope,
) : MutableState<T>  {

    private val state =
        mutableStateOf(defaultValue)

    init {
        dataStore.data.map { it[key] }
            .onEach {
                state.value = converter.convertFrom(it ?: return@onEach)
            }
            .launchIn(scope)
    }

    override var value: T
        get() = state.value
        set(value) {
            scope.launch {
                dataStore.set(key, converter.convertTo(value))
            }
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = { value = it }
}

fun <T> Preferences.Key<T>.prefAsState(
    dataStore: DataStore<Preferences>,
    defaultValue: T,
    scope: CoroutineScope,
) = PreferenceMutableState(defaultValue, dataStore, this, scope)

fun <T, V> Preferences.Key<V>.prefAsState(
    dataStore: DataStore<Preferences>,
    defaultValue: T,
    converter: PrefsConverter<T, V>,
    scope: CoroutineScope,
) = PreferenceMutableStateWithConversion(defaultValue, dataStore, converter, this, scope)

@Composable
fun <T> Preferences.Key<T>.collectPrefAsState(
    dataStore: DataStore<Preferences>,
    defaultValue: T,
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    PreferenceMutableState(defaultValue, dataStore, this, scope)
}

@Composable
fun <T, V> Preferences.Key<V>.collectPrefAsState(
    dataStore: DataStore<Preferences>,
    defaultValue: T,
    converter: PrefsConverter<T, V>,
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    PreferenceMutableStateWithConversion(defaultValue, dataStore, converter, this, scope)
}
