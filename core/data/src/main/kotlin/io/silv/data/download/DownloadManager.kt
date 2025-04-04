package io.silv.data.download

import android.content.Context
import android.util.Log
import io.silv.common.model.ChapterResource
import io.silv.common.model.Download
import io.silv.common.model.MangaDexSource
import io.silv.common.model.MangaResource
import io.silv.common.model.Page
import io.silv.common.model.Source
import io.silv.domain.chapter.interactor.GetChapter
import io.silv.domain.chapter.model.toResource
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.model.toResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class DownloadManager internal constructor(
    private val context: Context,
    private val applicationScope: CoroutineScope,
    private val downloadProvider: DownloadProvider,
    private val downloader: Downloader,
    private val downloadCache: DownloadCache,
    private val getManga: GetManga,
    private val getChapter: GetChapter,
) {

    val cacheChanges = downloadCache.changes
    val queue = downloader.queue

    val isRunning: Boolean get() = queue.isRunning
    val queueState get() = queue.queueState

    // For use by DownloadService only
    fun downloaderStart() = queue.start()


    fun downloaderStop(reason: String? = null) = queue.stop(reason)

    val isDownloaderRunning
        get() = DownloadWorker.isRunningFlow(context)

    /**
     * Tells the downloader to begin downloads.
     */
    fun startDownloads() {
        applicationScope.launch {
            if (queue.isRunning) return@launch

            if (DownloadWorker.isRunning(context)) {
                queue.start()
            } else {
                DownloadWorker.start(context)
            }
        }
    }

    /**
     * Tells the downloader to pause downloads.
     */
    fun pauseDownloads() {
        queue.pause()
    }

    /**
     * Empties the download queue.
     */
    fun clearQueue() {
        queue.clear()
    }

    fun statusFlow(): Flow<QItem<Download>> = queue.statusFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    fun progressFlow(): Flow<QItem<Download>> = queueState
        .flatMapLatest { downloads ->
            downloads
                .map { download ->
                    download.data.progressFlow.drop(1).map { download }
                }
                .merge()
        }
        .onStart {
            emitAll(
                queueState.value.filter { download -> download.status == QItem.State.RUNNING }
                    .asFlow(),
            )
        }

    /**
     * Returns the download from queue if the chapter is queued for download
     * else it will return null which means that the chapter is not queued for download
     *
     * @param chapterId the chapter to check.
     */
    fun getQueuedDownloadOrNull(chapterId: String): QItem<Download>? {
        return queueState.value.find { it.data.chapter.id == chapterId }
    }

    private suspend fun downloadFromChapterId(id: String): Download? {

        val chapter = getChapter.await(id) ?: return null
        val manga = getManga.await(chapter.mangaId) ?: return null

        return Download(manga.toResource(), chapter.toResource())
    }

    fun startDownloadNow(chapterId: String) {
        applicationScope.launch {
            val existingDownload = getQueuedDownloadOrNull(chapterId)?.data
            // If not in queue try to start a new download
            val toAdd = existingDownload ?: downloadFromChapterId(chapterId) ?: return@launch
            queueState.value.map { it.data }.toMutableList().apply {
                existingDownload?.let { existing -> remove(existing) }
                add(0, toAdd)
                reorderQueue(this)
            }
            startDownloads()
        }
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<Download>) {
        applicationScope.launch {
            queue.updateQueue(downloads.map(::QItem))
        }
    }
    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun downloadChapters(manga: MangaResource, chapters: List<ChapterResource>, autoStart: Boolean = true) {
        applicationScope.launch {
            Log.d("DownloadManager", "queing chapters $chapters manga $manga auto start $autoStart")
            downloader.queueChapters(manga, chapters, autoStart)
        }
    }

    /**
     * Returns true if the chapter is downloaded.
     *
     * @param chapterName the name of the chapter to query.
     * @param chapterScanlator scanlator of the chapter to query
     * @param mangaTitle the title of the manga to query.
     * @param sourceId the id of the source of the chapter.
     * @param skipCache whether to skip the directory cache and check in the filesystem.
     */
    fun isChapterDownloaded(
        chapterName: String,
        chapterScanlator: String?,
        mangaTitle: String,
        sourceId: Long = MangaDexSource.id,
        skipCache: Boolean = false,
    ): Boolean {
        return downloadCache.isChapterDownloaded(chapterName, chapterScanlator, mangaTitle, sourceId, skipCache)
    }

    /**
     * Returns the amount of downloaded chapters.
     */
    fun getDownloadCount(): Int {
        return downloadCache.getTotalDownloadCount()
    }


    /**
     * Tells the downloader to enqueue the given list of downloads at the start of the queue.
     *
     * @param downloads the list of downloads to enqueue.
     */
    fun addDownloadsToStartOfQueue(downloads: List<Download>) {
        applicationScope.launch {
            if (downloads.isEmpty()) return@launch
            queueState.value.map { it.data }.toMutableList().apply {
                addAll(0, downloads)
                reorderQueue(this)
            }
            if (!DownloadWorker.isRunning(context)) startDownloads()
        }
    }




    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the downloaded chapter.
     * @return the list of pages from the chapter.
     */
    fun buildPageList(source: Source, manga: MangaResource, chapter: ChapterResource): List<Page> {
        val chapterDir = downloadProvider.findChapterDir(chapter.title, chapter.scanlator, manga.title, source)
        val files = chapterDir?.listFiles().orEmpty()
            .filter { "image" in it.type.orEmpty() }

        if (files.isEmpty()) {
            throw Exception("Pages were empty")
        }

        return files.sortedBy { it.name }
            .mapIndexed { i, file ->
                Page(i, imageUrl = file.uri.toString()).apply { status = Page.State.READY }
            }
    }


    fun isChapterDownloaded(chapterName: String, chapterScanlator: String?, mangaTitle: String): Boolean {
        return downloadProvider.findChapterDir(chapterName, chapterScanlator, mangaTitle, MangaDexSource) != null
    }

    fun cancelQueuedDownloads(downloads: List<Download>) {
        applicationScope.launch {
            removeFromDownloadQueue(downloads.map { it.chapter })
        }
    }

    /**
     * Deletes the directories of a list of downloaded chapters.
     *
     * @param chapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     * @param source the source of the chapters.
     */
    fun deleteChapters(chapters: List<ChapterResource>, manga: MangaResource) {
        applicationScope.launch(Dispatchers.IO) {
            if (chapters.isEmpty()) {
                return@launch
            }

            removeFromDownloadQueue(chapters)

            val (mangaDir, chapterDirs) = downloadProvider.findChapterDirs(
                chapters,
                manga.title,
                MangaDexSource
            )
            chapterDirs.forEach { it.delete() }
            downloadCache.removeChapters(chapters, manga)

            // Delete manga directory if empty
            if (mangaDir?.listFiles()?.isEmpty() == true) {
                deleteManga(manga, MangaDexSource, removeQueued = false)
            }
        }
    }

    /**
     * Deletes the directory of a downloaded manga.
     *
     * @param manga the manga to delete.
     * @param source the source of the manga.
     * @param removeQueued whether to also remove queued downloads.
     */
    fun deleteManga(manga: MangaResource, source: Source, removeQueued: Boolean = true) {
        applicationScope.launch(Dispatchers.IO) {
            if (removeQueued) {
                queue.removeFromQueueIf { item -> item.data.manga.id == manga.id }
            }
            downloadProvider.findMangaDir(manga.title, source)?.delete()
            downloadCache.removeManga(manga)

            // Delete source directory if empty
            val sourceDir = downloadProvider.findSourceDir(source)
            if (sourceDir?.listFiles()?.isEmpty() == true) {
                sourceDir.delete()
                downloadCache.removeSource(source)
            }
        }
    }

    private suspend fun removeFromDownloadQueue(chapters: List<ChapterResource>) {
        val wasRunning = queue.isRunning
        if (wasRunning) {
            queue.pause()
        }

        queue.removeFromQueueIf { item -> item.data.chapter.id in chapters.map(ChapterResource::id) }

        if (wasRunning) {
            if (queueState.value.isEmpty()) {
                queue.stop()
            } else if (queueState.value.isNotEmpty()) {
                queue.start()
            }
        }
    }
}

