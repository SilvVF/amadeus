package eu.kanade.tachiyomi

import coil3.Extras
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.network.httpHeaders
import coil3.request.Options
import eu.kanade.tachiyomi.MangaCoverFetcher.Companion.USE_CUSTOM_COVER
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.CacheControl.Visibility
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.asSource
import io.silv.amadeus.coil.DiskBackedFetcher
import io.silv.amadeus.coil.FetcherDiskStore
import io.silv.amadeus.coil.FetcherDiskStoreImageFile
import io.silv.common.model.MangaCover
import io.silv.data.download.CoverCache
import io.silv.data.manga.model.Manga
import kotlinx.io.okio.asOkioSource
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source

/**
 * A [Fetcher] that fetches cover image for [Manga] object.
 *
 * It uses [Manga.coverArt] if custom cover is not set by the user.
 * Disk caching for library items is handled by [CoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER]: Use custom cover if set by user, default is true
 */
class MangaCoverFetcher(
    private val client: HttpClient,
    private val coverCache: CoverCache,
) {

    private val mangaKeyer = MangaKeyer(coverCache)
    private val coverKeyer = MangaCoverKeyer(coverCache)

    val coverFetcher: DiskBackedFetcher<MangaCover> = object: DiskBackedFetcher<MangaCover> {
        override val keyer: Keyer<MangaCover> = coverKeyer
        override val diskStore: FetcherDiskStore<MangaCover> =
            FetcherDiskStoreImageFile { data, _ ->
                if (data.isMangaFavorite) {
                    coverCache.getCoverFile(data.url)
                } else {
                    null
                }
            }
        override suspend fun fetch(options: Options, data: MangaCover): Source = fetchManga(options, data.url)
        override suspend fun overrideFetch(options: Options, data: MangaCover): FetchResult? {
            return customCoverOverride(options, data.mangaId, data.isMangaFavorite, coverKeyer.key(data, options))
        }
    }

    val mangaFetcher: DiskBackedFetcher<Manga> = object: DiskBackedFetcher<Manga> {
        override val keyer: Keyer<Manga> = mangaKeyer
        override val diskStore: FetcherDiskStore<Manga> =
            FetcherDiskStoreImageFile { data, options ->
                if (data.inLibrary) {
                    coverCache.getCoverFile(data.coverArt)
                } else {
                    null
                }
            }
        override suspend fun fetch(options: Options, data: Manga): Source = fetchManga(options, data.coverArt)
        override suspend fun overrideFetch(options: Options, data: Manga): FetchResult? {
            return customCoverOverride(options, data.id, data.inLibrary, mangaKeyer.key(data, options))
        }
    }

    suspend fun fetchManga(options: Options, url: String): Source {
        return client
            .get(url) {
                for (header in options.httpHeaders.asMap().toList()) {
                    header(header.first, header.second)
                }
                when {
                    options.networkCachePolicy.readEnabled -> {
                        // don't take up okhttp cache
                        header(HttpHeaders.CacheControl, CACHE_CONTROL_NO_STORE)
                    }
                    else -> {
                        // This causes the request to fail with a 504 Unsatisfiable Request.
                        header(HttpHeaders.CacheControl, CACHE_CONTROL_NO_CACHE)
                        header(HttpHeaders.CacheControl, CACHE_CONTROL_NO_NETWORK)
                    }
                }
            }
            .bodyAsChannel()
            .asSource()
            .asOkioSource()
    }

    fun customCoverOverride(options: Options, id: String, inLibrary: Boolean, key: String): FetchResult? {
        // Use custom cover if exists
        val useCustomCover = options.extras[USE_CUSTOM_COVER] != false
        if (useCustomCover && inLibrary) {
            val customCoverFile = coverCache.getCustomCoverFile(id)
            if (customCoverFile.exists()) {
                return SourceFetchResult(
                    source = ImageSource(
                        file = customCoverFile.toOkioPath(),
                        fileSystem = FileSystem.SYSTEM,
                        diskCacheKey = key,
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    companion object {
        val USE_CUSTOM_COVER = Extras.Key<Boolean>(true)

        private val CACHE_CONTROL_NO_STORE = NoStore(visibility = Visibility.Public)
        private val CACHE_CONTROL_NO_CACHE = NoCache(visibility = Visibility.Public)
        private const val CACHE_CONTROL_NO_NETWORK = "only-if-cached, max-stale=${Int.MAX_VALUE}"
    }
}
