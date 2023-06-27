package io.silv.amadeus

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
suspend fun <T, R> Iterable<T>.pmap(transform: suspend (T) -> R): List<R> {
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


fun <A, B> List<Pair<A, B>>.filterBothNotNull(): List<Pair<A, B>> {
    return this.mapNotNull { p ->
        if (p.first == null)
            return@mapNotNull null
        if (p.second == null)
            return@mapNotNull null
        return@mapNotNull p
    }
}