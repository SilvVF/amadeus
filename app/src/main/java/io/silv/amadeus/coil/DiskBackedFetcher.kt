package io.silv.amadeus.coil

import coil3.fetch.FetchResult
import coil3.key.Keyer
import coil3.request.Options
import okio.Source

interface DiskBackedFetcher<T : Any> {
    val keyer: Keyer<T>
    val diskStore: FetcherDiskStore<T>
    suspend fun overrideFetch(options: Options, data: T): FetchResult? { return null }
    suspend fun fetch(options: Options, data: T): Source
}