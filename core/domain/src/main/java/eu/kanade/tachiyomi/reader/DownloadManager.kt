package eu.kanade.tachiyomi.reader

import android.content.Context
import eu.kanade.tachiyomi.Download
import eu.kanade.tachiyomi.DownloadJob
import eu.kanade.tachiyomi.Downloader
import eu.kanade.tachiyomi.MangaDexSource
import eu.kanade.tachiyomi.reader.cache.DownloadCache
import eu.kanade.tachiyomi.reader.model.Page
import eu.kanade.tachiyomi.reader.model.Source
import io.silv.common.ApplicationScope
import io.silv.domain.chapter.GetChapter
import io.silv.domain.manga.GetManga
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DownloadManager(
    private val context: Context,
    private val downloadProvider: DownloadProvider,
    private val downloader: Downloader,
    private val getManga: GetManga,
    private val getChapter: GetChapter,
    private val downloadCache: DownloadCache,
    private val applicationScope: ApplicationScope,
) {

    val isRunning: Boolean
        get() = downloader.isRunning

    val queueState
        get() = downloader.queueState

    // For use by DownloadService only
    fun downloaderStart() = downloader.start()

    fun downloaderStop(reason: String? = null) = downloader.stop(reason)

    /**
     * Tells the downloader to begin downloads.
     */
    fun startDownloads() {
        if (downloader.isRunning) return

        if (DownloadJob.isRunning(context)) {
            downloader.start()
        } else {
            DownloadJob.start(context)
        }
    }

    /**
     * Tells the downloader to pause downloads.
     */
    fun pauseDownloads() {
        downloader.pause()
        downloader.stop()
    }

    /**
     * Empties the download queue.
     */
    fun clearQueue() {
        downloader.clearQueue()
        downloader.stop()
    }
    /**
     * Returns the download from queue if the chapter is queued for download
     * else it will return null which means that the chapter is not queued for download
     *
     * @param chapterId the chapter to check.
     */
    fun getQueuedDownloadOrNull(chapterId: String): Download? {
        return queueState.value.find { it.chapter.id == chapterId }
    }

    fun startDownloadNow(chapterId: String) {
        val existingDownload = getQueuedDownloadOrNull(chapterId)
        // If not in queue try to start a new download
        val toAdd = existingDownload ?: runBlocking { Download.fromChapterId(chapterId, getChapter, getManga) } ?: return
        queueState.value.toMutableList().apply {
            existingDownload?.let { remove(it) }
            add(0, toAdd)
            reorderQueue(this)
        }
        startDownloads()
    }

    /**
     * Reorders the download queue.
     *
     * @param downloads value to set the download queue to
     */
    fun reorderQueue(downloads: List<Download>) {
        downloader.updateQueue(downloads)
    }
    /**
     * Tells the downloader to enqueue the given list of chapters.
     *
     * @param manga the manga of the chapters.
     * @param chapters the list of chapters to enqueue.
     * @param autoStart whether to start the downloader after enqueing the chapters.
     */
    fun downloadChapters(manga: SavableManga, chapters: List<SavableChapter>, autoStart: Boolean = true) {
        downloader.queueChapters(manga, chapters, autoStart)
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
        if (downloads.isEmpty()) return
        queueState.value.toMutableList().apply {
            addAll(0, downloads)
            reorderQueue(this)
        }
        if (!DownloadJob.isRunning(context)) startDownloads()
    }




    /**
     * Builds the page list of a downloaded chapter.
     *
     * @param source the source of the chapter.
     * @param manga the manga of the chapter.
     * @param chapter the downloaded chapter.
     * @return the list of pages from the chapter.
     */
    fun buildPageList(source: Source, manga: SavableManga, chapter: SavableChapter): List<Page> {
        val chapterDir = downloadProvider.findChapterDir(chapter.title, chapter.scanlator, manga.titleEnglish, source)
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


    /**
     * Returns the amount of downloaded chapters for a manga.
     *
     * @param manga the manga to check.
     */
    fun getDownloadCount(manga: SavableManga): Int {
        return downloadCache.getDownloadCount(manga)
    }

    fun cancelQueuedDownloads(downloads: List<Download>) {
        removeFromDownloadQueue(downloads.map { it.chapter })
    }

    /**
     * Deletes the directories of a list of downloaded chapters.
     *
     * @param chapters the list of chapters to delete.
     * @param manga the manga of the chapters.
     * @param source the source of the chapters.
     */
    fun deleteChapters(chapters: List<SavableChapter>, manga: SavableManga) {
        applicationScope.launch(Dispatchers.IO) {
            val filteredChapters = getChaptersToDelete(chapters, manga)
            if (filteredChapters.isEmpty()) {
                return@launch
            }

            removeFromDownloadQueue(filteredChapters)

            val (mangaDir, chapterDirs) = downloadProvider.findChapterDirs(filteredChapters, manga, MangaDexSource)
            chapterDirs.forEach { it.delete() }
            downloadCache.removeChapters(filteredChapters, manga)

            // Delete manga directory if empty
            if (mangaDir?.listFiles()?.isEmpty() == true) {
                deleteManga(manga, MangaDexSource, removeQueued = false)
            }
        }
    }
    private suspend fun getChaptersToDelete(chapters: List<SavableChapter>, manga: SavableManga): List<SavableChapter> {
        return emptyList()
    }

    /**
     * Deletes the directory of a downloaded manga.
     *
     * @param manga the manga to delete.
     * @param source the source of the manga.
     * @param removeQueued whether to also remove queued downloads.
     */
    fun deleteManga(manga: SavableManga, source: Source, removeQueued: Boolean = true) {
        applicationScope.launch(Dispatchers.IO) {
            if (removeQueued) {
                downloader.removeFromQueue(manga)
            }
            downloadProvider.findMangaDir(manga.titleEnglish, source)?.delete()
            downloadCache.removeManga(manga)

            // Delete source directory if empty
            val sourceDir = downloadProvider.findSourceDir(source)
            if (sourceDir?.listFiles()?.isEmpty() == true) {
                sourceDir.delete()
                downloadCache.removeSource(source)
            }
        }
    }

    private fun removeFromDownloadQueue(chapters: List<SavableChapter>) {
        val wasRunning = downloader.isRunning
        if (wasRunning) {
            downloader.pause()
        }

        downloader.removeFromQueue(chapters)

        if (wasRunning) {
            if (queueState.value.isEmpty()) {
                downloader.stop()
            } else if (queueState.value.isNotEmpty()) {
                downloader.start()
            }
        }
    }
}

