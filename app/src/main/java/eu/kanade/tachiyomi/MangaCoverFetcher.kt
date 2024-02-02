package eu.kanade.tachiyomi

import android.content.Context
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import eu.kanade.tachiyomi.MangaCoverFetcher.Companion.USE_CUSTOM_COVER
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.CacheControl.NoCache
import io.ktor.http.CacheControl.NoStore
import io.ktor.http.CacheControl.Visibility
import io.ktor.http.HttpHeaders
import io.silv.amadeus.coil.DiskBackedFetcher
import io.silv.amadeus.coil.FetcherDiskStore
import io.silv.amadeus.coil.FetcherDiskStoreImageFile
import io.silv.common.model.MangaCover
import io.silv.data.download.CoverCache
import io.silv.domain.manga.model.Manga
import okio.Path.Companion.toOkioPath

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
    private val ctx: Context,
) {

    private val mangaKeyer = MangaKeyer()
    private val coverKeyer = MangaCoverKeyer(coverCache)

    val coverFetcher: DiskBackedFetcher<MangaCover> = object: DiskBackedFetcher<MangaCover> {
        override val context: Context = ctx
        override val keyer: Keyer<MangaCover> = coverKeyer
        override val diskStore: FetcherDiskStore<MangaCover> =
            FetcherDiskStoreImageFile { data, _ ->
                if (data.isMangaFavorite) {
                    coverCache.getCoverFile(data.url)
                } else {
                    null
                }
            }
        override suspend fun fetch(options: Options, data: MangaCover): ByteArray = fetchManga(options, data.url)
        override suspend fun overrideFetch(options: Options, data: MangaCover): FetchResult? {
            return customCoverOverride(options, data.mangaId, coverKeyer.key(data, options))
        }
    }

    val mangaFetcher: DiskBackedFetcher<Manga> = object: DiskBackedFetcher<Manga> {
        override val context: Context = ctx
        override val keyer: Keyer<Manga> = mangaKeyer
        override val diskStore: FetcherDiskStore<Manga> =
            FetcherDiskStoreImageFile { data, _ ->
                if (data.inLibrary) {
                    coverCache.getCoverFile(data.coverArt)
                } else {
                    null
                }
            }
        override suspend fun fetch(options: Options, data: Manga): ByteArray = fetchManga(options, data.coverArt)
        override suspend fun overrideFetch(options: Options, data: Manga): FetchResult? {
            return customCoverOverride(options, data.id, mangaKeyer.key(data, options))
        }
    }


    suspend fun fetchManga(options: Options, url: String): ByteArray {
        return client
            .get(url) {
                for (header in options.headers) {
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
            .body()
    }

    fun customCoverOverride(options: Options, id: String, key: String): FetchResult? {
        // Use custom cover if exists
        val useCustomCover = options.parameters.value(USE_CUSTOM_COVER) ?: true
        if (useCustomCover) {
            val customCoverFile = coverCache.getCustomCoverFile(id)
            if (customCoverFile.exists()) {
                return SourceResult(
                    source = ImageSource(
                        file = customCoverFile.toOkioPath(),
                        diskCacheKey = key
                    ),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }
        }
        return null
    }

    companion object {
        const val USE_CUSTOM_COVER = "use_custom_cover"

        private val CACHE_CONTROL_NO_STORE = NoStore(visibility = Visibility.Public)
        private val CACHE_CONTROL_NO_CACHE = NoCache(visibility = Visibility.Public)
        private const val CACHE_CONTROL_NO_NETWORK = "only-if-cached, max-stale=${Int.MAX_VALUE}"
    }
}
