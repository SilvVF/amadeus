package io.silv.reader.loader

import io.ktor.client.utils.CacheControl
import io.ktor.http.HttpHeaders
import io.silv.common.ApplicationScope
import io.silv.common.model.Page
import io.silv.data.download.ChapterCache
import io.silv.domain.chapter.model.toResource
import io.silv.network.sources.HttpSource
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.PriorityBlockingQueue
import kotlin.math.min


/**
 * Loader used to load chapters from an online source.
 */
internal class HttpPageLoader(
    private val chapter: ReaderChapter,
) : PageLoader(), KoinComponent {

    private val chapterCache: ChapterCache by inject()
    private val applicationScope: ApplicationScope by inject()
    private val source: HttpSource by inject()

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

                val imageResponse = source.getImage(
                    page,
                    headers = listOf(HttpHeaders.CacheControl to CacheControl.NO_CACHE)
                )


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
