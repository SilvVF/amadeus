package io.silv.reader

import android.util.Log
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.reader.model.ReaderChapter
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.model.Download
import io.silv.common.model.Page
import io.silv.data.download.DownloadManager
import io.silv.domain.chapter.ChapterHandler
import io.silv.domain.manga.GetSavableMangaWithChapters
import io.silv.model.SavableChapter
import io.silv.model.SavableManga
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderScreenModel(
    private val getSavableMangaWithChapters: GetSavableMangaWithChapters,
    private val chapterHandler: ChapterHandler,
    private val downloadManager: DownloadManager,
    private val mangaId: String,
    private val initialChapterId: String,
) : StateScreenModel<ReaderState>(ReaderState()) {

    private val chapterList = mutableListOf<ReaderChapter>()

    private var loader: ChapterLoader? = null

    private var chapterToDownload: Download? = null

    init {
        initializeReader()
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    private fun initializeReader() {
        screenModelScope.launch {
            suspendRunCatching {

                if (state.value.manga != null && chapterList.isNotEmpty())
                    return@suspendRunCatching

                val (manga, chapters) = getSavableMangaWithChapters.await(mangaId)!!

                val scanlator = chapters.find { it.id == initialChapterId }!!.scanlator

                mutableState.update { it.copy(chapters = chapters) }

                chapterList.addAll(
                    chapters
                        .filter { it.scanlator == scanlator }
                        .map(::ReaderChapter)
                )

                val chapter = chapterList.first { it.chapter.id == initialChapterId }

                Log.d("Reader", "$chapter")
                Log.d("Reader", manga.id)

                mutableState.update {
                    it.copy(manga = manga)
                }
                loader = ChapterLoader(downloadManager, manga)
                loadChapter(loader!!, chapter)
            }.onFailure {e ->
                e.printStackTrace()
            }.onSuccess {

            }
        }
    }


    /**
     * Called when the user is going to load the prev/next chapter through the toolbar buttons.
     */
    private suspend fun loadAdjacent(chapter: ReaderChapter) {

        val loader = loader ?: return

        mutableState.update { it.copy(loadingAdjacent = true) }

        runCatching {
            withContext(Dispatchers.IO) {
                loadChapter(loader, chapter)
            }
        }.onFailure { e ->
            if (e is CancellationException) {
                mutableState.update { it.copy(loadingAdjacent = false) }
                throw e
            }
        }
        mutableState.update { it.copy(loadingAdjacent = false) }
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    fun loadNextChapter() {
        screenModelScope.launch {
            val nextChapter = state.value.viewerChapters?.nextChapter ?: return@launch
            loadAdjacent(nextChapter)
        }
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    fun loadPreviousChapter() {
        screenModelScope.launch {
            val prevChapter = state.value.viewerChapters?.prevChapter ?: return@launch
            loadAdjacent(prevChapter)
        }
    }
    /**
     * Removes [currentChapter] from download queue
     * if setting is enabled and [currentChapter] is queued for download
     */
    private fun cancelQueuedDownloads(currentChapter: ReaderChapter): Download? {
        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id)?.also {
            downloadManager.cancelQueuedDownloads(listOf(it))
        }
    }

    /**
     * Loads the given [chapter] with this [loader] and updates the currently active chapters.
     * Callers must handle errors.
     */
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ): ViewerChapters {

        Log.d("Reader", "loading chapter $chapter")

        loader.loadChapter(chapter)

        val chapterPos = chapterList.indexOf(chapter)
        val newChapters = ViewerChapters(
            chapter,
            chapterList.getOrNull(chapterPos - 1),
            chapterList.getOrNull(chapterPos + 1),
        )

        withContext(Dispatchers.Main.immediate) {
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                it.viewerChapters?.unref()

                chapterToDownload = cancelQueuedDownloads(newChapters.currChapter)
                it.copy(
                    viewerChapters = newChapters,
                )
            }
        }
        return newChapters
    }

    fun pageChanged(
        readerChapter: ReaderChapter,
        page: Page
    ) {
        screenModelScope.launch {
            updateChapterProgress(readerChapter, page)
        }
    }

    /**
     * Saves the chapter progress (last read page and whether it's read)
     * if incognito mode isn't on.
     */
    private suspend fun updateChapterProgress(
        readerChapter: ReaderChapter,
        page: Page
    ) {
        val pageIndex = page.index

        readerChapter.requestedPage = pageIndex

        if (page.status != Page.State.ERROR) {

            chapterHandler.updateLastReadPage(
                readerChapter.chapter.id,
                pageIndex,
                isLast = readerChapter.pages?.lastIndex == pageIndex
            )
        }
    }

    override fun onDispose() {
        val currentChapters = state.value.viewerChapters
        if (currentChapters != null) {
            currentChapters.unref()
            chapterToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
        super.onDispose()
    }

}

data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
) {

    fun ref() {
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}

@Immutable
data class ReaderState(
    val viewerChapters: ViewerChapters? = null,
    val manga: SavableManga? = null,
    val loadingAdjacent: Boolean = false,
    val chapters: ImmutableList<SavableChapter> = persistentListOf()
)
