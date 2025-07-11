package io.silv.reader2

import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.coroutine.suspendRunCatching
import io.silv.common.log.LogPriority
import io.silv.common.log.asLog
import io.silv.common.log.logcat
import io.silv.common.model.Download
import io.silv.common.model.Page
import io.silv.common.time.epochMillis
import io.silv.common.time.localDateTimeNow
import io.silv.data.chapter.Chapter
import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.interactor.GetNextChapters
import io.silv.data.chapter.toResource
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.QItem
import io.silv.data.history.HistoryRepository
import io.silv.data.history.HistoryUpdate
import io.silv.data.manga.interactor.GetChaptersByMangaId
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.interactor.SetMangaViewerFlags
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource
import io.silv.datastore.SettingsStore
import io.silv.di.dataDeps
import io.silv.reader.loader.ChapterLoader
import io.silv.reader.loader.DownloadPageLoader
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader2.Reader2ScreenModel.Event
import io.silv.ui.AppState
import io.silv.ui.EventStateScreenModel
import io.silv.ui.SavedStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.cancellation.CancellationException


/**
 * Presenter used by the activity to perform background operations.
 */
class Reader2ScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val mangaId: String,
    private var chapterId: String,
    private var chapterPageIndex: Int,
    private val appState: AppState,
    private val downloadManager: DownloadManager = dataDeps.downloadManager,
    private val downloadProvider: DownloadProvider = dataDeps.downloadProvider,
    // private val imageSaver: ImageSaver,
    private val store: SettingsStore = dataDeps.settingsStore,
    //private val trackChapter: TrackChapter = Injekt.get(),
    private val getManga: GetManga = dataDeps.getManga,
    private val getChaptersByMangaId: GetChaptersByMangaId = dataDeps.getChaptersByMangaId,
    private val getNextChapters: GetNextChapters = dataDeps.getNextChapters,
    private val historyRepository: HistoryRepository = dataDeps.historyRepository,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    private val setMangaViewerFlags: SetMangaViewerFlags = dataDeps.setMangaViewerFlags,
    private val saveState: (state: MutableMap<String, Any>) -> Unit
) : EventStateScreenModel<Event, Reader2ScreenModel.State>(State()), SavedStateScreenModel {

    companion object {
        const val CHAPTER_KEY = "chapterId"
        const val MANGA_KEY = "mangaId"
        const val PAGE_KEY = "page"
    }

    override fun restoreStateMap(): MutableMap<String, Any> {
        return mutableMapOf(
            MANGA_KEY to mangaId,
            CHAPTER_KEY to chapterId,
            PAGE_KEY to chapterPageIndex
        )
    }

    override fun saveStateMap(state: MutableMap<String, Any>) = saveState(state)

    val manga: Manga? get() = state.value.manga
    private val loader: ChapterLoader by lazy { ChapterLoader(manga!!) }

    private var chapterReadStartTime: Long? = null
    private var chapterToDownload: Download? = null

    val viewer = PagerViewer(scope = screenModelScope) { action ->
        when (action) {
            PagerAction.ToggleMenu -> showMenus(!state.value.menuVisible)
            is PagerAction.OnPageSelected -> onPageSelected(action.page)
            is PagerAction.RequestPreloadChapter -> screenModelScope.launch {
                preload(action.chapter)
            }

            PagerAction.ShowMenu -> showMenus(true)
            is PagerAction.RetryLoad -> screenModelScope.launch {
                preload(action.chapter)
            }
        }
    }

    val settings = ReaderSettingsPresenter(screenModelScope, store)

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        runBlocking {
            getChaptersByMangaId.await(mangaId)
                .groupBy { it.scanlatorOrNull }
                .mapValues { (_, value) ->
                    value.sortedBy { it.chapter }
                }
                .values
                .flatten()
                .map(::ReaderChapter)
        }
    }

    private val incognitoMode = false
    private val downloadAheadAmount = 4

    init {
        settings.state.onEach {
            mutableState.update { state ->
                state.copy(settings = it)
            }
        }
            .launchIn(screenModelScope)

        snapshotFlow { appState.navigator?.lastItemOrNull }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach {
                if (it !is ReaderScreen2) {
                    deletePendingChapters()
                }
            }
            .launchIn(screenModelScope)
    }

    override fun onDispose() {
        val currentChapters = viewer.viewerChapters
        if (currentChapters != null) {
            currentChapters.unref()
            chapterToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
    }

    fun needsInit(): Boolean {
        return manga == null
    }

    suspend fun init(mangaId: String, savedChapter: String): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return suspendRunCatching {
            withContext(Dispatchers.IO) {
                val manga = getManga.await(mangaId)
                if (manga != null) {
                    mutableState.update {
                        it.copy(
                            manga = manga,
                            chapters = chapterList.map { it.chapter }
                        )
                    }
                    logcat { "chapterid $savedChapter initial $chapterId" }
                    if (savedChapter.isNotEmpty()) chapterId = savedChapter

                    loadChapter(loader, chapterList.first { chapterId == it.chapter.id })
                    true
                } else {
                    // Unlikely but okay
                    false
                }
            }
        }
    }

    /**
     * Loads the given [chapter] with this [loader] and updates the currently active chapters.
     * Callers must handle errors.
     */
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ) {
        loader.loadChapter(chapter)

        val chapterPos = chapterList.indexOf(chapter)
        val newChapters = ViewerChapters(
            chapter,
            chapterList.getOrNull(chapterPos - 1),
            chapterList.getOrNull(chapterPos + 1),
        )

        withContext(Dispatchers.Main) {
            newChapters.ref()
            viewer.viewerChapters?.currChapter?.let { cancelQueuedDownloads(it) }

            mutableState.update {
                it.copy(
                    bookmarked = newChapters.currChapter.chapter.bookmarked,
                )
            }

            viewer.viewerChapters?.unref()
            viewer.setChapters(newChapters)
        }
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        ioCoroutineScope.launch {
            logcat { "Loading ${chapter.chapter.url}" }

            flushReadTimer()
            restartReadTimer()

            try {
                loadChapter(loader, chapter)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                logcat(LogPriority.ERROR) { e.asLog() }
            }
        }
    }

    /**
     * Called when the user is going to load the prev/next chapter through the toolbar buttons.
     */
    private suspend fun loadAdjacent(chapter: ReaderChapter) {
        logcat { "Loading adjacent ${chapter.chapter.url}" }

        mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
        try {
            withContext(Dispatchers.IO) {
                loadChapter(loader, chapter)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            logcat(LogPriority.ERROR) { e.asLog() }
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
        }
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    suspend fun preload(chapter: ReaderChapter) {
        if (chapter.state is ReaderChapter.State.Loaded || chapter.state == ReaderChapter.State.Loading) {
            logcat { "chapter already loading ${chapter.chapter}" }
            return
        }

        if (chapter.pageLoader?.isLocal == false) {
            val manga = manga ?: return
            val dbChapter = chapter.chapter
            val isDownloaded = downloadManager.isChapterDownloaded(
                dbChapter.title,
                dbChapter.scanlator,
                manga.titleEnglish,
                skipCache = true,
            )
            if (isDownloaded) {
                chapter.state = ReaderChapter.State.Wait
            }
        }

        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            logcat { "chapter already loaded ${chapter.chapter}" }
            return
        }
        suspendRunCatching {
            logcat { "Preloading ${chapter.chapter}" }
            loader.loadChapter(chapter)
        }
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    private fun onPageSelected(page: ReaderPage) {
        // InsertPage doesn't change page progress
        if (page is InsertPage) {
            return
        }

        val selectedChapter = page.chapter
        val pages = selectedChapter.pages ?: return

        // Save last page read and mark as read if needed
        screenModelScope.launch(NonCancellable) {
            updateChapterProgress(selectedChapter, page)
        }

        if (selectedChapter != getCurrentChapter()) {
            logcat { "Setting ${selectedChapter.chapter.url} as active" }
            loadNewChapter(selectedChapter)
        }

        val inDownloadRange = page.number.toDouble() / pages.size > 0.25
        if (inDownloadRange) {
            downloadNextChapters()
        }
    }

    private fun downloadNextChapters() {
        if (downloadAheadAmount == 0) return
        val manga = manga ?: return

        // Only download ahead if current + next chapter is already downloaded too to avoid jank
        if (getCurrentChapter()?.pageLoader !is DownloadPageLoader) return
        val nextChapter = viewer.viewerChapters?.nextChapter?.chapter ?: return

        ioCoroutineScope.launch {
            val isNextChapterDownloaded = downloadManager.isChapterDownloaded(
                nextChapter.title,
                nextChapter.scanlator,
                manga.titleEnglish,
            )
            if (!isNextChapterDownloaded) return@launch

            val chaptersToDownload = getNextChapters
                .await(manga.id, nextChapter.id)
                .take(downloadAheadAmount)

            downloadManager.downloadChapters(
                manga.toResource(),
                chaptersToDownload.map(Chapter::toResource),
            )
        }
    }

    /**
     * Removes [currentChapter] from download queue
     * if setting is enabled and [currentChapter] is queued for download
     */
    private fun cancelQueuedDownloads(currentChapter: ReaderChapter): QItem<Download>? {
        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id)?.also {
            downloadManager.cancelQueuedDownloads(listOf(it.data))
        }
    }

    /**
     * Determines if deleting option is enabled and nth to last chapter actually exists.
     * If both conditions are satisfied enqueues chapter for delete
     * @param currentChapter current chapter, which is going to be marked as read.
     */
    private fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        val removeAfterReadSlots = state.value.settings.removeAfterReadSlots
        if (removeAfterReadSlots == -1) return

        // Determine which chapter should be deleted and enqueue
        val currentChapterPosition = chapterList.indexOf(currentChapter)
        val chapterToDelete = chapterList.getOrNull(currentChapterPosition - removeAfterReadSlots)

        // If chapter is completely read, no need to download it
        chapterToDownload = null

        if (chapterToDelete != null) {
            enqueueDeleteReadChapters(chapterToDelete.chapter)
        }
    }

    /**
     * Saves the chapter progress (last read page and whether it's read)
     * if incognito mode isn't on.
     */
    private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
        val pageIndex = page.index
        readerChapter.requestedPage = pageIndex

        if (!incognitoMode && page.status != Page.State.ERROR) {
            readerChapter.updateChapter(
                lastReadPage = pageIndex
            )

            if (readerChapter.pages?.lastIndex == pageIndex) {
                readerChapter.updateChapter(
                    lastReadPage = readerChapter.chapter.pages
                )
                updateTrackChapterRead(readerChapter)
                deleteChapterIfNeeded(readerChapter)
            }

            chapterHandler.updateLastReadPage(
                readerChapter.chapter.id,
                readerChapter.chapter.lastReadPage ?: -1
            )
        }
    }

    fun restartReadTimer() {
        chapterReadStartTime = Clock.System.now().toEpochMilliseconds()
    }

    fun flushReadTimer() {
        getCurrentChapter()?.let {
            screenModelScope.launch(NonCancellable) {
                updateHistory(it)
            }
        }
    }

    /**
     * Saves the chapter last read history if incognito mode isn't on.
     */
    private suspend fun updateHistory(readerChapter: ReaderChapter) {
        if (incognitoMode) return

        val chapterId = readerChapter.chapter.id
        val readAt = localDateTimeNow()
        val now = epochMillis()
        val sessionReadDuration = chapterReadStartTime?.let { now - it } ?: 0
        historyRepository.insertHistory(
            HistoryUpdate(
                chapterId, readAt, sessionReadDuration
            )
        )
        chapterReadStartTime = null
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    suspend fun loadNextChapter() {
        val nextChapter = viewer.viewerChapters?.nextChapter ?: return
        loadAdjacent(nextChapter)
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    suspend fun loadPreviousChapter() {
        val prevChapter = viewer.viewerChapters?.prevChapter ?: return
        loadAdjacent(prevChapter)
    }

    /**
     * Returns the currently active chapter.
     */
    private fun getCurrentChapter(): ReaderChapter? {
        return viewer.currentChapter
    }

    /**
     * Bookmarks the currently active chapter.
     */
    fun toggleChapterBookmark() {
        val chapter = getCurrentChapter()?.chapter ?: return
        screenModelScope.launch(NonCancellable) {
            chapterHandler.toggleChapterBookmarked(chapter.id)
                .onSuccess {
                    mutableState.update {
                        it.copy(
                            bookmarked = it.bookmarked,
                        )
                    }
                }
        }
    }

    fun showMenus(visible: Boolean) {
        mutableState.update { it.copy(menuVisible = visible) }
    }

    /**
     * Starts the service that updates the last chapter read in sync services. This operation
     * will run in a background thread and errors are ignored.
     */
    private fun updateTrackChapterRead(readerChapter: ReaderChapter) {
        // TODO("updateTrackChapterRead")
    }

    /**
     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadChapters(chapter: Chapter) {
        if (!chapter.read) return
        val manga = manga ?: return

        screenModelScope.launch(NonCancellable) {
            downloadManager.deleteChapters(
                listOf(chapter.toResource()),
                manga.toResource(),
            )
        }
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        screenModelScope.launch(NonCancellable) {

        }
    }

    @Immutable
    data class State(
        val manga: Manga? = null,
        val chapters: List<Chapter> = emptyList(),
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val viewerFlags: Long? = null,
        val menuVisible: Boolean = false,
        val settings: ReaderSettings = ReaderSettings(),
        @IntRange(from = -100, to = 100)
        val brightnessOverlayValue: Int = 0,
    )

    sealed interface Event {
        data object PageChanged : Event
        data class SetOrientation(val orientation: Int) : Event
        data class ShareImage(val uri: Uri, val page: ReaderPage) : Event
        data class CopyImage(val uri: Uri) : Event
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

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}