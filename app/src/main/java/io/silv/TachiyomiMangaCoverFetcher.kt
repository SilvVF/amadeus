package io.silv

import android.util.Log
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.network.HttpException
import coil.request.Options
import coil.request.Parameters
import io.silv.MangaCoverFetcher.Companion.USE_CUSTOM_COVER
import io.silv.amadeus.CoverCache
import io.silv.database.entity.manga.SavedMangaEntity
import io.silv.model.SavableManga
import io.silv.network.model.manga.Manga
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED

/**
 * A [Fetcher] that fetches cover image for [Manga] object.
 *
 * It uses [Manga.thumbnailUrl] if custom cover is not set by the user.
 * Disk caching for library items is handled by [CoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER]: Use custom cover if set by user, default is true
 */
class MangaCoverFetcher(
    private val url: String?,
    private val isLibraryManga: Boolean,
    private val options: Options,
    private val coverFileLazy: Lazy<File?>,
    private val customCoverFileLazy: Lazy<File>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher {

    private val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(USE_CUSTOM_COVER) ?: true
        if (useCustomCover) {
            val customCoverFile = customCoverFileLazy.value
            if (customCoverFile.exists()) {
                return fileLoader(customCoverFile)
            }
        }

        // diskCacheKey is thumbnail_url
        if (url == null) error("No cover specified")
        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> fileLoader(File(url.substringAfter("file://")))
            null -> error("Invalid image")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), diskCacheKey = diskCacheKey),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun httpLoader(): FetchResult {
        // Only cache separately if it's a library item
        val libraryCoverCacheFile = if (isLibraryManga) {
            coverFileLazy.value ?: error("No cover specified")
        } else {
            null
        }
        if (libraryCoverCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
            return fileLoader(libraryCoverCacheFile)
        }

        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, libraryCoverCacheFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                return SourceResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, libraryCoverCacheFile)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceResult(
                    source = ImageSource(source = responseBody.source(), context = options.context),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = callFactoryLazy.value
        val response = client.newCall(newRequest()).execute()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw HttpException(response)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url!!)
            .headers(options.headers)
            // Support attaching custom data to the network request.
            .tag(Parameters::class.java, options.parameters)

        when {
            options.networkCachePolicy.readEnabled -> {
                // don't take up okhttp cache
                request.cacheControl(CACHE_CONTROL_NO_STORE)
            }
            else -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            diskCacheLazy.value.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Log.e("moveSnapshotToCoverCache", "Failed to write snapshot data to cover cache ${cacheFile.name}")
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Log.e("writeResponseToCoverCache", "Failed to write response data to cover cache ${cacheFile.name}")
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
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
    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            diskCacheLazy.value.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun writeToDiskCache(
        response: Response,
    ): DiskCache.Snapshot? {
        val editor = diskCacheLazy.value.openEditor(diskCacheKey) ?: return null
        try {
            diskCacheLazy.value.fileSystem.write(editor.data) {
                response.body!!.source().readAll(this)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            else -> null
        }
    }

    private enum class Type {
        File, URL
    }

    class MangaFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<SavableManga>, KoinComponent {

        private val coverCache: CoverCache by inject()

        override fun create(data: SavableManga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                url = data.coverArt,
                isLibraryManga = data.bookmarked,
                options = options,
                coverFileLazy = lazy { coverCache.getCoverFile(data.coverArt) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.id) },
                diskCacheKeyLazy = lazy { MangaKeyer().key(data, options) },
                callFactoryLazy = callFactoryLazy,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    class MangaCoverFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<MangaCover>, KoinComponent {

        private val coverCache: CoverCache by inject<CoverCache>()

        override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                url = data.url,
                isLibraryManga = data.isMangaFavorite,
                options = options,
                coverFileLazy = lazy { coverCache.getCoverFile(data.url) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.mangaId) },
                diskCacheKeyLazy = lazy { MangaCoverKeyer(coverCache).key(data, options) },
                callFactoryLazy = callFactoryLazy,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"

        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}

/**
 * Contains the required data for MangaCoverFetcher
 */
data class MangaCover(
    val mangaId: String,
    val url: String?,
    val isMangaFavorite: Boolean,
    val lastModified: Long,
)

fun SavedMangaEntity.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        url = coverArt,
        isMangaFavorite = true,
        lastModified = savedAtLocal.toInstant(TimeZone.currentSystemDefault()).epochSeconds,
    )
}

fun SavableManga.hasCustomCover(): Boolean {
    return false
}

class MangaKeyer : Keyer<SavableManga> {
    override fun key(data: SavableManga, options: Options): String {
        return if (data.hasCustomCover()) {
            "${data.id};${data.savedLocalAtEpochSeconds}"
        } else {
            "${data.coverArt};${data.savedLocalAtEpochSeconds}"
        }
    }
}


class MangaCoverKeyer(
    private val coverCache: CoverCache,
) : Keyer<MangaCover> {
    override fun key(data: MangaCover, options: Options): String {
        return if (coverCache.getCustomCoverFile(data.mangaId).exists()) {
            "${data.mangaId};${data.lastModified}"
        } else {
            "${data.url};${data.lastModified}"
        }
    }
}