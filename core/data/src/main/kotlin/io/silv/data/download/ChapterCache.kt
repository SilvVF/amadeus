package io.silv.data.download

import android.content.Context
import android.text.format.Formatter
import android.util.Log
import com.mayakapps.kache.FileKache
import com.mayakapps.kache.KacheStrategy
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.utils.io.core.use
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.data.util.DiskUtil
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File

/**
 * Class used to create chapter cache
 * For each image in a chapter a file is created
 * For each chapter a Json list is created and converted to a file.
 * The files are in format *md5key*.0
 *
 * @param context the application context.
 */
class ChapterCache(
    private val context: Context,
    private val json: Json,
) {

    private val fileSystem = FileSystem.SYSTEM

    /** Cache class used for cache management. */
    private val diskCache by lazy {
        runBlocking {
            FileKache(
                cacheDir.path,
                maxSize = 100L * 1024 * 1024,
            ) {
                strategy = KacheStrategy.LRU
            }
        }
    }

    /**
     * Returns directory of cache.
     */
    private val cacheDir: File = File(context.cacheDir, "chapter_disk_cache")

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = diskCache.size

    /**
     * Returns real size of directory in human readable format.
     */
    val readableSize: String
        get() = Formatter.formatFileSize(context, realSize)

    /**
     * Get page list from cache.
     *
     * @param chapter the chapter.
     * @return the list of pages.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getPageListFromCache(chapter: ChapterResource): List<Page> {
        // Get the key for the chapter.
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))

        // Convert JSON string to list of objects. Throws an exception if snapshot is null
        return diskCache.get(key)!!.let { filePath ->

            json.decodeFromString<List<Page>>(
                fileSystem.source(filePath.toPath())
                    .buffer()
                    .readByteArray()
                    .decodeToString()
            )
        }
    }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    suspend fun putPageListToCache(chapter: ChapterResource, pages: List<Page>) {
        // Convert list of pages to json string.
        val cachedValue = json.encodeToString(pages)

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))

            diskCache.put(key) { filePath ->
                runCatching {
                    fileSystem.sink(filePath.toPath()).buffer().use { sink ->

                        sink.write(cachedValue.toByteArray())
                        sink.flush()
                    }
                }
                    .isSuccess
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore.
        }
    }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    suspend fun isImageInCache(imageUrl: String): Boolean {
        return try {
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
    suspend fun getImageFilePath(imageUrl: String): Path {
        // Get file from md5 key.
        val imageName = DiskUtil.hashKeyForDisk(imageUrl)

        return diskCache.get(imageName)!!.toPath()
    }

    /**
     * Add image to cache.
     *
     * @param imageUrl url of image.
     * @param response http response from page.
     * @throws IOException image error.
     */
    @Throws(IOException::class)
    suspend fun putImageToCache(imageUrl: String, response: HttpResponse) {
        // Initialize editor (edits the values for an entry).
        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(imageUrl)

            val responseBody: ByteArray = response.body()

            // Get OutputStream and write image with Okio.
            diskCache.put(key) {path ->
                runCatching {
                    fileSystem.sink(path.toPath()).buffer().use { os ->
                        os.write(responseBody)
                        os.flush()
                    }
                }
                    .onFailure {
                        Log.d("Chapter", "failed to cache ${it.message}")
                    }
                    .onSuccess {
                        Log.d("Chapter", "put to cache")
                    }
                    .isSuccess
            }
        } finally {
            response.cancel()
        }
    }

    suspend fun clear(): Int {

        val count = diskCache.getKeys().size

        diskCache.clear()

        return count - diskCache.getKeys().size
    }

    private fun getKey(chapter: ChapterResource): String {
        return "${chapter.mangaId}${chapter.url}"
    }
}

