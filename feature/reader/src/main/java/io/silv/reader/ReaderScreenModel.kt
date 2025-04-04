package io.silv.reader

import android.net.Uri
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.model.Download
import io.silv.common.model.Page
import io.silv.common.time.localDateTimeNow
import io.silv.data.download.DownloadManager
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.domain.chapter.interactor.ChapterHandler
import io.silv.domain.history.HistoryRepository
import io.silv.domain.history.HistoryUpdate
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.interactor.GetMangaWithChapters
import io.silv.domain.manga.model.Manga
import io.silv.reader.loader.ChapterLoader
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ReaderScreenModel @OptIn(DependencyAccessor::class) constructor(
    private val savedState: SavedStateHandle,
    private val getMangaWithChapters: GetMangaWithChapters = dataDeps.getMangaWithChapters,
    private val getManga: GetManga = dataDeps.getManga,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val historyRepository: HistoryRepository = dataDeps.historyRepository
) : StateScreenModel<ReaderScreenModel.ReaderState>(ReaderState()) {

    private val eventChannel = Channel<Event>(UNLIMITED)
    val eventFlow = eventChannel.receiveAsFlow()

    /**
     * The manga loaded in the reader. It can be null when instantiated for a short time.
     */
    val manga: Manga?
        get() = state.value.manga

    /**
     * The chapter id of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterId = savedState.get<String>("chapter_id")
        set(value) {
            savedState["chapter_id"] = value
            field = value
        }

    /**
     * The visible page index of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterPageIndex = savedState.get<Int>("page_index") ?: -1
        set(value) {
            savedState["page_index"] = value
            field = value
        }

    /**
     * The chapter loader for the loaded manga. It'll be null until [manga] is set.
     */
    private var loader: ChapterLoader? = null

    /**
     * The time the chapter was started reading
     */
    private var chapterReadStartTime: Long? = null

    private var chapterToDownload: Download? = null

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val (_, chapters) = runBlocking { getMangaWithChapters.await(manga!!.id)!! }

        assert(chapters.any { it.id == chapterId }) { "Requested chapter of id $chapterId not found in chapter list" }

        chapters
            .sortedBy { it.chapter }
            .map(::ReaderChapter)
    }

    init {
        // To save state
        state.map { it.viewerChapters?.currChapter }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { currentChapter ->
                if (chapterPageIndex >= 0) {
                    // Restore from SavedState
                    currentChapter.requestedPage = chapterPageIndex
                } else if (!currentChapter.chapter.read) {
                    currentChapter.requestedPage = currentChapter.chapter.lastReadPage ?: 1
                }
                chapterId = currentChapter.chapter.id
            }
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

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onActivityFinish() {
        //deletePendingChapters()
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return manga == null
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    suspend fun init(mangaId: String, initialChapterId: String): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return withContext(Dispatchers.IO) {
            try {
                val manga = getManga.await(mangaId)
                if (manga != null) {
                    mutableState.update { it.copy(manga = manga) }
                    if (chapterId == null) chapterId = initialChapterId
                    loader = ChapterLoader(manga)

                    loadChapter(loader!!, chapterList.first { chapterId == it.chapter.id })
                    Result.success(true)
                } else {
                    // Unlikely but okay
                    Result.success(false)
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                Result.failure(e)
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
    ): ViewerChapters {
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

                //chapterToDownload = cancelQueuedDownloads(newChapters.currChapter)
                it.copy(
                    viewerChapters = newChapters,
                    bookmarked = newChapters.currChapter.chapter.bookmarked,
                )
            }
        }
        return newChapters
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        screenModelScope.launch(Dispatchers.IO) {

            //flushReadTimer()
           // restartReadTimer()

            try {
                loadChapter(loader, chapter)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
            }
        }
    }

    /**
     * Called when the user is going to load the prev/next chapter through the toolbar buttons.
     */
    private suspend fun loadAdjacent(chapter: ReaderChapter) {
        val loader = loader ?: return

        mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
        try {
            withContext(Dispatchers.IO) {
                loadChapter(loader, chapter)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
        }
    }

//    /**
//     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
//     * that the user doesn't have to wait too long to continue reading.
//     */
//    suspend fun preload(chapter: ReaderChapter) {
//        if (chapter.state is ReaderChapter.State.Loaded || chapter.state == ReaderChapter.State.Loading) {
//            return
//        }
//
//        if (chapter.pageLoader?.isLocal == false) {
//            val manga = manga ?: return
//            val dbChapter = chapter.chapter
//            val isDownloaded = downloadManager.isChapterDownloaded(
//                dbChapter.title,
//                dbChapter.scanlator,
//                manga.titleEnglish,
//                skipCache = true
//            )
//            if (isDownloaded) {
//                chapter.state = ReaderChapter.State.Wait
//            }
//        }
//
//        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
//            return
//        }
//
//        val loader = loader ?: return
//        try {
//            loader.loadChapter(chapter)
//        } catch (e: Throwable) {
//            if (e is CancellationException) {
//                throw e
//            }
//            return
//        }
//        eventChannel.trySend(Event.ReloadViewerChapters)
//    }
//
//    fun onViewerLoaded(viewer: Viewer?) {
//        mutableState.update {
//            it.copy(viewer = viewer)
//        }
//    }
//
//    /**
//     * Called every time a page changes on the reader. Used to mark the flag of chapters being
//     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
//     * [page]'s chapter is different from the currently active.
//     */
//    fun onPageSelected(page: ReaderPage) {
//        // InsertPage doesn't change page progress
//        if (page is InsertPage) {
//            return
//        }
//
//        val selectedChapter = page.chapter
//
//        if (selectedChapter.pages == null) {
//            return
//        }
//        // Save last page read and mark as read if needed
//        screenModelScope.launch(NonCancellable) {
//            updateChapterProgress(selectedChapter, page)
//        }
//
//        if (selectedChapter != getCurrentChapter()) {
//            loadNewChapter(selectedChapter)
//        }
//
//
//        eventChannel.trySend(Event.PageChanged)
//    }
//
//    /**
//     * Removes [currentChapter] from download queue
//     * if setting is enabled and [currentChapter] is queued for download
//     */
//    private fun cancelQueuedDownloads(currentChapter: ReaderChapter): Download? {
//        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id)?.also {
//            downloadManager.cancelQueuedDownloads(listOf(it))
//        }
//    }
//
//    /**
//     * Saves the chapter progress (last read page and whether it's read)
//     * if incognito mode isn't on.
//     */
//    private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
//        val pageIndex = page.index
//
//        mutableState.update {
//            it.copy(currentPage = pageIndex + 1)
//        }
//        readerChapter.requestedPage = pageIndex
//        chapterPageIndex = pageIndex
//
//        if (page.status != Page.State.ERROR) {
//            chapterHandler.updateLastReadPage(readerChapter.chapter.id, pageIndex)
//        }
//    }
//
//    fun restartReadTimer() {
//        chapterReadStartTime = Clock.System.now().toEpochMilliseconds()
//    }
//
//    fun flushReadTimer() {
//        getCurrentChapter()?.let {
//            screenModelScope.launch(NonCancellable) {
//                updateHistory(it)
//            }
//        }
//    }
//
//    /**
//     * Saves the chapter last read history if incognito mode isn't on.
//     */
//    private suspend fun updateHistory(readerChapter: ReaderChapter) {
//        val chapterId = readerChapter.chapter.id.takeIf { it.isNotBlank() }!!
//
//        val readAt = Clock.System.now()
//        val sessionReadDuration = chapterReadStartTime?.let { readAt.time - it } ?: 0
//        historyRepository.insertHistory(
//            HistoryUpdate(chapterId, localDateTimeNow(), sessionReadDuration)
//        )
//        chapterReadStartTime = null
//    }
//
//    /**
//     * Called from the activity to load and set the next chapter as active.
//     */
//    suspend fun loadNextChapter() {
//        val nextChapter = state.value.viewerChapters?.nextChapter ?: return
//        loadAdjacent(nextChapter)
//    }
//
//    /**
//     * Called from the activity to load and set the previous chapter as active.
//     */
//    suspend fun loadPreviousChapter() {
//        val prevChapter = state.value.viewerChapters?.prevChapter ?: return
//        loadAdjacent(prevChapter)
//    }
//
//    /**
//     * Returns the currently active chapter.
//     */
//    private fun getCurrentChapter(): ReaderChapter? {
//        return state.value.currentChapter
//    }
//
//    fun getSource() = manga?.source?.let { sourceManager.getOrStub(it) } as? HttpSource
//
//    fun getChapterUrl(): String? {
//        val sChapter = getCurrentChapter()?.chapter ?: return null
//        val source = getSource() ?: return null
//
//        return try {
//            source.getChapterUrl(sChapter)
//        } catch (e: Exception) {
//            logcat(LogPriority.ERROR, e)
//            null
//        }
//    }
//
//    /**
//     * Bookmarks the currently active chapter.
//     */
//    fun toggleChapterBookmark() {
//        val chapter = getCurrentChapter()?.chapter ?: return
//        val bookmarked = !chapter.bookmark
//        chapter.bookmark = bookmarked
//
//        screenModelScope.launchNonCancellable {
//            updateChapter.await(
//                ChapterUpdate(
//                    id = chapter.id!!.toLong(),
//                    bookmark = bookmarked,
//                ),
//            )
//        }
//
//        mutableState.update {
//            it.copy(
//                bookmarked = bookmarked,
//            )
//        }
//    }
//
//    /**
//     * Returns the viewer position used by this manga or the default one.
//     */
//    fun getMangaReadingMode(resolveDefault: Boolean = true): Int {
//        val default = readerPreferences.defaultReadingMode().get()
//        val readingMode = ReadingMode.fromPreference(manga?.readingMode?.toInt())
//        return when {
//            resolveDefault && readingMode == ReadingMode.DEFAULT -> default
//            else -> manga?.readingMode?.toInt() ?: default
//        }
//    }
//
//    /**
//     * Updates the viewer position for the open manga.
//     */
//    fun setMangaReadingMode(readingMode: ReadingMode) {
//        val manga = manga ?: return
//        runBlocking(Dispatchers.IO) {
//            setMangaViewerFlags.awaitSetReadingMode(
//                manga.id,
//                readingMode.flagValue.toLong(),
//            )
//            val currChapters = state.value.viewerChapters
//            if (currChapters != null) {
//                // Save current page
//                val currChapter = currChapters.currChapter
//                currChapter.requestedPage = currChapter.chapter.last_page_read
//
//                mutableState.update {
//                    it.copy(
//                        manga = getManga.await(manga.id),
//                        viewerChapters = currChapters,
//                    )
//                }
//                eventChannel.send(Event.ReloadViewerChapters)
//            }
//        }
//    }
//
//    /**
//     * Returns the orientation type used by this manga or the default one.
//     */
//    fun getMangaOrientation(resolveDefault: Boolean = true): Int {
//        val default = readerPreferences.defaultOrientationType().get()
//        val orientation = ReaderOrientation.fromPreference(manga?.readerOrientation?.toInt())
//        return when {
//            resolveDefault && orientation == ReaderOrientation.DEFAULT -> default
//            else -> manga?.readerOrientation?.toInt() ?: default
//        }
//    }
//
//    /**
//     * Updates the orientation type for the open manga.
//     */
//    fun setMangaOrientationType(orientation: ReaderOrientation) {
//        val manga = manga ?: return
//        screenModelScope.launchIO {
//            setMangaViewerFlags.awaitSetOrientation(manga.id, orientation.flagValue.toLong())
//            val currChapters = state.value.viewerChapters
//            if (currChapters != null) {
//                // Save current page
//                val currChapter = currChapters.currChapter
//                currChapter.requestedPage = currChapter.chapter.last_page_read
//
//                mutableState.update {
//                    it.copy(
//                        manga = getManga.await(manga.id),
//                        viewerChapters = currChapters,
//                    )
//                }
//                eventChannel.send(Event.SetOrientation(getMangaOrientation()))
//                eventChannel.send(Event.ReloadViewerChapters)
//            }
//        }
//    }
//
//    fun toggleCropBorders(): Boolean {
//        val isPagerType = ReadingMode.isPagerType(getMangaReadingMode())
//        return if (isPagerType) {
//            readerPreferences.cropBorders().toggle()
//        } else {
//            readerPreferences.cropBordersWebtoon().toggle()
//        }
//    }
//
//    /**
//     * Generate a filename for the given [manga] and [page]
//     */
//    private fun generateFilename(
//        manga: Manga,
//        page: ReaderPage,
//    ): String {
//        val chapter = page.chapter.chapter
//        val filenameSuffix = " - ${page.number}"
//        return DiskUtil.buildValidFilename(
//            "${manga.title} - ${chapter.name}".takeBytes(
//                DiskUtil.MAX_FILE_NAME_BYTES - filenameSuffix.byteSize(),
//            ),
//        ) + filenameSuffix
//    }
//
//    fun showMenus(visible: Boolean) {
//        mutableState.update { it.copy(menuVisible = visible) }
//    }
//
//    fun showLoadingDialog() {
//        mutableState.update { it.copy(dialog = Dialog.Loading) }
//    }
//
//    fun openReadingModeSelectDialog() {
//        mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
//    }
//
//    fun openOrientationModeSelectDialog() {
//        mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
//    }
//
//    fun openPageDialog(page: ReaderPage) {
//        mutableState.update { it.copy(dialog = Dialog.PageActions(page)) }
//    }
//
//    fun openSettingsDialog() {
//        mutableState.update { it.copy(dialog = Dialog.Settings) }
//    }
//
//    fun closeDialog() {
//        mutableState.update { it.copy(dialog = null) }
//    }
//
//    fun setBrightnessOverlayValue(value: Int) {
//        mutableState.update { it.copy(brightnessOverlayValue = value) }
//    }
//
//    /**
//     * Saves the image of this the selected page on the pictures directory and notifies the UI of the result.
//     * There's also a notification to allow sharing the image somewhere else or deleting it.
//     */
//    fun saveImage() {
//        val page = (state.value.dialog as? Dialog.PageActions)?.page
//        if (page?.status != Page.State.READY) return
//        val manga = manga ?: return
//
//        val context = Injekt.get<Application>()
//        val notifier = SaveImageNotifier(context)
//        notifier.onClear()
//
//        val filename = generateFilename(manga, page)
//
//        // Pictures directory.
//        val relativePath = if (readerPreferences.folderPerManga().get()) {
//            DiskUtil.buildValidFilename(
//                manga.title,
//            )
//        } else {
//            ""
//        }
//
//        // Copy file in background.
//        screenModelScope.launchNonCancellable {
//            try {
//                val uri = imageSaver.save(
//                    image = Image.Page(
//                        inputStream = page.stream!!,
//                        name = filename,
//                        location = Location.Pictures.create(relativePath),
//                    ),
//                )
//                withUIContext {
//                    notifier.onComplete(uri)
//                    eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
//                }
//            } catch (e: Throwable) {
//                notifier.onError(e.message)
//                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
//            }
//        }
//    }
//
//    /**
//     * Shares the image of this the selected page and notifies the UI with the path of the file to share.
//     * The image must be first copied to the internal partition because there are many possible
//     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
//     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
//     * image will be kept so it won't be taking lots of internal disk space.
//     */
//    fun shareImage(copyToClipboard: Boolean) {
//        val page = (state.value.dialog as? Dialog.PageActions)?.page
//        if (page?.status != Page.State.READY) return
//        val manga = manga ?: return
//
//        val context = Injekt.get<Application>()
//        val destDir = context.cacheImageDir
//
//        val filename = generateFilename(manga, page)
//
//        try {
//            screenModelScope.launchNonCancellable {
//                destDir.deleteRecursively()
//                val uri = imageSaver.save(
//                    image = Image.Page(
//                        inputStream = page.stream!!,
//                        name = filename,
//                        location = Location.Cache,
//                    ),
//                )
//                eventChannel.send(
//                    if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(
//                        uri,
//                        page
//                    )
//                )
//            }
//        } catch (e: Throwable) {
//            logcat(LogPriority.ERROR, e)
//        }
//    }
//
//    /**
//     * Sets the image of this the selected page as cover and notifies the UI of the result.
//     */
//    fun setAsCover() {
//        val page = (state.value.dialog as? Dialog.PageActions)?.page
//        if (page?.status != Page.State.READY) return
//        val manga = manga ?: return
//        val stream = page.stream ?: return
//
//        screenModelScope.launchNonCancellable {
//            val result = try {
//                manga.editCover(Injekt.get(), stream())
//                if (manga.isLocal() || manga.favorite) {
//                    SetAsCoverResult.Success
//                } else {
//                    SetAsCoverResult.AddToLibraryFirst
//                }
//            } catch (e: Exception) {
//                SetAsCoverResult.Error
//            }
//            eventChannel.send(Event.SetCoverResult(result))
//        }
//    }
//
    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error,
    }
//
    sealed interface SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult
        class Error(val error: Throwable) : SaveImageResult
    }

//    /**
//     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
//     * manager handles persisting it across process deaths.
//     */
//    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
//        if (!chapter.chapter.read) return
//        val manga = manga ?: return
//
//        screenModelScope.launch(NonCancellable) {
//            downloadManager.enqueueChaptersToDelete(
//                listOf(chapter.chapter.toDomainChapter()!!),
//                manga,
//            )
//        }
//    }
//
//    /**
//     * Deletes all the pending chapters. This operation will run in a background thread and errors
//     * are ignored.
//     */
//    private fun deletePendingChapters() {
//        screenModelScope.launch(NonCancellable) {
//            downloadManager.deletePendingChapters()
//        }
//    }

    @Immutable
    data class ReaderState(
        val manga: Manga? = null,
        val viewerChapters: ViewerChapters? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val currentPage: Int = -1,

        /**
         * Viewer used to display the pages (pager, webtoon, ...).
         */
        val viewer: Viewer? = null,
        val dialog: Dialog? = null,
        val menuVisible: Boolean = false,
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
        data object ReloadViewerChapters : Event
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

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean

    @Composable
    fun Content()
}
class InsertPage(val parent: ReaderPage) : ReaderPage(parent.index, parent.url, parent.imageUrl) {

    override var chapter: ReaderChapter = parent.chapter

    init {
        status = State.READY
        stream = parent.stream
    }
}