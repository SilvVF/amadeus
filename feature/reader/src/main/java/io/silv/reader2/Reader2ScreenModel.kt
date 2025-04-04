package io.silv.reader2

import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import io.silv.common.DependencyAccessor
import io.silv.common.model.Download
import io.silv.common.model.Page
import io.silv.common.model.ReaderOrientation
import io.silv.data.download.DownloadManager
import io.silv.data.download.DownloadProvider
import io.silv.datastore.SettingsStore
import io.silv.datastore.dataStoreDeps
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.domain.chapter.interactor.ChapterHandler
import io.silv.domain.history.HistoryRepository
import io.silv.domain.manga.interactor.GetManga
import io.silv.domain.manga.interactor.GetMangaWithChapters
import io.silv.domain.manga.model.Manga
import io.silv.reader.Viewer
import io.silv.reader.ViewerChapters
import io.silv.reader.loader.ChapterLoader
import io.silv.reader.loader.ReaderChapter
import io.silv.reader.loader.ReaderPage
import io.silv.reader2.Reader2ScreenModel.Event
import io.silv.ui.EventStateScreenModel
import io.silv.ui.ScreenStateHandle

/**
 * Presenter used by the activity to perform background operations.
 */
class Reader2ScreenModel  @OptIn(DependencyAccessor::class) constructor(
    private val savedState: ScreenStateHandle,
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val downloadProvider: DownloadProvider = downloadDeps.downloadProvider,
    // private val imageSaver: ImageSaver,
    preferences:  SettingsStore = dataStoreDeps.settingsStore,
    //private val trackChapter: TrackChapter = Injekt.get(),
    private val getManga: GetManga = dataDeps.getManga,
    private val getChaptersByMangaId: GetMangaWithChapters = dataDeps.getMangaWithChapters,
    //private val getNextChapters: GetNextChapters = Injekt.get(),
    private val historyRepository: HistoryRepository = dataDeps.historyRepository,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
    //private val setMangaViewerFlags: SetMangaViewerFlags = Injekt.get(),
) : EventStateScreenModel<Event, Reader2ScreenModel.State>(State()) {


    val manga: Manga? get() = state.value.manga
    private var loader: ChapterLoader? = null

    private var chapterReadStartTime: Long? = null
    private var chapterToDownload: Download? = null

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        // Implementation removed
    }

    private val incognitoMode = false
    private val downloadAheadAmount = 4

    init {
        // Implementation removed
    }

    override fun onDispose() {
        // Implementation removed
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onActivityFinish() {
        // Implementation removed
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        // Implementation removed
        return false
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    suspend fun init(mangaId: Long, initialChapterId: Long): Result<Boolean> {
        // Implementation removed
        return Result.success(false)
    }

    /**
     * Loads the given [chapter] with this [loader] and updates the currently active chapters.
     * Callers must handle errors.
     */
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ): ViewerChapters {
        // Implementation removed
        return ViewerChapters(chapter, null, null)
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        // Implementation removed
    }

    /**
     * Called when the user is going to load the prev/next chapter through the toolbar buttons.
     */
    private suspend fun loadAdjacent(chapter: ReaderChapter) {
        // Implementation removed
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    suspend fun preload(chapter: ReaderChapter) {
        // Implementation removed
    }

    fun onViewerLoaded(viewer: Viewer?) {
        // Implementation removed
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage) {
        // Implementation removed
    }

    private fun downloadNextChapters() {
        // Implementation removed
    }

    /**
     * Removes [currentChapter] from download queue
     * if setting is enabled and [currentChapter] is queued for download
     */
    private fun cancelQueuedDownloads(currentChapter: ReaderChapter): Download? {
        // Implementation removed
        return null
    }

    /**
     * Determines if deleting option is enabled and nth to last chapter actually exists.
     * If both conditions are satisfied enqueues chapter for delete
     * @param currentChapter current chapter, which is going to be marked as read.
     */
    private fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        // Implementation removed
    }

    /**
     * Saves the chapter progress (last read page and whether it's read)
     * if incognito mode isn't on.
     */
    private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
        // Implementation removed
    }

    fun restartReadTimer() {
        // Implementation removed
    }

    fun flushReadTimer() {
        // Implementation removed
    }

    /**
     * Saves the chapter last read history if incognito mode isn't on.
     */
    private suspend fun updateHistory(readerChapter: ReaderChapter) {
        // Implementation removed
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    suspend fun loadNextChapter() {
        // Implementation removed
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    suspend fun loadPreviousChapter() {
        // Implementation removed
    }

    /**
     * Returns the currently active chapter.
     */
    private fun getCurrentChapter(): ReaderChapter? {
        // Implementation removed
        return null
    }

    fun getSource() = null

    fun getChapterUrl(): String? {
        // Implementation removed
        return null
    }

    /**
     * Bookmarks the currently active chapter.
     */
    fun toggleChapterBookmark() {
        // Implementation removed
    }

    /**
     * Returns the viewer position used by this manga or the default one.
     */
    fun getMangaReadingMode(resolveDefault: Boolean = true): Int {
        // Implementation removed
        return 0
    }

    /**
     * Updates the viewer position for the open manga.
     */
//    fun setMangaReadingMode(readingMode: ReadingMode) {
//        // Implementation removed
//    }

    /**
     * Returns the orientation type used by this manga or the default one.
     */
    fun getMangaOrientation(resolveDefault: Boolean = true): Int {
        // Implementation removed
        return 0
    }

    /**
     * Updates the orientation type for the open manga.
     */
    fun setMangaOrientationType(orientation: ReaderOrientation) {
        // Implementation removed
    }

    fun toggleCropBorders(): Boolean {
        // Implementation removed
        return false
    }

    /**
     * Generate a filename for the given [manga] and [page]
     */
    private fun generateFilename(
        manga: Manga,
        page: ReaderPage,
    ): String {
        // Implementation removed
        return ""
    }

    fun showMenus(visible: Boolean) {
        // Implementation removed
    }

    fun showLoadingDialog() {
        // Implementation removed
    }

    fun openReadingModeSelectDialog() {
        // Implementation removed
    }

    fun openOrientationModeSelectDialog() {
        // Implementation removed
    }

    fun openPageDialog(page: ReaderPage) {
        // Implementation removed
    }

    fun openSettingsDialog() {
        // Implementation removed
    }

    fun closeDialog() {
        // Implementation removed
    }

    fun setBrightnessOverlayValue(value: Int) {
        // Implementation removed
    }

    /**
     * Saves the image of this the selected page on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage() {
        // Implementation removed
    }

    /**
     * Shares the image of this the selected page and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(copyToClipboard: Boolean) {
        // Implementation removed
    }

    /**
     * Sets the image of this the selected page as cover and notifies the UI of the result.
     */
    fun setAsCover() {
        // Implementation removed
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
        // Implementation removed
    }

    /**
     * Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
     * manager handles persisting it across process deaths.
     */
    private fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        // Implementation removed
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        // Implementation removed
    }

    @Immutable
    data class State(
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
        data object ReloadViewerChapters : Event
        data object PageChanged : Event
        data class SetOrientation(val orientation: Int) : Event
        data class SetCoverResult(val result: SetAsCoverResult) : Event

        data class SavedImage(val result: SaveImageResult) : Event
        data class ShareImage(val uri: Uri, val page: ReaderPage) : Event
        data class CopyImage(val uri: Uri) : Event
    }
}