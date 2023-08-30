package io.silv.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (start * (1f - fraction)) + (stop * fraction)
}