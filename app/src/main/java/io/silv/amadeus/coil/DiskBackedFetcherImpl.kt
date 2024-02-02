package io.silv.amadeus.coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.graphics.drawable.toDrawable
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import coil.size.isOriginal
import io.silv.amadeus.coil.CoilDiskUtils.toImageSource
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File


class DiskBackedFetcherImpl<T: Any>(
    val keyer: Keyer<T>,
    val diskStore: FetcherDiskStore<T>,
    val context: Context,
    val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    val fetch: suspend (options: Options, data: T) -> ByteArray,
    val memoryCacheInit: () -> MemoryCache,
    val diskCacheInit: () -> DiskCache
) {

    internal val memCache
        get() = memoryCacheInit()

    internal val diskCache
        get() = diskCacheInit()

    private fun fileLoader(file: File, diskCacheKey: String): FetchResult {
        return SourceResult(
            source = ImageSource(
                file = file.toOkioPath(),
                diskCacheKey = diskCacheKey
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun FetchResult.toBytes(): ByteArray? = when (this) {
        is DrawableResult -> {
            val bitmap = (this.drawable as? BitmapDrawable)?.bitmap
            bitmap?.let { bmp ->
                val stream = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bmp.compress(
                        Bitmap.CompressFormat.WEBP_LOSSLESS,
                        100,
                        stream
                    )
                } else {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                stream.toByteArray()
            }
        }
        is SourceResult -> this.source.source().buffer.readByteArray()
    }


    val factory = Fetcher.Factory<T> { data, options, _ ->
        fetcher(data, options)
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun fetcher(data: T, options: Options) = object : Fetcher {


        val diskCacheKey = keyer.key(data, options) ?: error("null disk cache key provided")
        val memCacheKey = MemoryCache.Key(keyer.key(data, options) ?: error("null mem cache key provided"))

        override suspend fun fetch(): FetchResult {

            if (options.memoryCachePolicy.readEnabled) {

                memCache[memCacheKey]?.let { cachedValue ->

                    return DrawableResult(
                        drawable = cachedValue.bitmap.toDrawable(context.resources),
                        isSampled = options.size.isOriginal,
                        dataSource = DataSource.MEMORY_CACHE
                    )
                }
            }

            val overrideData = overrideCall(options, data)

            if (overrideData != null) {

                overrideData.toBytes()?.let {
                    CoilDiskUtils.writeToMemCache(
                        options = options,
                        bytes = it,
                        memCacheKey = memCacheKey,
                        memCache = memCache
                    )
                }
                return overrideData
            }

            val imageCacheFile = diskStore.getImageFile(data, options)

            // Check if the file path already has an existing file meaning the image exists
            if (imageCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {

                CoilDiskUtils.writeToMemCache(
                    options,
                    imageCacheFile.readBytes(),
                    memCacheKey,
                    memCache
                )

                return fileLoader(imageCacheFile, diskCacheKey)
            }

            var snapshot = CoilDiskUtils.readFromDiskCache(options, diskCache, diskCacheKey)

            try {
                // Fetch from disk cache
                if (snapshot != null) {

                    val snapshotCoverCache = CoilDiskUtils.moveSnapshotToCoverCache(
                        diskCache,
                        diskCacheKey,
                        snapshot,
                        imageCacheFile
                    )

                    if (snapshotCoverCache != null) {
                        CoilDiskUtils.writeToMemCache(
                            options,
                            snapshotCoverCache.readBytes(),
                            memCacheKey,
                            memCache
                        )
                        // Read from cover cache after added to library
                        return fileLoader(snapshotCoverCache, diskCacheKey)
                    }

                    CoilDiskUtils.writeToMemCache(
                        options,
                        snapshot.data.toFile().readBytes(),
                        memCacheKey,
                        memCache
                    )
                    // Read from snapshot
                    return SourceResult(
                        source = snapshot.toImageSource(diskCacheKey),
                        mimeType = "image/*",
                        dataSource = DataSource.DISK,
                    )
                }
                // Fetch from network
                val response = fetch(options, data)
                try {
                    // Read from cover cache after library manga cover updated
                    val responseCoverCache = CoilDiskUtils.writeResponseToCoverCache(
                        ByteArrayInputStream(response),
                        imageCacheFile,
                        options
                    )
                    if (responseCoverCache != null) {
                        CoilDiskUtils.writeToMemCache(
                            options,
                            responseCoverCache.readBytes(),
                            memCacheKey,
                            memCache
                        )
                        return fileLoader(responseCoverCache, diskCacheKey)
                    }

                    // Read from disk cache
                    snapshot = CoilDiskUtils.writeToDiskCache(response, diskCache, diskCacheKey)
                    if (snapshot != null) {
                        CoilDiskUtils.writeToMemCache(
                            options,
                            snapshot.data.toFile().readBytes(),
                            memCacheKey,
                            memCache
                        )
                        return SourceResult(
                            source = snapshot.toImageSource(diskCacheKey),
                            mimeType = "image/*",
                            dataSource = DataSource.NETWORK,
                        )
                    }

                    CoilDiskUtils.writeToMemCache(
                        options,
                        response,
                        memCacheKey,
                        memCache
                    )
                    // Read from response if cache is unused or unusable
                    return SourceResult(
                        source = ImageSource(
                            source = ByteArrayInputStream(response).source().buffer(),
                            context = options.context
                        ),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                } catch (e: Exception) {
                    throw e
                }
            } catch (e: Exception) {
                snapshot?.close()
                throw e
            }
        }
    }
}