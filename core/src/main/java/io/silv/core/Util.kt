package io.silv.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.contracts.ExperimentalContracts
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
