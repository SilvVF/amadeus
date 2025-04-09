package io.silv.amadeus.coil

import android.content.Context
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import io.silv.amadeus.coil.CoilDiskUtils.toImageSource
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import java.io.ByteArrayInputStream
import java.io.File


class DiskBackedFetcherImpl<T: Any>(
    val keyer: Keyer<T>,
    val diskStore: FetcherDiskStore<T>,
    val overrideCall: suspend (options: Options, data: T) -> FetchResult? = { _, _ -> null },
    val fetch: suspend (options: Options, data: T) -> Source,
    val diskCacheInit: () -> DiskCache
) {
    private val fs = FileSystem.SYSTEM

    internal val diskCache get() = diskCacheInit()

    private fun fileLoader(file: File, diskCacheKey: String): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = fs,
                diskCacheKey = diskCacheKey
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    val factory = Fetcher.Factory<T> { data, options, _ ->
        fetcher(data, options)
    }

    private fun fetcher(data: T, options: Options) = object : Fetcher {


        val diskCacheKey = keyer.key(data, options) ?: error("null disk cache key provided")

        override suspend fun fetch(): FetchResult {

            overrideCall(options, data)?.let {
                return it
            }

            val imageCacheFile = diskStore.getImageFile(data, options)

            // Check if the file path already has an existing file meaning the image exists
            if (imageCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
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
                        // Read from cover cache after added to library
                        return fileLoader(snapshotCoverCache, diskCacheKey)
                    }

                    // Read from snapshot
                    return SourceFetchResult(
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
                        response,
                        imageCacheFile,
                        options
                    )
                    if (responseCoverCache != null) {
                        return fileLoader(responseCoverCache, diskCacheKey)
                    }

                    // Read from disk cache
                    snapshot = CoilDiskUtils.writeToDiskCache(response, diskCache, diskCacheKey)
                    if (snapshot != null) {
                        return SourceFetchResult(
                            source = snapshot.toImageSource(diskCacheKey),
                            mimeType = "image/*",
                            dataSource = DataSource.NETWORK,
                        )
                    }
                    // Read from response if cache is unused or unusable
                    return SourceFetchResult(
                        source = ImageSource(
                            source = response.buffer(),
                            fileSystem = fs
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