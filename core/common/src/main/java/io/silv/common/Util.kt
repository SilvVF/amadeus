package io.silv.common

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface PrefsConverter<T, V> {
    fun convertTo(value: T): V
    fun convertFrom(value: V): T

    companion object {
        fun <T, V> create(
            convertTo: (obj: T) -> V,
            convertFrom: (obj: V) -> T
        ): PrefsConverter<T, V> {
            return object : PrefsConverter<T, V> {
                override fun convertFrom(value: V): T = convertFrom(value)
                override fun convertTo(value: T): V = convertTo(value)
            }
        }
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

fun <T> emptyImmutableList(): ImmutableList<T> = persistentListOf()


suspend inline fun <T, R> Sequence<T>.pmap(crossinline transform: suspend (T) -> R): List<R> {
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

fun <T> Iterable<T>.pForEach(
    scope: CoroutineScope,
    action: suspend (T) -> Unit,
) {
    for (item in this) {
        scope.launch {
            action(item)
        }
    }
}

// @OptIn(ExperimentalContracts::class)
// suspend fun <T> Iterable<T>.pForEach(
//    action: suspend (T) -> Unit
// ) {
//    contract { callsInPlace(action) }
//    coroutineScope {
//        for (item in this@pForEach) {
//            launch {
//                action(item)
//            }
//        }
//    }
// }

fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float {
    return (start * (1f - fraction)) + (stop * fraction)
}
