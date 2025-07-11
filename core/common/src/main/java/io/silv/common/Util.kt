package io.silv.common

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> mutablePropertyFrom(stateFlow: MutableStateFlow<T>) = object :
    ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = stateFlow.value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        stateFlow.value = value
    }
}


fun Channel<*>.drain() {
    do {
        val result = tryReceive()
    } while (result.isSuccess)
}

fun createTrigger(
    vararg flows: Flow<*>,
    emitAtStart: Boolean = true,
) = channelFlow<Unit> {
    for (flow in flows) {
        launch {
            flow.collect {
                send(Unit)
            }
        }
    }
}.run {
    if (emitAtStart) {
        onStart { emit(Unit) }
    } else {
        this
    }
}

@OptIn(ExperimentalContracts::class)
suspend inline fun <T, R> Iterable<T>.pmap(crossinline transform: suspend (T) -> R): List<R> {
    return buildList {
        coroutineScope {
            for (item in this@pmap) {
                add(
                    async { transform(item) },
                )
            }
        }
    }
        .awaitAll()
}

fun <T, R> Iterable<T>.filterUnique(item: (T) -> R): List<T> {
    val seen = mutableSetOf<R>()
    return buildList {
        for (e in this@filterUnique) {
            if (seen.add(item(e))) {
                add(e)
            }
        }
    }
}
