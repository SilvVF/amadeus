package io.silv.amadeus.coil

import android.content.Context
import coil.fetch.FetchResult
import coil.key.Keyer
import coil.request.Options

interface DiskBackedFetcher<T : Any> {
    val keyer: Keyer<T>
    val diskStore: FetcherDiskStore<T>
    val context: Context
    suspend fun overrideFetch(options: Options, data: T): FetchResult? { return null }
    suspend fun fetch(options: Options, data: T): ByteArray
}