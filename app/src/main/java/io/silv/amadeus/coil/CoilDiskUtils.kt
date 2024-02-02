package io.silv.amadeus.coil

import android.graphics.BitmapFactory
import android.util.Log
import coil.annotation.ExperimentalCoilApi
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.Options
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayInputStream
import java.io.File

internal object CoilDiskUtils {

    fun writeToMemCache(
        options: Options,
        bytes: ByteArray,
        memCacheKey: MemoryCache.Key,
        memCache: MemoryCache
    ) = runCatching {
        if (options.memoryCachePolicy.writeEnabled) {
            val bmp = with(
                BitmapFactory.Options().apply { inMutable = true }
            ) {
                BitmapFactory.decodeByteArray(
                    bytes, 0,
                    bytes.size,
                    this
                )
            }
            memCache[memCacheKey] = MemoryCache.Value(bitmap = bmp)
        }
    }

    fun writeResponseToCoverCache(
        response: ByteArrayInputStream,
        cacheFile: File?,
        options: Options
    ): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Log.e(
                "writeResponseToCoverCache",
                "Failed to write response data to cover cache ${cacheFile.name}"
            )
            null
        }
    }

    fun writeSourceToCoverCache(
        input: Source,
        cacheFile: File,
    ) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun readFromDiskCache(
        options: Options,
        diskCache: DiskCache,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCache.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun moveSnapshotToCoverCache(
        diskCache: DiskCache,
        diskCacheKey: String,
        snapshot: DiskCache.Snapshot,
        cacheFile: File?,
    ): File? {
        if (cacheFile == null) return null
        return try {
            diskCache.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Log.e(
                "moveSnapshotToCoverCache",
                "Failed to write snapshot data to cover cache ${cacheFile.name}"
            )
            null
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun DiskCache.Snapshot.toImageSource(key: String): ImageSource {
        return ImageSource(
            file = data,
            diskCacheKey = key,
            closeable = this
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    fun writeToDiskCache(
        response: ByteArray,
        diskCache: DiskCache,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        val editor = diskCache.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                write(response)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            runCatching { editor.abort() }
            throw e
        }
    }
}