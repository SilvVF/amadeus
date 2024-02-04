package io.silv.reader.loader

import io.silv.data.download.DownloadManager
import io.silv.domain.manga.model.Manga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loader used to retrieve the [PageLoader] for a given chapter.
 */
class ChapterLoader(
    private val downloadManager: DownloadManager,
    private val manga: Manga,
) {
    /**
     * Assigns the chapter's page loader and loads the its pages. Returns immediately if the chapter
     * is already loaded.
     */
    suspend fun loadChapter(chapter: ReaderChapter) {
        if (chapterIsReady(chapter)) {
            return
        }

        chapter.state = ReaderChapter.State.Loading
        withContext(Dispatchers.IO) {
            try {
                val loader = getPageLoader(chapter)
                chapter.pageLoader = loader

                val pages =
                    loader.getPages()
                        .onEach { it.chapter = chapter }

                if (pages.isEmpty()) {
                    throw Exception("empty page list")
                }

                // If the chapter is partially read, set the starting page to the last the user read
                // otherwise use the requested page.
                if (!chapter.chapter.read) {
                    chapter.requestedPage = chapter.chapter.lastReadPage ?: 0
                }

                chapter.state = ReaderChapter.State.Loaded(pages)
            } catch (e: Throwable) {
                chapter.state = ReaderChapter.State.Error(e)
                throw e
            }
        }
    }

    /**
     * Checks [chapter] to be loaded based on present pages and loader in addition to state.
     */
    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [chapter].
     */
    private fun getPageLoader(chapter: ReaderChapter): PageLoader {
        return when {
            downloadManager.isChapterDownloaded(
                chapter.chapter.title,
                chapter.chapter.scanlator,
                manga.titleEnglish,
            ) -> DownloadPageLoader(chapter, manga, downloadManager)
            else -> HttpPageLoader(chapter)
        }
    }
}
