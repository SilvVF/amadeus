package io.silv.reader2

import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.data.download.QItem
import io.silv.datastore.ReaderPrefs
import io.silv.datastore.dataStoreDeps
import io.silv.datastore.get
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.interactor.GetNextChapters
import io.silv.data.chapter.Chapter
import io.silv.data.chapter.toResource
import io.silv.data.history.HistoryRepository
import io.silv.data.history.HistoryUpdate
import io.silv.data.manga.interactor.GetChaptersByMangaId
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.interactor.SetMangaViewerFlags
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.toResource
import io.silv.reader.loader.ChapterLoader
import io.silv.reader.loader.DownloadPageLoader
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader2.Reader2ScreenModel.Event
import io.silv.ui.AppState
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ioCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    val saveState: (String, Int) -> Unit,
    private val appState: AppState,
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val downloadProvider: DownloadProvider = downloadDeps.downloadProvider,
    // private val imageSaver: ImageSaver,
    private val dataStore: DataStore<Preferences> = dataStoreDeps.dataStore,
    //private val trackChapter: TrackChapter = Injekt.get(),
    private val getManga: GetManga = dataDeps.getManga,
    private val getChaptersByMangaId: GetChaptersByMangaId = dataDeps.getChaptersByMangaId,
    private val getNextChapters: GetNextChapters = dataDeps.getNextChapters,
    private val historyRepository: HistoryRepository = dataDeps.historyRepository,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    private val setMangaViewerFlags: SetMangaViewerFlags = dataDeps.setMangaViewerFlags,
) : EventStateScreenModel<Event, Reader2ScreenModel.State>(State()) {


    val manga: Manga? get() = state.value.manga
    private val loader: ChapterLoader by lazy { ChapterLoader(manga!!) }

    private var chapterReadStartTime: Long? = null
    private var chapterToDownload: Download? = null


    val viewer = PagerViewer(scope = MainScope()) { action ->
        when (action) {
            PagerAction.ToggleMenu -> showMenus(!state.value.menuVisible)
            is PagerAction.OnPageSelected -> onPageSelected(action.page)
            is PagerAction.RequestPreloadChapter -> screenModelScope.launch {
                preload(action.chapter)
            }

            PagerAction.ShowMenu -> showMenus(true)
        }
    }

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
        snapshotFlow { appState.navigator?.lastItemOrNull }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach {
                if (it !is ReaderScreen2) {
                    deletePendingChapters()
                }
            }
            .launchIn(screenModelScope)

        state.map { it.viewerChapters?.currChapter }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { currentChapter ->
                if (chapterPageIndex >= 0) {
                    // Restore from SavedState
                    currentChapter.requestedPage = chapterPageIndex
                } else if (!currentChapter.chapter.read) {
                    currentChapter.requestedPage = currentChapter.chapter.lastReadPage ?: -1
                }
                chapterId = currentChapter.chapter.id
            }
            .launchIn(screenModelScope)

        state.map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(viewer::setChapters)
            .launchIn(screenModelScope)
    }

    override fun onDispose() {
        val currentChapters = state.value.viewerChapters
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
                    mutableState.update { it.copy(manga = manga) }
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
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                it.viewerChapters?.unref()

                chapterToDownload = cancelQueuedDownloads(newChapters.currChapter)?.data
                it.copy(
                    viewerChapters = newChapters,
                    bookmarked = newChapters.currChapter.chapter.bookmarked,
                )
            }
        }
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

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
            return
        }
        try {
            logcat { "Preloading ${chapter.chapter.url}" }
            loader.loadChapter(chapter)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            return
        }
        state.value.viewerChapters?.let(viewer::setChapters)
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
        val nextChapter = state.value.viewerChapters?.nextChapter?.chapter ?: return

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
    private suspend fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        val removeAfterReadSlots = dataStore.get(ReaderPrefs.removeAfterReadSlots) ?: -1
        if (removeAfterReadSlots == -1) return

        // Determine which chapter should be deleted and enqueue
        val currentChapterPosition = chapterList.indexOf(currentChapter)
        val chapterToDelete = chapterList.getOrNull(currentChapterPosition - removeAfterReadSlots)

        // If chapter is completely read, no need to download it
        chapterToDownload = null

        if (chapterToDelete != null) {
            enqueueDeleteReadChapters(chapterToDelete)
        }
    }

    /**
     * Saves the chapter progress (last read page and whether it's read)
     * if incognito mode isn't on.
     */
    private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
        val pageIndex = page.index

        mutableState.update {
            it.copy(currentPage = pageIndex + 1)
        }
        readerChapter.requestedPage = pageIndex
        saveState(chapterId, pageIndex)

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
        val nextChapter = state.value.viewerChapters?.nextChapter ?: return
        loadAdjacent(nextChapter)
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    suspend fun loadPreviousChapter() {
        val prevChapter = state.value.viewerChapters?.prevChapter ?: return
        loadAdjacent(prevChapter)
    }

    /**
     * Returns the currently active chapter.
     */
    private fun getCurrentChapter(): ReaderChapter? {
        return state.value.currentChapter
    }

    fun getChapterUrl(): String? {
        return getCurrentChapter()?.chapter?.url
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

    /**
     * Returns the viewer position used by this manga or the default one.
     */
    suspend fun getMangaReadingMode(resolveDefault: Boolean = true): Int {
        val default =
            dataStore.get(ReaderPrefs.defaultReadingMode) ?: ReadingMode.LEFT_TO_RIGHT.flagValue
        return default
    }

    /**
     * Updates the viewer position for the open manga.
     */
    fun setMangaReadingMode(readingMode: ReadingMode) {
        val manga = manga ?: return
        runBlocking(Dispatchers.IO) {
            TODO("Set manga view flags")
//            setMangaViewerFlags.awaitSetReadingMode(
//                manga.id,
//                readingMode.flagValue.toLong(),
//            )
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.currChapter
                currChapter.requestedPage = currChapter.chapter.lastReadPage ?: -1

                mutableState.update {
                    it.copy(
                        manga = getManga.await(manga.id),
                        viewerChapters = currChapters,
                    )
                }
            }
            state.value.viewerChapters?.let(viewer::setChapters)
        }
    }

    /**
     * Returns the orientation type used by this manga or the default one.
     */
    suspend fun getMangaOrientation(resolveDefault: Boolean = true): Int {
        val default =
            dataStore.get(ReaderPrefs.defaultOrientationType) ?: Reader2Orientation.FREE.flagValue
        return default
    }

    /**
     * Updates the orientation type for the open manga.
     */
    fun setMangaOrientationType(orientation: Reader2Orientation) {
        val manga = manga ?: return
        ioCoroutineScope.launch {
            setMangaViewerFlags.awaitSetOrientation(manga.id, orientation.flagValue.toLong())
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.currChapter
                currChapter.requestedPage = currChapter.chapter.lastReadPage ?: -1

                mutableState.update {
                    it.copy(
                        manga = getManga.await(manga.id),
                        viewerChapters = currChapters,
                    )
                }
                sendEvent(Event.SetOrientation(getMangaOrientation()))
            }
        }
    }

    suspend fun toggleCropBorders(): Boolean {
        val isPagerType = ReadingMode.isPagerType(getMangaReadingMode())
        return if (isPagerType) {
            TODO("readerPreferences.cropBorders().toggle()")
        } else {
            TODO("readerPreferences.cropBordersWebtoon().toggle()")
        }
    }

    fun showMenus(visible: Boolean) {
        mutableState.update { it.copy(menuVisible = visible) }
    }

    fun showLoadingDialog() {
        mutableState.update { it.copy(dialog = Dialog.Loading) }
    }

    fun openReadingModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
    }

    fun openOrientationModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
    }

    fun openPageDialog(page: ReaderPage) {
        mutableState.update { it.copy(dialog = Dialog.PageActions(page)) }
    }

    fun openSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.Settings) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun setBrightnessOverlayValue(value: Int) {
        mutableState.update { it.copy(brightnessOverlayValue = value) }
    }

    /**
     * Saves the image of this the selected page on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage() {
        TODO("saveImage")
    }

    /**
     * Shares the image of this the selected page and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(copyToClipboard: Boolean) {
        TODO("shareImage")
    }

    /**
     * Sets the image of this the selected page as cover and notifies the UI of the result.
     */
    fun setAsCover() {
        TODO("setAsCover")
    }

    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error,
    }

    sealed interface SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult
        class Error(val error: Throwable) : SaveImageResult
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
    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        if (!chapter.chapter.read) return
        val manga = manga ?: return

        screenModelScope.launch(NonCancellable) {
            downloadManager.deleteChapters(
                listOf(chapter.chapter.toResource()),
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
        val viewerChapters: ViewerChapters? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val currentPage: Int = -1,
        val viewerFlags: Long? = null,
        val dialog: Dialog? = null,
        val menuVisible: Boolean = false,
        @IntRange(from = -100, to = 100)
        val brightnessOverlayValue: Int = 0,
    ) {
        val currentChapter: ReaderChapter?
            get() = viewerChapters?.currChapter

        val totalPages: Int
            get() = currentChapter?.pages?.size ?: -1
    }

    sealed interface Dialog {
        data object Loading : Dialog
        data object Settings : Dialog
        data object ReadingModeSelect : Dialog
        data object OrientationModeSelect : Dialog
        data class PageActions(val page: ReaderPage) : Dialog
    }

    sealed interface Event {
        data object PageChanged : Event
        data class SetOrientation(val orientation: Int) : Event
        data class SetCoverResult(val result: SetAsCoverResult) : Event

        data class SavedImage(val result: SaveImageResult) : Event
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

/**
 * Interface for implementing a viewer.
 */
interface Viewer {

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    fun destroy() {}

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    fun setChapters(chapters: ViewerChapters)

    /**
     * Tells this viewer to move to the given [page].
     */
    fun moveToPage(page: ReaderPage)
}

class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}