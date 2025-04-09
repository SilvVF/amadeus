package io.silv.amadeus.coil

import coil3.ComponentRegistry
import coil3.disk.DiskCache

inline fun <reified T: Any> ComponentRegistry.Builder.addDiskFetcher(
    fetcher: DiskBackedFetcher<T>,
    noinline diskCache: () -> DiskCache,
): ComponentRegistry.Builder {

    val realFetcher =  DiskBackedFetcherImpl(
        keyer = fetcher.keyer,
        diskStore = fetcher.diskStore,
        overrideCall = { options, data ->  fetcher.overrideFetch(options, data) },
        fetch = { options, data ->  fetcher.fetch(options, data) },
        diskCacheInit = diskCache,
    )

    return this
        .add(realFetcher.factory)
        .add(fetcher.keyer)
}