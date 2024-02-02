package io.silv.amadeus.coil

import android.content.Context
import coil.memory.MemoryCache

internal object CoilMemoryCache {

    private var instance: MemoryCache? = null

    @Synchronized
    fun get(context: Context): MemoryCache {
        return instance ?: run {
            // Create the singleton mem cache instance.
            MemoryCache.Builder(context).build()
        }
    }
}