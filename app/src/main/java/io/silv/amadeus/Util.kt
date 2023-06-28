package io.silv.amadeus

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