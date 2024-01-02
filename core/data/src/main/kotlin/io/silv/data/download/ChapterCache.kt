package io.silv.data.download

import android.content.Context
import android.text.format.Formatter
import com.jakewharton.disklrucache.DiskLruCache
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.data.util.DiskUtil
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import okio.buffer
import okio.sink
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
class ChapterCache(
    private val context: Context,
    private val json: Json,
) {

    /** Cache class used for cache management. */
    private val diskCache = DiskLruCache.open(
        File(context.cacheDir, "chapter_disk_cache"),
        1,
        1,
        100L * 1024 * 1024,
    )

    /**
     * Returns directory of cache.
     */
    private val cacheDir: File = diskCache.directory

    /**
     * Returns real size of directory.
     */
    private val realSize: Long
        get() = DiskUtil.getDirectorySize(cacheDir)

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
    fun getPageListFromCache(chapter: ChapterResource): List<Page> {
        // Get the key for the chapter.
        val key = DiskUtil.hashKeyForDisk(getKey(chapter))

        // Convert JSON string to list of objects. Throws an exception if snapshot is null
        return diskCache.get(key).use {
            json.decodeFromString(it.getString(0))
        }
    }

    /**
     * Add page list to disk cache.
     *
     * @param chapter the chapter.
     * @param pages list of pages.
     */
    fun putPageListToCache(chapter: ChapterResource, pages: List<Page>) {
        // Convert list of pages to json string.
        val cachedValue = json.encodeToString(pages)

        // Initialize the editor (edits the values for an entry).
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(getKey(chapter))
            editor = diskCache.edit(key) ?: return

            // Write chapter urls to cache.
            editor.newOutputStream(0).sink().buffer().use {
                it.write(cachedValue.toByteArray())
                it.flush()
            }

            diskCache.flush()
            editor.commit()
            editor.abortUnlessCommitted()
        } catch (e: Exception) {
            // Ignore.
        } finally {
            editor?.abortUnlessCommitted()
        }
    }

    /**
     * Returns true if image is in cache.
     *
     * @param imageUrl url of image.
     * @return true if in cache otherwise false.
     */
    fun isImageInCache(imageUrl: String): Boolean {
        return try {
            diskCache.get(DiskUtil.hashKeyForDisk(imageUrl)).use { it != null }
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
    fun getImageFile(imageUrl: String): File {
        // Get file from md5 key.
        val imageName = DiskUtil.hashKeyForDisk(imageUrl) + ".0"
        return File(diskCache.directory, imageName)
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
        var editor: DiskLruCache.Editor? = null

        try {
            // Get editor from md5 key.
            val key = DiskUtil.hashKeyForDisk(imageUrl)
            editor = diskCache.edit(key) ?: throw IOException("Unable to edit key")

            val responseBody: ByteArray = response.body()

            // Get OutputStream and write image with Okio.
            editor.newOutputStream(0).use {
                it.write(responseBody)
            }

            diskCache.flush()
            editor.commit()
        } finally {
            response.cancel()
            editor?.abortUnlessCommitted()
        }
    }

    fun clear(): Int {
        var deletedFiles = 0
        cacheDir.listFiles()?.forEach {
            if (removeFileFromCache(it.name)) {
                deletedFiles++
            }
        }
        return deletedFiles
    }

    /**
     * Remove file from cache.
     *
     * @param file name of file "md5.0".
     * @return status of deletion for the file.
     */
    private fun removeFileFromCache(file: String): Boolean {
        // Make sure we don't delete the journal file (keeps track of cache)
        if (file == "journal" || file.startsWith("journal.")) {
            return false
        }

        return try {
            // Remove the extension from the file to get the key of the cache
            val key = file.substringBeforeLast(".")
            // Remove file from cache
            diskCache.remove(key)
        } catch (e: Exception) {
            false
        }
    }

    private fun getKey(chapter: ChapterResource): String {
        return "${chapter.mangaId}${chapter.url}"
    }
}

