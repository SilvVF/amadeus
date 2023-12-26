package eu.kanade.tachiyomi.reader.loader

import com.skydoves.sandwich.getOrThrow
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import eu.kanade.tachiyomi.reader.model.ReaderPage
import io.silv.common.ApplicationScope
import io.silv.common.model.ChapterResource
import io.silv.common.model.Page
import io.silv.data.download.ChapterCache
import io.silv.data.download.await
import io.silv.model.toResource
import io.silv.network.MangaDexApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes


@Serializable
data class AtHomeDto(
    val baseUrl: String,
    val chapter: AtHomeChapterDto,
)

@Serializable
data class AtHomeChapterDto(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
)

@Serializable
data class ImageReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Int?,
    val cached: Boolean,
    val duration: Long,
)


class HttpSource(
    private val mangaDexApi: MangaDexApi,
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val tokenTracker = mutableMapOf<String, Long>()

    fun getImageUrl(page: Page): String {
        return page.imageUrl ?: ""
    }

    /**
     * create an md at home Request
     */
    private fun mdAtHomeRequest(
        tokenRequestUrl: String,
        headers: Headers,
        cacheControl: CacheControl,
    ): Request {
        if (cacheControl == CacheControl.FORCE_NETWORK) {
            tokenTracker[tokenRequestUrl] = Date().time
        }
        return Request.Builder()
                .url(tokenRequestUrl)
                .headers(headers)
                .cacheControl(cacheControl)
                .build()
    }
    /**
     * Get the MD@Home URL.
     */
    private fun getMdAtHomeUrl(
        tokenRequestUrl: String,
        client: OkHttpClient,
        headers: Headers,
        cacheControl: CacheControl,
    ): String {
        val request = mdAtHomeRequest(tokenRequestUrl, headers, cacheControl)
        val response = client.newCall(request).execute()

        // This check is for the error that causes pages to fail to load.
        // It should never be entered, but in case it is, we retry the request.
        if (response.code == 504) {
            return getMdAtHomeUrl(tokenRequestUrl, client, headers, CacheControl.FORCE_NETWORK)
        }

        return response.use { json.decodeFromString<AtHomeDto>(it.body!!.string()).baseUrl }
    }

    /**
     * Check the token map to see if the MD@Home host is still valid.
     */
    private fun getValidImageUrlForPage(page: Page, headers: Headers): Request {
        val (host, tokenRequestUrl, time) = page.url.split(",")

        val mdAtHomeServerUrl =
            when (Date().time - time.toLong() > 5.minutes.inWholeMilliseconds) {
                false -> host
                true -> {
                    val tokenLifespan = Date().time - (tokenTracker[tokenRequestUrl] ?: 0)
                    val cacheControl = if (tokenLifespan > 5.minutes.inWholeMilliseconds) {
                        CacheControl.FORCE_NETWORK
                    } else {
                        CacheControl.FORCE_CACHE
                    }
                    getMdAtHomeUrl(tokenRequestUrl, client, headers, cacheControl)
                }
            }
        return Request.Builder().url(
            page.imageUrl!!.replaceBefore("/data", mdAtHomeServerUrl)
        ).headers(headers).build()
    }

    private val headers = Headers.Builder().apply {
        set("Referer", "${mangaDexApi.mangaDexUrl}/")
    }
        .build()

    suspend fun getImage(page: Page): Response {
        return client.newCall(
            getValidImageUrlForPage(page, headers)
        )
            .await()
    }

    suspend fun getPageList(chapter: ChapterResource): List<Page> {
        val response = mangaDexApi.getChapterImages(chapter.id).getOrThrow()
        val host = response.baseUrl
        val atHomeRequestUrl = "https://api.mangadex.org/at-home/server/${chapter.id}"

        // Have to add the time, and url to the page because pages timeout within 30 minutes now.
        val now = Date().time

        return response.chapter.data.mapIndexed { index, data ->
            Page(
                index = index,
                url = "$host,$atHomeRequestUrl,$now",
                imageUrl = "${response.baseUrl}/data/${response.chapter.hash}/$data",
            )
        }
    }
}

/**
 * Loader used to load chapters from an online source.
 */
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
) : PageLoader(), KoinComponent {

    private val chapterCache: ChapterCache by inject()
    private val applicationScope: ApplicationScope by inject()
    private val mangaDexApi: MangaDexApi by inject()
    private val client: OkHttpClient by inject()
    private val json: Json by inject()

    private val source: HttpSource = HttpSource(mangaDexApi, client, json)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * A queue used to manage requests one by one while allowing priorities.
     */
    private val queue = PriorityBlockingQueue<PriorityPage>()

    private val preloadSize = 4

    init {
        scope.launch(Dispatchers.IO) {
            flow {
                while (true) {
                    emit(runInterruptible { queue.take() }.page)
                }
            }
                .filter { it.status == Page.State.QUEUE }
                .collect(::internalLoadPage)
        }
    }

    override var isLocal: Boolean = false

    /**
     * Returns the page list for a chapter. It tries to return the page list from the local cache,
     * otherwise fallbacks to network.
     */
    override suspend fun getPages(): List<ReaderPage> {
        val pages = try {
            chapterCache.getPageListFromCache(chapter.chapter.toResource())
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            source.getPageList(chapter.chapter.toResource())
        }
        return pages.mapIndexed { index, page ->
            // Don't trust sources and use our own indexing
            ReaderPage(index, page.url, page.imageUrl)
        }
    }

    /**
     * Loads a page through the queue. Handles re-enqueueing pages if they were evicted from the cache.
     */
    override suspend fun loadPage(page: ReaderPage) = withContext(Dispatchers.IO) {
        val imageUrl = page.imageUrl

        // Check if the image has been deleted
        if (page.status == Page.State.READY && imageUrl != null && !chapterCache.isImageInCache(imageUrl)) {
            page.status = Page.State.QUEUE
        }

        // Automatically retry failed pages when subscribed to this page
        if (page.status == Page.State.ERROR) {
            page.status = Page.State.QUEUE
        }

        val queuedPages = mutableListOf<PriorityPage>()
        if (page.status == Page.State.QUEUE) {
            queuedPages += PriorityPage(page, 1).also { queue.offer(it) }
        }
        queuedPages += preloadNextPages(page, preloadSize)

        suspendCancellableCoroutine<Nothing> { continuation ->
            continuation.invokeOnCancellation {
                queuedPages.forEach {
                    if (it.page.status == Page.State.QUEUE) {
                        queue.remove(it)
                    }
                }
            }
        }
    }

    /**
     * Retries a page. This method is only called from user interaction on the viewer.
     */
    override fun retryPage(page: ReaderPage) {
        if (page.status == Page.State.ERROR) {
            page.status = Page.State.QUEUE
        }
        queue.offer(PriorityPage(page, 2))
    }

    override fun recycle() {
        super.recycle()
        scope.cancel()
        queue.clear()

        // Cache current page list progress for online chapters to allow a faster reopen
        chapter.pages?.let { pages ->
            applicationScope.launch(Dispatchers.IO) {
                try {
                    // Convert to pages without reader information
                    val pagesToSave = pages.map { Page(it.index, it.url, it.imageUrl) }
                    chapterCache.putPageListToCache(chapter.chapter.toResource(), pagesToSave)
                } catch (e: Throwable) {
                    if (e is CancellationException) {
                        throw e
                    }
                }
            }
        }
    }

    /**
     * Preloads the given [amount] of pages after the [currentPage] with a lower priority.
     *
     * @return a list of [PriorityPage] that were added to the [queue]
     */
    private fun preloadNextPages(currentPage: ReaderPage, amount: Int): List<PriorityPage> {
        val pageIndex = currentPage.index
        val pages = currentPage.chapter.pages ?: return emptyList()
        if (pageIndex == pages.lastIndex) return emptyList()

        return pages
            .subList(pageIndex + 1, min(pageIndex + 1 + amount, pages.size))
            .mapNotNull {
                if (it.status == Page.State.QUEUE) {
                    PriorityPage(it, 0).apply { queue.offer(this) }
                } else {
                    null
                }
            }
    }

    /**
     * Loads the page, retrieving the image URL and downloading the image if necessary.
     * Downloaded images are stored in the chapter cache.
     *
     * @param page the page whose source image has to be downloaded.
     */
    private suspend fun internalLoadPage(page: ReaderPage) {
        try {
            if (page.imageUrl.isNullOrEmpty()) {
                page.status = Page.State.LOAD_PAGE
                page.imageUrl = source.getImageUrl(page)
            }
            val imageUrl = page.imageUrl!!

            if (!chapterCache.isImageInCache(imageUrl)) {
                page.status = Page.State.DOWNLOAD_IMAGE
                val imageResponse = source.getImage(page)
                chapterCache.putImageToCache(imageUrl, imageResponse)
            }
            page.stream = { chapterCache.getImageFile(imageUrl).inputStream() }
            page.status = Page.State.READY
        } catch (e: Throwable) {
            e.printStackTrace()
            page.status = Page.State.ERROR
            if (e is CancellationException) {
                throw e
            }
        }
    }
}

/**
 * Data class used to keep ordering of pages in order to maintain priority.
 */
private class PriorityPage(
    val page: ReaderPage,
    val priority: Int,
) : Comparable<PriorityPage> {
    companion object {
        private val idGenerator = AtomicInteger()
    }

    private val identifier = idGenerator.incrementAndGet()

    override fun compareTo(other: PriorityPage): Int {
        val p = other.priority.compareTo(priority)
        return if (p != 0) p else identifier.compareTo(other.identifier)
    }
}