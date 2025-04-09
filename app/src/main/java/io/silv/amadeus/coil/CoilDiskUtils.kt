package io.silv.amadeus.coil

import android.util.Log
import coil3.annotation.ExperimentalCoilApi
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.request.Options
import io.silv.common.log.LogPriority
import io.silv.common.log.logcat
import io.silv.data.download.saveTo
import okio.FileSystem
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayInputStream
import java.io.File

internal object CoilDiskUtils {

    fun writeResponseToCoverCache(
        response: Source,
        cacheFile: File?,
        options: Options
    ): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) {
                "writeResponseToCoverCache:" +
                        "Failed to write response data to cover cache ${cacheFile.name}"
            }
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
            logcat {
                "moveSnapshotToCoverCache" + "Failed to write snapshot data to cover cache ${cacheFile.name}"
            }
            null
        }
    }

    fun DiskCache.Snapshot.toImageSource(key: String): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = key,
            closeable = this
        )
    }

    fun writeToDiskCache(
        response: Source,
        diskCache: DiskCache,
        diskCacheKey: String
    ): DiskCache.Snapshot? {
        val editor = diskCache.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                response.buffer().use {
                    write(it.readByteArray())
                }
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            runCatching { editor.abort() }
            throw e
        }
    }
}