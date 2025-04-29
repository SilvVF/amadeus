package eu.kanade.tachiyomi

import androidx.core.net.toUri
import coil3.Extras
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.getOrDefault
import coil3.request.Options
import com.hippo.unifile.UniFile
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.CacheControl.Visibility
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asSource
import io.ktor.utils.io.cancel
import io.ktor.utils.io.peek
import io.silv.common.log.LogPriority
import io.silv.common.log.logcat
import io.silv.common.model.MangaCover
import io.silv.data.download.CoverCache
import io.silv.data.manga.model.Manga
import kotlinx.coroutines.cancel
import kotlinx.io.okio.asOkioSource
import okio.Buffer
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException

val USE_CUSTOM_COVER_KEY = Extras.Key<Boolean>(true)

private val CACHE_CONTROL_NO_STORE = NoStore(visibility = Visibility.Public)
private val CACHE_CONTROL_NO_CACHE = NoCache(visibility = Visibility.Public)
private const val CACHE_CONTROL_NO_NETWORK = "only-if-cached, max-stale=${Int.MAX_VALUE}"

class MangaFetcher(
    private val url: String?,
    private val isLibraryManga: Boolean,
    private val options: Options,
    private val coverFileLazy: Lazy<File?>,
    private val customCoverFileLazy: Lazy<File>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val client: Lazy<HttpClient>,
    private val imageLoader: ImageLoader,
) : Fetcher {

    private val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult {
        logcat { "trying to fetch $url" }
        // Use custom cover if exists
        val useCustomCover = options.extras.getOrDefault(USE_CUSTOM_COVER_KEY)
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
            Type.URI -> uniFileLoader(url)
            null -> error("Invalid image")
        }
    }

    private fun uniFileLoader(urlString: String): FetchResult {
        val uniFile = UniFile.fromUri(options.context, urlString.toUri())!!
        val tempFile = uniFile.openInputStream().source().buffer()
        return SourceFetchResult(
            source = ImageSource(source = tempFile, fileSystem = FileSystem.SYSTEM),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKey,
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

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
                return SourceFetchResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = response.bodyAsChannel()

            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(responseBody, libraryCoverCacheFile)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(responseBody)
                if (snapshot != null) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceFetchResult(
                    source = ImageSource(
                        source = responseBody.asSource().asOkioSource().buffer(),
                        fileSystem = FileSystem.SYSTEM
                    ),
                    mimeType = "image/*",
                    dataSource = if (response.isFromCache()) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                response.cancel()
                responseBody.cancel()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): HttpResponse {
        val client = client.value
        val response = client.get(urlString = url!!) {
            if (options.networkCachePolicy.readEnabled) {
                // Equivalent to CacheControl.NO_STORE in OkHttp
                headers.append("Cache-Control", "no-store")
            } else {
                // Equivalent to CacheControl.FORCE_CACHE but without network
                headers.append("Cache-Control", "only-if-cached, max-stale=2147483647")
            }
        }
        if (!response.status.isSuccess() && response.status != HttpStatusCode.NotModified) {
            response.cancel()
            throw IOException("HTTP ${response.status.value}: ${response.status.description}")
        }

        return response
    }

    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            imageLoader.diskCache?.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            logcat(
                LogPriority.ERROR
            ) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private suspend fun writeResponseToCoverCache(response: ByteReadChannel, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null

        return try {
            Buffer().write(response.peek(Int.MAX_VALUE)!!.toByteArray()).use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) {
                "Failed to write response data to cover cache ${cacheFile.name}"
            }
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

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            imageLoader.diskCache?.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    private fun writeToDiskCache(
        response: ByteReadChannel,
    ): DiskCache.Snapshot? {
        val diskCache = imageLoader.diskCache
        val editor = diskCache?.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                response.asSource().asOkioSource().buffer().readAll(this)
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

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            cover.startsWith("content") -> Type.URI
            else -> null
        }
    }

    private enum class Type {
        File,
        URL,
        URI,
    }
}


class MangaFactory(
    private val client: Lazy<HttpClient>,
    private val coverCache: CoverCache
) : Fetcher.Factory<Manga> {

    override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
        return MangaFetcher(
            url = data.coverArt,
            isLibraryManga = data.inLibrary,
            options = options,
            coverFileLazy = lazy { coverCache.getCoverFile(data.coverArt) },
            customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.id) },
            diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
            client = client,
            imageLoader = imageLoader,
        )
    }
}

class MangaCoverFactory(
    private val client: Lazy<HttpClient>,
    private val coverCache: CoverCache
) : Fetcher.Factory<MangaCover> {

    override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher {
        return MangaFetcher(
            url = data.url,
            isLibraryManga = data.isMangaFavorite,
            options = options,
            coverFileLazy = lazy { coverCache.getCoverFile(data.url) },
            customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.mangaId) },
            diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
            client = client,
            imageLoader = imageLoader,
        )
    }
}


fun HttpResponse.isFromCache() =
    headers["Age"] != null || headers["X-Cache"]?.contains("HIT", ignoreCase = true) == true
