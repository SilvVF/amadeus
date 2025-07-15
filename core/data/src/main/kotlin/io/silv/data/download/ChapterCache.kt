package io.silv.data.download

import android.content.Context
import android.text.format.Formatter
import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.asSource
import io.silv.common.AmadeusDispatchers
import io.silv.common.DependencyAccessor
import io.silv.common.commonDeps
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.data.util.DiskUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import java.io.File

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 */
class ChapterCache @OptIn(DependencyAccessor::class) constructor(
    private val context: Context,
    private val json: Json,
    private val dispatchers: AmadeusDispatchers = commonDeps.dispatchers,
) {

    private val fileSystem = FileSystem.SYSTEM

    private val diskCache by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runBlocking {
            FileKache(
                File(context.cacheDir, "chapter_disk_cache_v1").path,
                maxSize = 100L * 1024 * 1024,
            ) {
                strategy = KacheStrategy.LRU
            }
        }
    }


    /**
     * Returns real size of directory.
     */
    suspend fun realSize(): Long = diskCache.size

    /**
     * Returns real size of directory in human readable format.
     */
    suspend fun readableSize(): String = Formatter.formatFileSize(context, realSize())

    /**
     * Get page list from cache.
     *
     * @param chapter the chapter.
     * @return the list of pages.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getPageListFromCache(chapter: ChapterResource): List<Page> =
        withContext(dispatchers.io) {
            // Get the key for the chapter.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))

            // Convert JSON string to list of objects. Throws an exception if snapshot is null
            val filePath = requireNotNull(diskCache.get(key))
            fileSystem.source(filePath.toPath()).buffer().inputStream().use {
                json.decodeFromStream<List<Page>>(it)
            }
        }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    suspend fun putPageListToCache(chapter: ChapterResource, pages: List<Page>) =
        withContext(dispatchers.io) {
            try {
                // Get editor from md5 key.
                val key = DiskUtil.hashKeyForDisk(getKey(chapter))

                diskCache.put(key) { filePath ->
                    runCatching {
                        fileSystem.sink(filePath.toPath()).buffer().use {
                            it.writeUtf8(json.encodeToString(pages))
                        }
                    }
                        .isSuccess
                }
            } catch (e: Exception) {
                logcat { e.asLog() }
                // Ignore.
            }
        }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    suspend fun isImageInCache(imageUrl: String): Boolean = withContext(dispatchers.io) {
        try {
            diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)) != null
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Get image file from url.
     *
     * @param imageUrl url of image.
     * @return path of image.
     */
    suspend fun getImageFilePath(imageUrl: String): Path = withContext(dispatchers.io) {
        // Get file from md5 key.
        val imageName = DiskUtil.hashKeyForDisk(imageUrl)
        val image = requireNotNull(diskCache.get(imageName))
        image.toPath()
    }

    /**
     * Add image to cache.
     *
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    @Throws(IOException::class)
    suspend fun putImageToCache(imageUrl: String, response: HttpResponse) =
        withContext(dispatchers.io) {
            // Initialize editor (edits the values for an entry).
            try {
                // Get editor from md5 key.
                val key = DiskUtil.hashKeyForDisk(imageUrl)
                response.bodyAsChannel().asSource().buffered().use {
                    // Get OutputStream and write image with Okio.
                    diskCache.put(key) { path ->
                        runCatching {
                            fileSystem.sink(path.toPath()).buffer().use { sink ->
                                sink.write(it.readByteArray())
                            }
                        }
                            .onFailure {
                                logcat { "Chapter failed to cache ${it.message}" }
                            }
                            .onSuccess {
                                logcat { "Chapter put to cache" }
                            }
                            .isSuccess
                    }
                }
            } finally {
                response.cancel()
            }
        }

    suspend fun clear(): Int = withContext(dispatchers.io) {
        val count = diskCache.getKeys().size

        diskCache.clear()

        count - diskCache.getKeys().size
    }

    private fun getKey(chapter: ChapterResource): String {
        return "${chapter.mangaId}${chapter.url}"
    }
}

