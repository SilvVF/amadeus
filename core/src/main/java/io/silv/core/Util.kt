package io.silv.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
suspend inline fun <T, R> Iterable<T>.pmap(
    crossinline transform: suspend (T) -> R
): List<R> {
    contract { callsInPlace(transform) }
    val list = this
    return buildList {
        coroutineScope {
            for (item in list) {
                add(
                    async { transform(item) }
                )
            }
        }
    }
        .awaitAll()
}
@OptIn(ExperimentalContracts::class)
suspend inline fun <T, R> Array<T>.pmapIndexed(
    crossinline transform: suspend (Int, T) -> R
): List<R> {
    contract { callsInPlace(transform) }
    val list = this
    return buildList {
        coroutineScope {
            for ((i, item) in list.withIndex()) {
                add(
                    async { transform(i, item) }
                )
            }
        }
    }
        .awaitAll()
}

suspend inline fun <K, V, R, C : MutableCollection<in R>> Map<out K, V>.mapToSuspend(destination: C, transform: suspend (Map.Entry<K, V>) -> R): C {
    for (item in this)
        destination.add(transform(item))
    return destination
}

suspend inline fun <K, V, R> Map<out K, V>.mapSuspend(
    crossinline transform: suspend (Map.Entry<K, V>) -> R
): List<R> {
    return mapToSuspend(ArrayList(size), transform)
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

@OptIn(ExperimentalContracts::class)
fun <T> Iterable<T>.pForEach(
    scope: CoroutineScope,
    action: suspend (T) -> Unit
) {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    for (item in this) {
        scope.launch {
            action(item)
        }
    }
}

@OptIn(ExperimentalContracts::class)
suspend fun <T> Iterable<T>.pForEach(
    action: suspend (T) -> Unit
) {
    contract { callsInPlace(action) }
    coroutineScope {
        for (item in this@pForEach) {
            launch {
                action(item)
            }
        }
    }
}


@OptIn(ExperimentalContracts::class)
fun <K, T> Map<K, List<T>>.pForEachKey(
    scope: CoroutineScope,
    action: suspend (Pair<K, List<T>>) -> Unit
) {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    for ((k, v) in this) {
        scope.launch {
            action(k to v)
        }
    }
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (start * (1f - fraction)) + (stop * fraction)
}

fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(
    combine(flow, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, ::Triple)
) { t1, t2 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third
    )
}

fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R
): Flow<R> = combine(
    combine(flow, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, ::Triple),
    flow7
) { t1, t2, t3 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third,
        t3
    )
}