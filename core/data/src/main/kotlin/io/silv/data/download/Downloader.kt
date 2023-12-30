package io.silv.data.download

import android.content.Context
import android.util.Log
import com.hippo.unifile.UniFile
import com.skydoves.sandwich.getOrThrow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import io.silv.common.ApplicationScope
import io.silv.common.model.ChapterResource
import io.silv.common.model.Download
import io.silv.common.model.MangaDexSource
import io.silv.common.model.MangaResource
import io.silv.common.model.Page
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
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

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
    private val httpClient: HttpClient,
    private val store: DownloadStore,
    private val getChapter: GetChapter,
    private val getManga: GetManga,
    applicationScope: ApplicationScope,
) {


    /**
     * Queue where active downloads are kept.
     */
    private val _queueState = MutableStateFlow<List<Download>>(emptyList())
    val queueState = _queueState.asStateFlow()


    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloaderJob: Job? = null

    /**
     * Whether the downloader is running.
     */
    val isRunning: Boolean
        get() = downloaderJob?.isActive ?: false

    /**
     * Whether the downloader is paused
     */
    @Volatile
    var isPaused: Boolean = false

    init {
        applicationScope.launch {
            val chapters = async {
                store.restore(
                    getManga = { getManga.await(it)?.toResource() },
                    getChapter = { getChapter.await(it)?.toResource() }
                )
            }
            addAllToQueue(chapters.await())
        }
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) {
            return false
        }

        val pending = queueState.value.filter { it.status != Download.State.DOWNLOADED }
        pending.forEach { if (it.status != Download.State.QUEUE) it.status = Download.State.QUEUE }

        isPaused = false

        launchDownloaderJob()

        return pending.isNotEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.ERROR }

        if (reason != null) {
            return
        }

//        if (isPaused && queueState.value.isNotEmpty()) {
//           // notifier.onPaused()
//        } else {
//           // notifier.onComplete()
//        }

        isPaused = false

        DownloadWorker.stop(context)
    }

    /**
     * Pauses the downloader
     */
    fun pause() {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.status = Download.State.QUEUE }
        isPaused = true
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() = scope.launch {
        cancelDownloaderJob()

        _clearQueue()
            // notifier.dismissProgress()
    }

    /**
     * Prepares the subscriptions to start downloading.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun launchDownloaderJob() {
        if (isRunning) return

        downloaderJob = scope.launch {
            val activeDownloadsFlow = queueState.transformLatest { queue ->
                while (true) {
                    val activeDownloads = queue.asSequence()
                        .filter { it.status.value <= Download.State.DOWNLOADING.value } // Ignore completed downloads, leave them in the queue
                        .groupBy { MangaDexSource }
                        .toList().take(5) // Concurrently download from 5 different sources
                        .map { (_, downloads) -> downloads.first() }
                    emit(activeDownloads)

                    if (activeDownloads.isEmpty()) break
                    // Suspend until a download enters the ERROR state
                    val activeDownloadsErroredFlow =
                        combine(activeDownloads.map(Download::statusFlow)) { states ->
                            states.contains(Download.State.ERROR)
                        }
                            .filter { it }
                    activeDownloadsErroredFlow.first()
                }
            }
                .distinctUntilChanged()

            // Use supervisorScope to cancel child jobs when the downloader job is cancelled
            supervisorScope {
                val downloadJobs = mutableMapOf<Download, Job>()

                activeDownloadsFlow.collectLatest { activeDownloads ->

                    val downloadJobsToStop = downloadJobs.filter { it.key !in activeDownloads }

                    downloadJobsToStop.forEach { (download, job) ->
                        job.cancel()
                        downloadJobs.remove(download)
                    }

                    val downloadsToStart = activeDownloads.filter { it !in downloadJobs }
                    downloadsToStart.forEach { download ->
                        downloadJobs[download] = launchDownloadJob(download)
                    }
                }
            }
        }
    }

    private fun CoroutineScope.launchDownloadJob(download: Download) = launch(Dispatchers.IO) {
        try {
            downloadChapter(download)

            // Remove successful download from queue
            if (download.status == Download.State.DOWNLOADED) {
                removeFromQueue(download)
            }
            if (areAllDownloadsFinished()) {
                stop()
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            //logcat(LogPriority.ERROR, e)
           // notifier.onError(e.message)
            stop()
        }
    }

    /**
     * Destroys the downloader subscriptions.
     */
    private fun cancelDownloaderJob() {
        downloaderJob?.cancel()
        downloaderJob = null
    }

    /**
     * Creates a download object for every chapter and adds them to the downloads queue.
     *
     * @param manga the manga of the chapters to download.
     * @param chapters the list of chapters to download.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    suspend fun queueChapters(manga: MangaResource, chapters: List<ChapterResource>, autoStart: Boolean) {

            Log.d("Downloader", "chapter empty = ${chapters.isEmpty()}")

            if (chapters.isEmpty()) return

            val chaptersToQueue = chapters.asSequence()
                // Filter out those already downloaded.
                .filter { provider.findChapterDir(it.title, it.scanlator, manga.title, MangaDexSource) == null }
                // Add chapters to queue from the start.
                // Filter out those already enqueued.
                .filter { chapter -> queueState.value.none { it.chapter.id == chapter.id } }
                // Create a download for each one.
                .map { Download(manga, it) }
                .toList()

            Log.d("Downloader", "chapters to queue = $chaptersToQueue")

            if (chaptersToQueue.isNotEmpty()) {
                addAllToQueue(chaptersToQueue)

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
            download.status = Download.State.ERROR
            Log.d("Downloader", "error disk space")
            return
        }

        val chapterDirname = provider.getChapterDirName(download.chapter.title, download.chapter.scanlator)
        Log.d("Downloader", "chapterDirname $chapterDirname")
        val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)!!
        Log.d("Downloader", "tmp dir $tmpDir")
        try {
            Log.d("Downloader", "page list")
            // If the page list already exists, start from the file
            val pageList = download.pages ?: run {
                Log.d("Downloader", "getting page list bc empty")
                // Otherwise, pull page list from network and add them to download object
                val pages = getPageList(download.chapter.url, download.chapter.id)

                if (pages.isEmpty()) {
                    throw Exception("empty Page list")
                }
                // Don't trust index from source
                val reIndexedPages = pages.mapIndexed { index, page -> Page(index, page.url, page.imageUrl) }
                download.pages = reIndexedPages
                reIndexedPages
            }

            // Delete all temporary (unfinished) files
            tmpDir.listFiles()
                ?.filter { it.extension == "tmp" }
                ?.forEach { it.delete() }

            download.status = Download.State.DOWNLOADING

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
                download.status = Download.State.ERROR
                return
            }


            Log.d("Downloader", "rename")
            tmpDir.renameTo(chapterDirname)

            cache.addChapter(chapterDirname, mangaDir, download.manga)
            Log.d("Downloader", "added chapter to cache")

            DiskUtil.createNoMediaFile(tmpDir, context)
            Log.d("Downloader", "created no media file")

            download.status = Download.State.DOWNLOADED
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            // If the page list threw, it will resume here
            download.status = Download.State.ERROR
            //notifier.onError(error.message, download.chapter.name, download.manga.title)
        }
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
                ) -> copyImageFromCache(chapterCache.getImageFile(page.imageUrl!!), tmpDir, filename)
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
            val response =  httpClient.get(page.imageUrl!!)
            val file = tmpDir.createFile("$filename.tmp")!!
            try {
                file.openOutputStream().use {
                    response.bodyAsChannel().copyTo(it)
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
    private fun copyImageFromCache(cacheFile: File, tmpDir: UniFile, filename: String): UniFile {
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
           ?: context.contentResolver.getType(file.uri)
           ?: ImageUtil.findImageType { file.openInputStream() }?.mime
        return ImageUtil.getExtensionFromMimeType(mime)
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

    /**
     * Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
     */
    private fun areAllDownloadsFinished(): Boolean {
        return queueState.value.none { it.status.value <= Download.State.DOWNLOADING.value }
    }

    private suspend fun addAllToQueue(downloads: List<Download>) {
        _queueState.update {
            downloads.forEach { download ->
                download.status = Download.State.QUEUE
            }
            store.addAll(downloads)
            it + downloads
        }
    }

    private suspend fun removeFromQueue(download: Download) {
        _queueState.update {
            store.remove(download)
            if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                download.status = Download.State.NOT_DOWNLOADED
            }
            it - download
        }
    }

    private suspend inline fun removeFromQueueIf(predicate: (Download) -> Boolean) {
        _queueState.update { queue ->
            val downloads = queue.filter { predicate(it) }
            store.removeAll(downloads)
            downloads.forEach { download ->
                if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                    download.status = Download.State.NOT_DOWNLOADED
                }
            }
            queue - downloads
        }
    }

    suspend fun removeFromQueue(chapters: List<ChapterResource>) {
        val chapterIds = chapters.map { it.id }
        removeFromQueueIf { it.chapter.id in chapterIds }
    }

    suspend fun removeFromQueue(manga: MangaResource) {
        removeFromQueueIf { it.manga.id == manga.id }
    }

    private suspend fun _clearQueue() {
        _queueState.update {
            it.forEach { download ->
                if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                    download.status = Download.State.NOT_DOWNLOADED
                }
            }
            store.clear()
            emptyList()
        }
    }

    suspend fun updateQueue(downloads: List<Download>) {
        val wasRunning = isRunning

        if (downloads.isEmpty()) {
            clearQueue()
            stop()
            return
        }

        pause()
        _clearQueue()
        addAllToQueue(downloads)

        if (wasRunning) {
            start()
        }
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
//        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
//        const val CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 15
//        private const val DOWNLOADS_QUEUED_WARNING_THRESHOLD = 30
    }
}

// Arbitrary minimum required space to start a download: 200 MB
private const val MIN_DISK_SPACE = 200L * 1024 * 1024