@file:OptIn(FlowPreview::class)

package io.silv.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import cafe.adriel.voyager.core.model.screenModelScope
import io.silv.common.DependencyAccessor
import io.silv.common.createTrigger
import io.silv.common.model.Download
import io.silv.data.download.DownloadManager
import io.silv.data.download.QItem
import io.silv.di.dataDeps
import io.silv.di.downloadDeps
import io.silv.data.chapter.interactor.ChapterHandler
import io.silv.data.chapter.interactor.GetBookmarkedChapters
import io.silv.data.chapter.interactor.GetChapter
import io.silv.data.chapter.toResource
import io.silv.data.history.GetLibraryLastUpdated
import io.silv.data.manga.interactor.GetLibraryMangaWithChapters
import io.silv.data.manga.interactor.GetManga
import io.silv.data.manga.interactor.MangaHandler
import io.silv.data.manga.model.Manga
import io.silv.data.manga.model.MangaWithChapters
import io.silv.data.manga.model.toResource
import io.silv.data.update.UpdateWithRelations
import io.silv.data.update.UpdatesRepository
import io.silv.library.state.LibraryError
import io.silv.library.state.LibraryEvent
import io.silv.library.state.LibraryMangaState
import io.silv.library.state.LibraryState
import io.silv.sync.workers.MangaSyncWorker
import io.silv.ui.EventStateScreenModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class UiChapterUpdate(
    val update: UpdateWithRelations,
    val downloaded: Boolean,
    val download: QItem<Download>?
)

class LibraryScreenModel @OptIn(DependencyAccessor::class) constructor(
    updatesRepository: UpdatesRepository = dataDeps.updatesRepository,
    getLibraryLastUpdated: GetLibraryLastUpdated = dataDeps.getLibraryLastUpdated,
    getBookmarkedChapters: GetBookmarkedChapters = dataDeps.getBookmarkedChapters,
    getLibraryMangaWithChapters: GetLibraryMangaWithChapters = dataDeps.getLibraryMangaWithChapters,
    private val downloadManager: DownloadManager = downloadDeps.downloadManager,
    private val getManga: GetManga = dataDeps.getManga,
    private val getChapter: GetChapter = dataDeps.getChapter,
    private val mangaHandler: MangaHandler = dataDeps.mangaHandler,
    private val chapterHandler: ChapterHandler = dataDeps.chapterHandler,
): EventStateScreenModel<LibraryEvent, LibraryState>(LibraryState()) {

    var mangaSearchText by mutableStateOf("")
    private val filteredTagIds = MutableStateFlow(emptySet<String>())

    @OptIn(FlowPreview::class)
    private val debouncedSearch = snapshotFlow { mangaSearchText }
        .debounce(100)
        .distinctUntilChanged()
        .onStart { emit("") }

    private val downloadTrigger = createTrigger(
        downloadManager.queueState,
        downloadManager.cacheChanges,
    )

    private val libraryMangaWithDownloadState = getLibraryMangaWithChapters.subscribe()
        .map { list ->
            list.map {(manga, chapters) ->
                MangaWithChapters(
                    manga = manga,
                    chapters = chapters.map {
                        it.copy(
                            downloaded = withContext(Dispatchers.IO) {
                                downloadManager.isChapterDownloaded(it.title, it.scanlator, manga.titleEnglish)
                            }
                        )
                    }
                        .toList()
                )
            }
        }

    private val cachedManga = mutableMapOf<String, Manga>()

    init {
        combine(
            libraryMangaWithDownloadState,
            debouncedSearch,
            filteredTagIds,
            downloadTrigger
        ) { list, query, tagIds, _ ->

                if (list.isEmpty()) {
                    mutableState.value = mutableState.value.copy(
                        libraryMangaState = LibraryMangaState.Error(LibraryError.NoFavoritedChapters)
                    )
                    return@combine
                }

                mutableState.update { state ->
                    state.copy(
                        libraryMangaState =
                        (state.libraryMangaState.success ?: LibraryMangaState.Success()).copy(
                            filteredTagIds = tagIds.toList(),
                            filteredText = query,
                            mangaWithChapters = list.toList()
                        )
                    )
                }
            }
            .catch {
                mutableState.value = mutableState.value.copy(
                    libraryMangaState = LibraryMangaState.Error(LibraryError.NoFavoritedChapters)
                )
            }
            .launchIn(screenModelScope)

        combine(
            updatesRepository.observeUpdates(limit = 100),
            downloadManager.queueState,
            downloadTrigger,
        ) { x, y ,z -> Triple(x, y, z) }
            .onEach { (updates, downloads, _) ->
                mutableState.update { state ->
                   state.copy(
                        updates = updates.map {

                            val downloaded = withContext(Dispatchers.IO) {
                                downloadManager.isChapterDownloaded(it.chapterName, it.scanlator, it.mangaTitle)
                            }

                            UiChapterUpdate(
                                update = it,
                                downloaded = downloaded,
                                download = downloads.find { download -> download.data.chapter.id == it.chapterId },
                            )
                        }
                            .groupBy { it.update.chapterUpdatedAt.date.toEpochDays() }
                            .toList()
                            .map { it.first to it.second.toList() }
                            .toList()
                    )
                }
            }
            .launchIn(screenModelScope)

        getBookmarkedChapters.subscribe()
            .onEach { chapters ->

                val grouped = chapters.groupBy { it.mangaId }
                    .mapKeys { (k, _) -> cachedManga.getOrElse(k) { getManga.await(k) } }
                    .toList()
                    .mapNotNull { if (it.first == null) null else Pair(it.first!!, it.second.toList()) }
                    .toList()

                mutableState.update { state ->
                    state.copy(
                        bookmarkedChapters = grouped
                    )
                }
            }
            .launchIn(screenModelScope)

        MangaSyncWorker.isRunning.onEach {
            mutableState.update { state ->
                state.copy(updatingLibrary = it)
            }
        }
            .launchIn(screenModelScope)

        getLibraryLastUpdated.subscribe().onEach { time ->
            mutableState.update { state -> state.copy(libraryLastUpdated = time) }
        }
            .launchIn(screenModelScope)
    }

    fun onTagFiltered(id: String) {
        screenModelScope.launch {
            filteredTagIds.update { set ->
                set.toMutableSet().apply {
                    if(!add(id)) { remove(id) }
                }
            }
        }
    }

    fun startDownload(mangaId: String, chapterId: String) {
        screenModelScope.launch {

            val manga = getManga.await(mangaId) ?: return@launch
            val chapter = getChapter.await(chapterId) ?: return@launch

            downloadManager.downloadChapters(manga.toResource(), listOf(chapter.toResource()))
        }
    }

    fun cancelDownload(download: Download) {
        screenModelScope.launch {
            downloadManager.cancelQueuedDownloads(listOf(download))
        }
    }

    fun refreshLibrary() {
        screenModelScope.launch {
            MangaSyncWorker.enqueueOneTimeWork()
        }
    }

    fun deleteDownloadedChapter(mangaId: String, chapterId: String) {
        screenModelScope.launch {

            val manga = getManga.await(mangaId) ?: return@launch
            val chapter = getChapter.await(chapterId) ?: return@launch

            downloadManager.deleteChapters(
                manga = manga.toResource(),
                chapters = listOf(chapter.toResource())
            )
        }
    }

    fun updateMangaUpdatedTrackedAfter(mangaId: String, chapterId: String) {
        screenModelScope.launch {

            val manga = getManga.await(mangaId) ?: return@launch
            val chapter = getChapter.await(chapterId) ?: return@launch

            mangaHandler.updateTrackedAfterTime(manga, chapter.updatedAt)
        }
    }

    fun pauseAllDownloads() {
        screenModelScope.launch {
            downloadManager.pauseDownloads()
        }
    }

    fun startDownloadNow(download: Download) {
        screenModelScope.launch {
            downloadManager.startDownloadNow(download.chapter.id)
        }
    }

    fun toggleChapterRead(chapterId: String) {
        screenModelScope.launch {
            chapterHandler.toggleReadOrUnread(chapterId)
        }
    }

    fun toggleChapterBookmark(chapterId: String) {
        screenModelScope.launch {
            chapterHandler.toggleChapterBookmarked(chapterId)
        }
    }

    fun onSearchChanged(text: String) {
        mangaSearchText = text
    }
}
