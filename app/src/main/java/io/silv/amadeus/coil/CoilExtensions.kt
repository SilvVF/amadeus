package io.silv.amadeus.coil

import coil.ComponentRegistry
import coil.disk.DiskCache
import coil.memory.MemoryCache

inline fun <reified T: Any> ComponentRegistry.Builder.addDiskFetcher(
    fetcher: DiskBackedFetcher<T>,
    noinline diskCache: () -> DiskCache,
    noinline memoryCache: () -> MemoryCache
) {

    val realFetcher =  DiskBackedFetcherImpl(
        keyer = fetcher.keyer,
        diskStore = fetcher.diskStore,
        context = fetcher.context,
        overrideCall = { options, data ->  fetcher.overrideFetch(options, data) },
        fetch = { options, data ->  fetcher.fetch(options, data) },
        diskCacheInit = diskCache,
        memoryCacheInit = memoryCache
    )

    this.add(realFetcher.factory)
    this.add(fetcher.keyer)
}