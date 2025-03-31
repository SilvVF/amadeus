package io.silv.datastore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import io.silv.common.PrefsConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PreferenceMutableState<T>(
    defaultValue: T,
    private val key: Preferences.Key<T>,
    private val scope: CoroutineScope,
) : MutableState<T>, KoinComponent {

    private val dataStore by inject<DataStore<Preferences>>()

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
                dataStore.edit {
                    it[key] = value
                }
            }
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = { value = it }
}

class PreferenceMutableStateWithConversion<T, V>(
    defaultValue: T,
    private val converter: PrefsConverter<T, V>,
    private val key: Preferences.Key<V>,
    private val scope: CoroutineScope,
) : MutableState<T>, KoinComponent {
    private val dataStore by inject<DataStore<Preferences>>()

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
                dataStore.edit {
                    it[key] = converter.convertTo(value)
                }
            }
        }

    override fun component1(): T = state.value

    override fun component2(): (T) -> Unit = { value = it }
}

fun <T> Preferences.Key<T>.asState(
    defaultValue: T,
    scope: CoroutineScope,
) = PreferenceMutableState(defaultValue, this, scope)

fun <T, V> Preferences.Key<V>.asState(
    defaultValue: T,
    converter: PrefsConverter<T, V>,
    scope: CoroutineScope,
) = PreferenceMutableStateWithConversion(defaultValue, converter, this, scope)

@Composable
fun <T> Preferences.Key<T>.collectAsState(
    defaultValue: T,
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    PreferenceMutableState(defaultValue, this, scope)
}

@Composable
fun <T, V> Preferences.Key<V>.collectAsState(
    defaultValue: T,
    converter: PrefsConverter<T, V>,
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember {
    PreferenceMutableStateWithConversion(defaultValue, converter, this, scope)
}
