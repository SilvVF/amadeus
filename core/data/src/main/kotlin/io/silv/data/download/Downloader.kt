package io.silv.data.download

import android.content.Context
import android.util.Log
import com.hippo.unifile.UniFile
import com.skydoves.sandwich.getOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.utils.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.silv.common.AmadeusDispatchers
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.ChapterResource
import io.silv.common.model.Download
import io.silv.common.model.MangaDexSource
import io.silv.common.model.MangaResource
import io.silv.common.model.Page
import io.silv.common.model.ProgressListener
import io.silv.data.util.DiskUtil
import io.silv.data.util.ImageUtil
import io.silv.datastore.DownloadStore
import io.silv.domain.chapter.interactor.GetChapter
import io.silv.domain.chapter.model.toResource
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.model.toResource
import io.silv.network.MangaDexApi
import io.silv.network.sources.ImageSourceFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.IOException
import okio.Source
import okio.buffer
import java.io.File

/**
 * This class is the one in charge of downloading chapters.
 *
 * Its queue contains the list of chapters to download.
 */
internal class Downloader(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val chapterCache: ChapterCache,
    private val imageSourceFactory: ImageSourceFactory,
    private val mangaDexApi: MangaDexApi,
    private val client: HttpClient,
    private val store: DownloadStore,
    private val getChapter: GetChapter,
    private val getManga: GetManga,
    amadeusDispatchers: AmadeusDispatchers,
) {
    val queue = DownloadQueue<Download, String>(
        workDispatcher = amadeusDispatchers.io,
        keySelector = { it.manga.id },
        store = object : QueueStore<Download> {
            override suspend fun addAll(downloads: List<Download>) = store.addAll(downloads)
            override suspend fun remove(download: Download) = store.remove(download)
            override suspend fun clear() = store.clear()
            override suspend fun removeAll(downloads: List<Download>) = store.removeAll(downloads)
            override suspend fun restore(): List<QItem<Download>> {
                return store.restore(
                    getManga = { getManga.await(it)?.toResource() },
                    getChapter = { getChapter.await(it)?.toResource() }
                )
                    .map(::QItem)
            }

        }
    ) { download ->
        suspendRunCatching {
            downloadChapter(download)
        }
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    suspend fun queueChapters(
        manga: MangaResource,
        chapters: List<ChapterResource>,
        autoStart: Boolean
    ) {
        Log.d("Downloader", "chapter empty = ${chapters.isEmpty()}")

        if (chapters.isEmpty()) return

        val chaptersToQueue = chapters.asSequence()
            // Filter out those already downloaded.
            .filter {
                provider.findChapterDir(
                    it.title,
                    it.scanlator,
                    manga.title,
                    MangaDexSource
                ) == null
            }
            // Add chapters to queue from the start.
            // Filter out those already enqueued.
            .filter { chapter -> queue.queueState.value.none { it.data.chapter.id == chapter.id } }
            // Create a download for each one.
            .map { Download(manga, it) }
            .toList()
            .map(::QItem)

        Log.d("Downloader", "chapters to queue = $chaptersToQueue")

        if (chaptersToQueue.isNotEmpty()) {
            queue.addAllToQueue(chaptersToQueue)

            DownloadWorker.start(context)
            Log.d("Downloader", "started download worker")
        }
    }

    private suspend fun getPageList(url: String, chapterId: String): List<Page> {
        return withContext(Dispatchers.IO) {
            when {
                url.isBlank() -> {
                    val response = mangaDexApi.getChapterImages(chapterId).getOrThrow()
                    response.chapter.data.mapIndexed { i, data ->
                        Page(
                            index = i,
                            url = url,
                            imageUrl = "${response.baseUrl}/data/${response.chapter.hash}/$data"
                        )
                    }
                }

                else -> imageSourceFactory.getSource(url).fetchImageUrls(url)
                    .mapIndexed { i, data ->
                        Page(
                            index = i,
                            url = url,
                            imageUrl = data,
                        )
                    }
            }
        }
    }

    /**
     * Downloads a chapter.
     *
     * @param download the chapter to be downloaded.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun downloadChapter(download: Download) {

        val mangaDir = provider.getMangaDir(download.manga.title, MangaDexSource)
        Log.d("Downloader", "dir $mangaDir")


        val availSpace = DiskUtil.getAvailableStorageSpace(mangaDir)

        Log.d("Downloader", "space $availSpace")

        if (availSpace != -1L && availSpace < MIN_DISK_SPACE) {
            Log.d("Downloader", "error disk space")
            error("no disk space")
            return
        }

        val chapterDirname =
            provider.getChapterDirName(download.chapter.title, download.chapter.scanlator)
        Log.d("Downloader", "chapterDirname $chapterDirname")
        val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)!!
        Log.d("Downloader", "tmp dir $tmpDir")
        // If the page list already exists, start from the file
        val pageList = download.pages ?: run {
            Log.d("Downloader", "getting page list bc empty")
            // Otherwise, pull page list from network and add them to download object
            val pages = getPageList(download.chapter.url, download.chapter.id)

            if (pages.isEmpty()) {
                throw Exception("empty Page list")
            }
            // Don't trust index from source
            val reIndexedPages =
                pages.mapIndexed { index, page -> Page(index, page.url, page.imageUrl) }
            download.pages = reIndexedPages
            reIndexedPages
        }

        // Delete all temporary (unfinished) files
        tmpDir.listFiles()
            ?.filter { it.extension == "tmp" }
            ?.forEach { it.delete() }

        // Start downloading images, consider we can have downloaded images already
        // Concurrently do 2 pages at a time
        pageList.asFlow()
            .flatMapMerge(concurrency = 2) { page ->
                flow<Page> {
                    withContext(Dispatchers.IO) {
                        Log.d("Downloader", "getting images")
                        getOrDownloadImage(page, download, tmpDir)
                    }
                    emit(page)
                }
                    .flowOn(Dispatchers.IO)
            }
            .collect {
                Log.d("Downloader", "page collected $it")
                // Do when page is downloaded.
                //notifier.onProgressChange(download)
            }

        // Do after download completes

        if (!isDownloadSuccessful(download, tmpDir)) {
            Log.d("Downloader", "not successful")
            error("failed to download")
            return
        }


        Log.d("Downloader", "rename")
        tmpDir.renameTo(chapterDirname)

        cache.addChapter(chapterDirname, mangaDir, download.manga)
        Log.d("Downloader", "added chapter to cache")

        DiskUtil.createNoMediaFile(tmpDir, context)
        Log.d("Downloader", "created no media file")
    }

    /**
     * Gets the image from the filesystem if it exists or downloads it otherwise.
     *
     * @param page the page to download.
     * @param download the download of the page.
     * @param tmpDir the temporary directory of the download.
     */
    private suspend fun getOrDownloadImage(page: Page, download: Download, tmpDir: UniFile) {
        // If the image URL is empty, do nothing
        if (page.imageUrl == null) {
            return
        }

        val digitCount = (download.pages?.size ?: 0).toString().length.coerceAtLeast(3)
        val filename = String.format("%0${digitCount}d", page.number)
        val tmpFile = tmpDir.findFile("$filename.tmp")

        // Delete temp file if it exists
        tmpFile?.delete()

        // Try to find the image file
        val imageFile = tmpDir.listFiles()?.firstOrNull {
            it.name!!.startsWith("$filename.") || it.name!!.startsWith("${filename}__001")
        }

        try {
            // If the image is already downloaded, do nothing. Otherwise download from network
            val file = when {
                imageFile != null -> imageFile
                chapterCache.isImageInCache(
                    page.imageUrl!!,
                ) -> copyImageFromCache(
                    chapterCache.getImageFilePath(page.imageUrl!!).toFile(),
                    tmpDir,
                    filename
                )

                else -> downloadImage(page, tmpDir, filename)
            }


            page.imageUrl = file.uri.toString()
            page.progress = 100
            page.status = Page.State.READY
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // Mark this page as error and allow to download the remaining
            page.progress = 0
            page.status = Page.State.ERROR
            // notifier.onError(e.message, download.chapter.name, download.manga.title)
        }
    }

    /**
     * Downloads the image from network to a file in tmpDir.
     *
     * @param page the page to download.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private suspend fun downloadImage(page: Page, tmpDir: UniFile, filename: String): UniFile {
        page.status = Page.State.DOWNLOAD_IMAGE
        page.progress = 0
        return flow {
            val response = withContext(Dispatchers.IO) {
                client.get {
                    url(page.imageUrl!!)
                    headers {
                        set(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
                    }
                    onDownload { bytesSentTotal, contentLength ->
                        runCatching {
                            page.update(
                                bytesSentTotal,
                                contentLength!!,
                                bytesSentTotal >= contentLength
                            )
                        }
                    }
                }
            }

            val file = tmpDir.createFile("$filename.tmp")!!
            try {
                val bytes: ByteArray = response.body()
                file.openOutputStream().use {
                    it.write(bytes)
                }
                val extension = getImageExtension(response, file)
                file.renameTo("$filename.$extension")
            } catch (e: Exception) {
                response.cancel()
                file.delete()
                throw e
            }
            emit(file)
        }
            // Retry 3 times, waiting 2, 4 and 8 seconds between attempts.
            .retryWhen { _, attempt ->
                if (attempt < 3) {
                    delay((2L shl attempt.toInt()) * 1000)
                    true
                } else {
                    false
                }
            }
            .first()
    }

    /**
     * Copies the image from cache to file in tmpDir.
     *
     * @param cacheFile the file from cache.
     * @param tmpDir the temporary directory of the download.
     * @param filename the filename of the image.
     */
    private fun copyImageFromCache(
        cacheFile: File,
        tmpDir: UniFile,
        filename: String
    ): UniFile {
        val tmpFile = tmpDir.createFile("$filename.tmp")!!
        cacheFile.inputStream().use { input ->
            tmpFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }
        val extension = ImageUtil.findImageType(cacheFile.inputStream()) ?: return tmpFile
        tmpFile.renameTo("$filename.${extension.extension}")
        cacheFile.delete()
        return tmpFile
    }

    /**
     * Returns the extension of the downloaded image from the network response, or if it's null,
     * analyze the file. If everything fails, assume it's a jpg.
     *
     * @param response the network response of the image.
     * @param file the file where the image is already downloaded.
     */
    private fun getImageExtension(response: HttpResponse, file: UniFile): String {
        // Read content type if available.
        val mime = response.headers["content-type"].takeIf { it?.contains("image") == true }
        return ImageUtil.getExtensionFromMimeType(mime) {
           file.openInputStream()
        }
    }


    /**
     * Checks if the download was successful.
     *
     * @param download the download to check.
     * @param tmpDir the directory where the download is currently stored.
     */
    private fun isDownloadSuccessful(
        download: Download,
        tmpDir: UniFile,
    ): Boolean {
        // Page list hasn't been initialized
        val downloadPageCount = download.pages?.size ?: return false

        // Ensure that all pages have been downloaded
        if (download.downloadedImages != downloadPageCount) {
            return false
        }

        // Ensure that the chapter folder has all the pages
        val downloadedImagesCount = tmpDir.listFiles().orEmpty().count {
            val fileName = it.name.orEmpty()
            when {
                fileName.endsWith(".tmp") -> false
                // Only count the first split page and not the others
                fileName.contains("__") && !fileName.endsWith("__001.jpg") -> false
                else -> true
            }
        }
        if (downloadedImagesCount != downloadPageCount) {
            return false
        }

        return true
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
    }
}

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024