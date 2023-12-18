package io.silv.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.silv.common.model.ChapterResource
import io.silv.common.model.Download
import io.silv.common.model.MangaResource
import kotlinx.coroutines.flow.first
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Class used for download serialization
 *
 * @param mangaId the id of the manga.
 * @param chapterId the id of the chapter.
 * @param order the order of the download in the queue.
 */
@Serializable
private data class DownloadObject(val mangaId: String, val chapterId: String, val order: Int)

/**
 * This class is used to persist active downloads across application restarts.
 */
class DownloadStore(
    context: Context,
) {
    private val Context.store: DataStore<Preferences> by preferencesDataStore(
        name = "download_store"
    )

    private val datastore = context.store

    /**
     * Counter used to keep the queue order.
     */
    private var counter = 0

    /**
     * Adds a list of downloads to the store.
     *
     * @param downloads the list of downloads to add.
     */
    suspend fun addAll(downloads: List<Download>) {
        Log.d("DownloadStore", "serializing")
        datastore.edit { prefs ->
            downloads.forEach {
                prefs[getKey(it)] = serialize(it)
            }
        }
    }

    /**
     * Removes a download from the store.
     *
     * @param download the download to remove.
     */
    suspend fun remove(download: Download) {
        datastore.edit {
            it.remove(getKey(download))
        }
    }

    /**
     * Removes a list of downloads from the store.
     *
     * @param downloads the download to remove.
     */
    suspend fun removeAll(downloads: List<Download>) {
        datastore.edit {
            downloads.forEach { download ->
                it.remove(getKey(download))
            }
        }
    }

    /**
     * Removes all the downloads from the store.
     */
    suspend fun clear() {
        datastore.edit {
            it.clear()
        }
    }

    /**
     * Returns the preference's key for the given download.
     *
     * @param download the download.
     */
    private fun getKey(download: Download): Preferences.Key<String> {
        return stringPreferencesKey(download.chapter.id)
    }

    /**
     * Returns the list of downloads to restore. It should be called in a background thread.
     */
    suspend fun restore(
        getManga: suspend (id: String) -> MangaResource?,
        getChapter: suspend (id: String) -> ChapterResource?,
    ): List<Download> {
        val objs =
            datastore.data.first().asMap()
                .mapNotNull { it.value as? String }
                .mapNotNull { deserialize(it) }
                .sortedBy { it.order }

        val downloads = mutableListOf<Download>()
        if (objs.isNotEmpty()) {
            val cachedManga = mutableMapOf<String, MangaResource?>()
            for ((mangaId, chapterId) in objs) {
                val manga = cachedManga.getOrPut(mangaId) { getManga(mangaId) } ?: continue
                val chapter = getChapter(chapterId) ?: continue

                downloads.add(Download(manga, chapter))
            }
        }

        // Clear the store, downloads will be added again immediately.
        clear()
        return downloads
    }

    /**
     * Converts a download to a string.
     *
     * @param download the download to serialize.
     */
    @OptIn(InternalSerializationApi::class)
    private fun serialize(download: Download): String {
        val obj = DownloadObject(download.manga.id, download.chapter.id, counter++)
        return try {
            Json.encodeToString<DownloadObject>(obj)
        } catch (e: SerializationException) {
            Log.d("DownloadStore", e.stackTraceToString())
            ""
        }
    }

    /**
     * Restore a download from a string.
     *
     * @param string the download as string.
     */
    private fun deserialize(string: String): DownloadObject? {
        return try {
            Json.decodeFromString<DownloadObject>(string)
        } catch (e: Exception) {
            Log.d("DownloadStore", e.stackTraceToString())
            null
        }
    }
}
