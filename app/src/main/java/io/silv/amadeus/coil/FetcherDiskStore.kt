package io.silv.amadeus.coil

import coil.request.Options
import java.io.File

interface FetcherDiskStore<T: Any> {
    fun getImageFile(data: T, options: Options): File?
}

fun interface FetcherDiskStoreImageFile<T: Any> : FetcherDiskStore<T>